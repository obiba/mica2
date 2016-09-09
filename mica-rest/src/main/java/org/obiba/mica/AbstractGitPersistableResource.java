/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.git.CommitInfo;
import org.obiba.mica.comment.rest.CommentResource;
import org.obiba.mica.comment.rest.CommentsResource;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

public abstract class AbstractGitPersistableResource<T extends EntityState, T1 extends GitPersistable> {

  @Inject
  protected SubjectAclService subjectAclService;

  protected abstract String getId();

  protected abstract AbstractGitPersistableService<T, T1> getService();

  @Inject
  Dtos dtos;

  @Inject
  private CommentsResource commentsResource;

  @Inject
  private CommentResource commentResource;

  @GET
  @Path("/commits")
  public List<Mica.GitCommitInfoDto> getCommitsInfo() {
    subjectAclService.checkPermission("/draft/" + getService().getTypeName(), "VIEW", getId());
    return dtos.asDto(getService().getCommitInfos(getService().findDraft(getId())));
  }

  @PUT
  @Path("/commit/{commitId}/restore")
  public Response restoreCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    subjectAclService.checkPermission("/draft/" + getService().getTypeName(), "EDIT", getId());
    T1 gitPersistable = getService().getFromCommit(getService().findDraft(getId()), commitId);

    if (gitPersistable != null){
      getService().save(gitPersistable, createRestoreComment(getService().getCommitInfo(gitPersistable, commitId)));
    }

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}")
  public Mica.GitCommitInfoDto getCommitInfo(@NotNull @PathParam("commitId") String commitId) throws IOException {
    subjectAclService.checkPermission("/draft/" + getService().getTypeName(), "VIEW", getId());
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
