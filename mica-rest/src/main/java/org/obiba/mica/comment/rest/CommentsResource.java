/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.comment.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.notification.CommentMailNotification;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope("request")
public class CommentsResource {

  @Inject
  private CommentsService commentsService;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private CommentMailNotification commentMailNotification;

  private AbstractGitPersistableService service;

  @GET
  public List<Mica.CommentDto> comments(@PathParam("id") String id) {
    subjectAclService.checkPermission(String.format("/draft/%s", service.getTypeName()), "VIEW", id);
    return commentsService.findByResourceAndInstance(String.format("/draft/%s", service.getTypeName()), id).stream()
      .map(dtos::asDto).collect(Collectors.toList());
  }

  @POST
  public Response createComment(@PathParam("id") String id, String message) {
    subjectAclService.checkPermission(String.format("/draft/%s", service.getTypeName()), "VIEW", id);
    Comment comment = commentsService.save( //
      Comment.newBuilder() //
        .createdBy(SecurityUtils.getSubject().getPrincipal().toString()) //
        .message(message) //
        .resourceId(String.format("/draft/%s", service.getTypeName())) //
        .instanceId(id) //
        .build(), commentMailNotification);

    subjectAclService.addPermission(String.format("/draft/%s/%s/comment", service.getTypeName(), id), "VIEW,EDIT,DELETE", comment.getId());

    return Response.noContent().build();
  }

  public void setService(AbstractGitPersistableService service) {
    this.service = service;
  }
}
