/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.Comment;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class CommentDtos {

  @NotNull
  Mica.CommentDto asDto(@NotNull Comment comment) {
    Mica.CommentDto.Builder builder = Mica.CommentDto.newBuilder() //
      .setId(comment.getId()) //
      .setMessage(comment.getMessage()) //
      .setClassName(comment.getClassName()) //
      .setInstanceId(comment.getInstanceId()) //
      .setCreatedBy(comment.getCreatedBy()) //
      .setTimestamps(TimestampsDtos.asDto(comment));

    String modifiedBy = comment.getLastModifiedBy();
    if (!Strings.isNullOrEmpty(modifiedBy)) builder.setModifiedBy(modifiedBy);

    return builder.build();
  }

  @NotNull
  Comment fromDto(@NotNull Mica.CommentDtoOrBuilder dto) {
    Comment comment = Comment.newBuilder() //
      .id(dto.getId()) //
      .message(dto.getMessage()) //
      .className(dto.getClassName()) //
      .instanceId(dto.getInstanceId()) //
      .build();

    if (dto.hasModifiedBy()) comment.setLastModifiedBy(dto.getModifiedBy());
    TimestampsDtos.fromDto(dto.getTimestamps(), comment);

    return comment;
  }
}
