/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.notification;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static java.util.stream.Collectors.toList;

@Component
public class CommentMailNotification implements MailNotification<Comment> {

  private static final String DEFAULT_NOTIFICATION_SUBJECT = "[${organization}] ${documentId} was commented";

  @Inject
  MailService mailService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private SubjectAclService subjectAclService;

  @Override
  public void send(Comment comment) {
    MicaConfig config = micaConfigService.getConfig();

    if(comment == null || !config.isCommentNotificationsEnabled()) return;

    List<SubjectAcl> acls = Lists.newArrayList(
      SubjectAcl.newBuilder(Roles.MICA_REVIEWER, SubjectAcl.Type.GROUP).action(PermissionsUtils.asActions("REVIEWER"))
        .build(),
      SubjectAcl.newBuilder(Roles.MICA_EDITOR, SubjectAcl.Type.GROUP).action(PermissionsUtils.asActions("EDITOR"))
        .build());

    acls.addAll(subjectAclService.findByResourceInstance(comment.getResourceId(), comment.getInstanceId()));
    List<String> users = acls.stream().filter(s -> s.hasAction("VIEW") && s.getType() == SubjectAcl.Type.USER &&
      !s.getPrincipal().equals(comment.getCreatedBy())).map(SubjectAcl::getPrincipal).collect(toList());
    List<String> groups = acls.stream().filter(s -> s.hasAction("VIEW") && s.getType() == SubjectAcl.Type.GROUP)
      .map(SubjectAcl::getPrincipal).collect(toList());

    if(users.isEmpty() && groups.isEmpty()) return;

    Map<String, String> ctx = Maps.newHashMap();
    ctx.put("organization", config.getName());
    ctx.put("publicUrl", micaConfigService.getPublicUrl());
    ctx.put("documentType", comment.getResourceId().replaceFirst("/draft/", ""));
    ctx.put("documentId", comment.getInstanceId());
    ctx.put("createdBy", comment.getCreatedBy());
    ctx.put("message", comment.getMessage());
    ctx.put("status", comment.isNew() ? "CREATED" : "UPDATED");

    String commentNotificationSubject = micaConfigService.getConfig().getCommentNotificationsSubject();

    mailService
      .sendEmailToGroupsAndUsers(mailService.getSubject(commentNotificationSubject, ctx, DEFAULT_NOTIFICATION_SUBJECT),
        "commentAdded", ctx, groups, users);
  }
}
