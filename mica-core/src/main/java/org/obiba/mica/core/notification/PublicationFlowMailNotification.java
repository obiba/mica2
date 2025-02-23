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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.math3.util.Pair;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public abstract class PublicationFlowMailNotification {

  @Inject
  protected MicaConfigService micaConfigService;

  @Inject
  protected SubjectAclService subjectAclService;

  @Inject
  protected MailService mailService;

  protected List<SubjectAcl> getResourceAcls(String resource, String instance) {
    return subjectAclService.findByResourceInstance(resource, instance);
  }

  protected Map<String, String> createContext() {
    Map<String, String> ctx = Maps.newHashMap();
    ctx.put("organization", micaConfigService.getConfig().getName());
    ctx.put("publicUrl", micaConfigService.getPublicUrl());

    return ctx;
  }

  protected void sendNotification(RevisionStatus status, Map<String, String> ctx, String subject,
    String template, List<SubjectAcl> acls) {

    List<SubjectAcl> allAcls = Lists.newArrayList(
      SubjectAcl.newBuilder(Roles.MICA_REVIEWER, SubjectAcl.Type.GROUP).action(PermissionsUtils.getReviewerActions())
        .build(),
      SubjectAcl.newBuilder(Roles.MICA_EDITOR, SubjectAcl.Type.GROUP).action(PermissionsUtils.getEditorActions())
        .build());

    allAcls.addAll(acls);

    Map<RevisionStatus, String> requiredActions = new HashMap<RevisionStatus, String>() {
      {
        put(RevisionStatus.DRAFT, "EDIT");
        put(RevisionStatus.UNDER_REVIEW, "PUBLISH");
        put(RevisionStatus.DELETED, "DELETE");
      }
    };

    Map<SubjectAcl.Type, List<String>> recipients = allAcls.stream()
      .filter(a -> a.getActions().contains(requiredActions.get(status)))
      .map(a -> Pair.create(a.getPrincipal(), a.getType()))
      .collect(groupingBy(Pair::getSecond, mapping(Pair::getFirst, toList())));

    if(recipients.isEmpty()) return;

    mailService.sendEmailToGroupsAndUsers(subject, template, ctx,
      recipients.getOrDefault(SubjectAcl.Type.GROUP, Lists.newArrayList()),
      recipients.getOrDefault(SubjectAcl.Type.USER, Lists.newArrayList()));
  }

}
