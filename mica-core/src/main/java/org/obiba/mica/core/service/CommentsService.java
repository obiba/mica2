/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.domain.NoSuchCommentException;
import org.obiba.mica.core.event.CommentDeletedEvent;
import org.obiba.mica.core.event.CommentUpdatedEvent;
import org.obiba.mica.core.notification.MailNotification;
import org.obiba.mica.core.repository.CommentsRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

@Service
@Validated
public class CommentsService {

  @Inject
  protected EventBus eventBus;

  @Inject
  CommentsRepository commentsRepository;

  private static final Pageable LIMITER = Pageable.ofSize(2);

  public Comment save(Comment comment, MailNotification<Comment> mailNotification) {
    if (comment.getMessage().isEmpty()) throw new IllegalArgumentException("Comment message cannot be empty");

    Comment saved = comment;

    if (!comment.isNew()) {
      saved = commentsRepository.findById(comment.getId()).orElse(null);
      if(saved == null) {
        saved = comment;
      } else {
        BeanUtils.copyProperties(comment, saved, "id", "classId", "createdDate");
      }

      saved.setLastModifiedDate(LocalDateTime.now());
    }

    if (mailNotification != null) mailNotification.send(saved);

    commentsRepository.save(saved);
    eventBus.post(new CommentUpdatedEvent(comment));
    return saved;
  }

  public void delete(Comment comment)throws NoSuchCommentException  {
    commentsRepository.delete(comment);
    eventBus.post(new CommentDeletedEvent(comment));
  }

  public void delete(String id) throws NoSuchCommentException {
    delete(commentsRepository.findById(id).get());
  }

  public void delete(String name, String id) throws NoSuchCommentException {
    commentsRepository.findByResourceIdAndInstanceId(name, id).forEach(this::delete);
  }

  public Comment findById(String id) {
    Comment comment = commentsRepository.findById(id).orElse(null);
    if(comment == null) throw NoSuchCommentException.withId(id);
    return comment;
  }

  public List<Comment> findCommentAndNext(String commentId, String resourceId, String instanceId) {
    List<Comment> comments = commentsRepository.findCommentAndNext(new ObjectId(commentId), resourceId, instanceId, LIMITER);
    if (comments.size()>1) {
      Optional<Comment> cmt = comments.stream().filter(c -> c.getId().equals(commentId)).findFirst();
      if (cmt.isPresent()) {
        // filter replies of the same type
        comments = comments.stream().filter(c -> c.getAdmin() == cmt.get().getAdmin()).collect(Collectors.toList());
      }
    }
    return comments;
  }

  public List<Comment> findByResourceAndInstance(String name, String id) {
    return commentsRepository.findByResourceIdAndInstanceId(name, id);
  }

  public List<Comment> findPublicComments(String name, String id) {
    return commentsRepository.findPublicCommentsByResourceIdAndInstanceId(name, id);
  }

  public int countPublicComments(String name, String id) {
    return commentsRepository.countPublicCommentsByResourceIdAndInstanceId(name, id);
  }

  public List<Comment> findPrivateComments(String name, String id) {
    return commentsRepository.findByResourceIdAndInstanceIdAndAdminIsTrue(name, id);
  }

  public int countPrivateComments(String name, String id) {
    return commentsRepository.countByResourceIdAndInstanceIdAndAdminIsTrue(name, id);
  }
}
