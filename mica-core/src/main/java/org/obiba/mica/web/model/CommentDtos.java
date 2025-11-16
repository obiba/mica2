/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class CommentDtos {
  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private UserProfileDtos userProfileDtos;

  @NotNull
  Mica.CommentDto asDto(@NotNull Comment comment, boolean withPermission) {
    Mica.CommentDto.Builder builder = asDtoBuilder(comment);
    if (withPermission) addCommentPermissions(builder, comment);
    return builder.build();
  }

  public List<Mica.CommentDto> asDtos(@NotNull List<Comment> comments) {
    boolean canAdministrate = SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) || SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN);

    return comments.stream().map(comment -> {
      Mica.CommentDto.Builder builder = asDtoBuilder(comment);
      if (canAdministrate || comments.indexOf(comment) == (comments.size() - 1)) addCommentPermissions(builder, comment);
      return builder.build();
    }).collect(Collectors.toList());
  }

  @NotNull
  Comment fromDto(@NotNull Mica.CommentDtoOrBuilder dto) {
    Comment.Builder commentBuilder = Comment.newBuilder() //
        .id(dto.getId()) //
        .message(dto.getMessage()) //
        .resourceId(dto.getResourceId()) //
        .instanceId(dto.getInstanceId());

    if (dto.hasAdmin()) commentBuilder.admin(dto.getAdmin());
    Comment comment = commentBuilder.build();

    if (dto.hasModifiedBy()) comment.setLastModifiedBy(dto.getModifiedBy());
    TimestampsDtos.fromDto(dto.getTimestamps(), comment);

    return comment;
  }

  private void addCommentPermissions(Mica.CommentDto.Builder builder, Comment comment) {
    if (subjectAclService.isPermitted(Paths.get(comment.getResourceId(), comment.getInstanceId(), "/comment").toString(), "EDIT", comment.getId())) {
      builder.addActions("EDIT");
    }

    if (subjectAclService.isPermitted(Paths.get(comment.getResourceId(), comment.getInstanceId(), "/comment").toString(), "DELETE", comment.getId())) {
      builder.addActions("DELETE");
    }
  }

  private Mica.CommentDto.Builder asDtoBuilder(Comment comment) {
    Mica.CommentDto.Builder builder = Mica.CommentDto.newBuilder() //
      .setId(comment.getId()) //
      .setMessage(comment.getMessage()) //
      .setResourceId(comment.getResourceId()) //
      .setInstanceId(comment.getInstanceId()) //
      .setCreatedBy(comment.getCreatedBy().orElse(null)) //
      .setTimestamps(TimestampsDtos.asDto(comment));

    Optional<String> modifiedBy = comment.getLastModifiedBy();
    if (modifiedBy.isPresent() && !Strings.isNullOrEmpty(modifiedBy.get())) builder.setModifiedBy(modifiedBy.get());

    Subject profile = userProfileService.getProfile(comment.getCreatedBy().orElse(null));
    if (profile != null) {
      builder.setCreatedByProfile(userProfileDtos.asDto(profile));
    }

    Optional<String> lastModifiedBy = comment.getLastModifiedBy();
    if (lastModifiedBy.isPresent() && !Strings.isNullOrEmpty(lastModifiedBy.get())) {
      profile = userProfileService.getProfile(lastModifiedBy.get());
      if(profile != null) {
        builder.setModifiedByProfile(userProfileDtos.asDto(profile));
      }
    }

    builder.setAdmin(comment.getAdmin());

    return builder;
  }
}
