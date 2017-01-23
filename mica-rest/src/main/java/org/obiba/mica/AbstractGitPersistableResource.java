/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.elasticsearch.common.Strings;
import org.obiba.git.CommitInfo;
import org.obiba.mica.comment.rest.CommentResource;
import org.obiba.mica.comment.rest.CommentsResource;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGitPersistableResource<T extends EntityState, T1 extends GitPersistable> {

  private static final Logger log = LoggerFactory.getLogger(AbstractGitPersistableResource.class);

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private SubjectAclService subjectAclService;

  protected abstract String getId();

  protected abstract AbstractGitPersistableService<T, T1> getService();

  private final DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd");

  @Inject
  Dtos dtos;

  @Inject
  private CommentsResource commentsResource;

  @Inject
  private CommentResource commentResource;

  @GET
  @Path("/commits")
  public List<Mica.GitCommitInfoDto> getCommitsInfo() {
    checkPermission("/draft/" + getService().getTypeName(), "VIEW");
    return dtos.asDto(getService().getCommitInfos(getService().findDraft(getId())));
  }

  @PUT
  @Path("/commit/{commitId}/restore")
  public Response restoreCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/" + getService().getTypeName(), "EDIT");
    T1 gitPersistable = getService().getFromCommit(getService().findDraft(getId()), commitId);

    if (gitPersistable != null){
      getService().save(gitPersistable, createRestoreComment(getService().getCommitInfo(gitPersistable, commitId)));
    }

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}")
  public Mica.GitCommitInfoDto getCommitInfo(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/" + getService().getTypeName(), "VIEW");
    return dtos.asDto(
      getCommitInfoInternal(getService().getCommitInfo(getService().findDraft(getId()), commitId), commitId, null));
  }

  @Path("/comments")
  public CommentsResource comments() {
    commentsResource.setService(getService());
    return commentsResource;
  }

  @Path("/comment/{commentId}")
  public CommentResource getCommentResource() {
    commentResource.setService(getService());
    return commentResource;
  }

  @PUT
  @Path("/_share_key")
  public Response getShareKey(@QueryParam("expire") String expire) {
    checkPermission("/draft/" + getService().getTypeName(), "EDIT");
    return Response.ok().entity(createShareKey(expire)).build();
  }

  @PUT
  @Path("/_share")
  public Response getShareURL(@QueryParam("expire") String expire) {
    checkPermission("/draft/" + getService().getTypeName(), "EDIT");
    return Response.ok().entity(String.format("https://portal.example.org/mica/%s/%s/draft/%s",
        getService().getTypeName(), getId(), createShareKey(expire))).build();
  }


  /**
   * Check the permission (action on a resource). If a key is provided and is valid, the permission check is by-passed.
   * If the provided key is not valid, permission check is applied.
   * @param resource
   * @param action
   * @param shareKey
   */
  protected void checkPermission(@NotNull String resource, @NotNull String action, @Nullable String shareKey) {
     if (!Strings.isNullOrEmpty(shareKey) && "VIEW".equals(action)) {
       if (!validateShareKey(shareKey)) {
         // second chance
         subjectAclService.checkPermission(resource, action, getId());
       }
     } else {
       subjectAclService.checkPermission(resource, action, getId());
     }
  }

  protected void checkPermission(@NotNull String resource, @NotNull String action) {
    checkPermission(resource, action, null);
  }

  //
  // Private methods
  //

  private String createShareKey(String expire) {
    if(!Strings.isNullOrEmpty(expire)) {
      try {
        iso8601.parse(expire);
      } catch (ParseException e) {
        throw new IllegalArgumentException("Not a valid expiration date");
      }
    }

    return micaConfigService.encrypt(String.format("/%s/%s|%s|%s",
        getService().getTypeName(), getId(), Strings.isNullOrEmpty(expire) ? "" : expire, SecurityUtils.getSubject().getPrincipal()));
  }

  /**
   * Check
   * @param key
   * @return
   */
  private boolean validateShareKey(String key) {
    String decrypted = micaConfigService.decrypt(key);
    if (!decrypted.startsWith(String.format("/%s/%s|", getService().getTypeName(), getId()))) return false;
    String[] tokens = decrypted.split("\\|");
    if (tokens.length != 3) return false;
    if (!Strings.isNullOrEmpty(tokens[1])) {
      try {
        if (!iso8601.parse(tokens[1]).after(new Date())) return false;
      } catch (ParseException e) {
        return false;
      }
    }
    log.info("Using share key on resource {} provided by {} expiring on {}",
        tokens[0], tokens[2], Strings.isNullOrEmpty(tokens[1]) ? "<never>" : tokens[1]);
    return true;
  }

  private CommitInfo getCommitInfoInternal(@NotNull CommitInfo commitInfo, @NotNull String commitId,
    @Nullable String prevCommitId) {
    Iterable<String> diffEntries = getService().getDiffEntries(getService().findDraft(getId()), commitId, prevCommitId);
    return CommitInfo.Builder.createFromObject(commitInfo).diffEntries((List<String>) diffEntries).build();
  }

  private String createRestoreComment(CommitInfo commitInfo) {
    LocalDateTime date = LocalDateTime
      .parse(commitInfo.getDate().toString(), DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss zzz yyyy"));
    String formatted =  date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a"));

    return String.format("Restored revision from '%s' (%s...)", formatted, commitInfo.getCommitId().substring(0,9));
  }
}
