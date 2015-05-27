/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
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
import org.obiba.mica.core.repository.CommentsRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CommentsService {

  @Inject
  CommentsRepository commentsRepository;

  public Comment save(Comment comment) {
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

    return commentsRepository.save(saved);
  }

  public void delete(Comment comment)throws NoSuchCommentException  {
    commentsRepository.delete(comment);
  }

  public void delete(String id) throws NoSuchCommentException {
    delete(commentsRepository.findOne(id));
  }

  public Comment findById(String id) {
    Comment comment = commentsRepository.findOne(id);
    if(comment == null) throw NoSuchCommentException.withId(id);
    return comment;
  }

  public void deleteByClassId(String classId) throws NoSuchCommentException {
    commentsRepository.findByClassId(classId).forEach(this::delete);
  }

  public List<Comment> findByClassId(String id) {
    return commentsRepository.findByClassId(id);
  }

}
