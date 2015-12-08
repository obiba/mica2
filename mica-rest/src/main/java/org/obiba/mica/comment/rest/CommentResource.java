package org.obiba.mica.comment.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.notification.CommentMailNotification;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
@Scope("request")
public class CommentResource {

  @Inject
  private CommentsService commentsService;

  @Inject
  private Dtos dtos;

  @Inject
  private EventBus eventBus;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private CommentMailNotification commentMailNotification;

  private AbstractGitPersistableService service;

  @GET
  public Mica.CommentDto getComment(@PathParam("id") String id, @PathParam("commentId") String commentId) {
    subjectAclService.checkPermission(String.format("/draft/%s", service.getTypeName()), "VIEW", id);
    service.findDraft(id);

    return dtos.asDto(commentsService.findById(commentId));
  }

  @PUT
  public Response updateComment(@PathParam("id") String id, @PathParam("commentId") String commentId, String message) {
    subjectAclService.checkPermission(String.format("/draft/%s/%s/comment", service.getTypeName(), id), "EDIT", commentId);
    service.findDraft(id);
    commentsService.save(Comment.newBuilder(commentsService.findById(commentId)) //
      .message(message) //
      .modifiedBy(SecurityUtils.getSubject().getPrincipal().toString()) //
      .build(), commentMailNotification); //

    return Response.noContent().build();
  }

  @DELETE
  public Response deleteComment(@PathParam("id") String id, @PathParam("commentId") String commentId) {
    subjectAclService.checkPermission(String.format("/draft/%s/%s/comment", service.getTypeName(), id), "DELETE", commentId);
    service.findDraft(id);
    commentsService.delete(commentId);
    eventBus.post(new ResourceDeletedEvent(String.format("/draft/%s/%s/comment", service.getTypeName(), id), commentId));

    return Response.noContent().build();
  }

  public void setService(AbstractGitPersistableService service) {
    this.service = service;
  }
}
