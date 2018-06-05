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

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.domain.NoSuchCommentException;
import org.obiba.mica.core.notification.MailNotification;
import org.obiba.mica.core.repository.CommentsRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CommentsService {

  @Inject
  CommentsRepository commentsRepository;

  private static final Pageable LIMITER = new PageRequest(0, 2);

  public Comment save(Comment comment, MailNotification<Comment> mailNotification) {
    if (comment.getMessage().isEmpty()) throw new IllegalArgumentException("Comment message cannot be empty");

    Comment saved = comment;

    if (!comment.isNew()) {
      saved = commentsRepository.findOne(comment.getId());
      if(saved == null) {
        saved = comment;
      } else {
        BeanUtils.copyProperties(comment, saved, "id", "classId", "createdBy", "createdDate");
      }

      saved.setLastModifiedDate(DateTime.now());
    }

    if (mailNotification != null) mailNotification.send(saved);

    return commentsRepository.save(saved);
  }

  public void delete(Comment comment)throws NoSuchCommentException  {
    commentsRepository.delete(comment);
  }

  public void delete(String id) throws NoSuchCommentException {
    delete(commentsRepository.findOne(id));
  }

  public void delete(String name, String id) throws NoSuchCommentException {
    commentsRepository.findByResourceIdAndInstanceId(name, id).forEach(this::delete);
  }

  public Comment findById(String id) {
    Comment comment = commentsRepository.findOne(id);
    if(comment == null) throw NoSuchCommentException.withId(id);
    return comment;
  }

  public List<Comment> findCommentAndNext(String commentId, String resourceId, String instanceId) {
    return commentsRepository.findCommentAndNext(commentId, resourceId, instanceId, LIMITER);
  }

  public List<Comment> findByResourceAndInstance(String name, String id) {
    return commentsRepository.findByResourceIdAndInstanceId(name, id);
  }

  public List<Comment> findPublicComments(String name, String id) {
    return commentsRepository.findPublicCommentsByResourceIdAndInstanceId(name, id);
  }

  public List<Comment> findPrivateComments(String name, String id) {
    return commentsRepository.findByResourceIdAndInstanceIdAndAdminIsTrue(name, id);
  }
}
