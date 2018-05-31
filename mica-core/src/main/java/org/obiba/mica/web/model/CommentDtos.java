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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.Comment;
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
  Mica.CommentDto asDto(@NotNull Comment comment) {
    Mica.CommentDto.Builder builder = Mica.CommentDto.newBuilder() //
      .setId(comment.getId()) //
      .setMessage(comment.getMessage()) //
      .setResourceId(comment.getResourceId()) //
      .setInstanceId(comment.getInstanceId()) //
      .setCreatedBy(comment.getCreatedBy()) //
      .setTimestamps(TimestampsDtos.asDto(comment));

    String modifiedBy = comment.getLastModifiedBy();
    if (!Strings.isNullOrEmpty(modifiedBy)) builder.setModifiedBy(modifiedBy);

    if (subjectAclService.isPermitted(Paths.get(comment.getResourceId(), comment.getInstanceId(), "/comment").toString(), "EDIT", comment.getId())) {
      builder.addActions("EDIT");
    }
    if (subjectAclService.isPermitted(Paths.get(comment.getResourceId(), comment.getInstanceId(), "/comment").toString(), "DELETE", comment.getId())) {
      builder.addActions("DELETE");
    }

    Subject profile = userProfileService.getProfile(comment.getCreatedBy());
    if (profile != null) {
      builder.setCreatedByProfile(userProfileDtos.asDto(profile));
    }

    String lastModifiedBy = comment.getLastModifiedBy();
    if (!Strings.isNullOrEmpty(lastModifiedBy)) {
      profile = userProfileService.getProfile(lastModifiedBy);
      if(profile != null) {
        builder.setModifiedByProfile(userProfileDtos.asDto(profile));
      }
    }

    Boolean adminMessage = comment.getAdmin();
    if(adminMessage != null){
      builder.setAdmin(adminMessage);
    }

    return builder.build();
  }

  @NotNull
  Comment fromDto(@NotNull Mica.CommentDtoOrBuilder dto) {
    Comment.Builder commentBuilder = Comment.newBuilder() //
      .id(dto.getId()) //
      .message(dto.getMessage()) //
      .resourceId(dto.getResourceId()) //
      .instanceId(dto.getInstanceId());
    if(dto.hasAdmin()){
      commentBuilder.admin(dto.getAdmin());
    }
    Comment comment = commentBuilder.build();
    if (dto.hasModifiedBy()) comment.setLastModifiedBy(dto.getModifiedBy());
    TimestampsDtos.fromDto(dto.getTimestamps(), comment);

    return comment;
  }
}
