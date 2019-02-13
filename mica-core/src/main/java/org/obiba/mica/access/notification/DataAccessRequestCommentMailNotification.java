/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.notification;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.notification.MailNotification;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.domain.SubjectAcl.Type;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@Component
public class DataAccessRequestCommentMailNotification implements MailNotification<Comment>  {

  private DataAccessFormService dataAccessFormService;

  private DataAccessRequestUtilService dataAccessRequestUtilService;

  private DataAccessRequestService dataAccessRequestService;

  private MailService mailService;

  private SubjectAclService subjectAclService;

  private MicaConfigService micaConfigService;

  @Inject
  public DataAccessRequestCommentMailNotification(
    DataAccessFormService dataAccessFormService,
    DataAccessRequestUtilService dataAccessRequestUtilService,
    DataAccessRequestService dataAccessRequestService,
    MailService mailService,
    SubjectAclService subjectAclService,
    MicaConfigService micaConfigService) {
    this.dataAccessFormService = dataAccessFormService;
    this.dataAccessRequestUtilService = dataAccessRequestUtilService;
    this.dataAccessRequestService = dataAccessRequestService;
    this.mailService = mailService;
    this.subjectAclService = subjectAclService;
    this.micaConfigService = micaConfigService;
  }

  @Override
  public void send(Comment comment) {
    if(comment == null) return;
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (!dataAccessForm.isNotifyCommented()) return;

    DataAccessRequest request = dataAccessRequestService.findById(comment.getInstanceId());
    Map<String, String> ctx = Maps.newHashMap();
    String organization = micaConfigService.getConfig().getName();
    String id = request.getId();
    String title = dataAccessRequestUtilService.getRequestTitle(request);

    ctx.put("organization", organization);
    ctx.put("publicUrl", micaConfigService.getPublicUrl());
    ctx.put("id", id);
    if(Strings.isNullOrEmpty(title)) title = id;
    ctx.put("title", title);
    ctx.put("applicant", request.getApplicant());
    ctx.put("status", request.getStatus().name());

    if (comment.getAdmin()) {
      List<SubjectAcl> privateCommentsAcls = getPrivateCommentsAcls();

      mailService.sendEmailToGroupsAndUsers(
        dataAccessForm.getCommentedSubject(),
        "dataAccessRequestCommentAdded",
        ctx,
        getAclForType(privateCommentsAcls, Type.GROUP),
        getAclForType(privateCommentsAcls, Type.USER));

    } else {
      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getCommentedSubject(), ctx, DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT),
        "dataAccessRequestCommentAdded", ctx, request.getApplicant());
    }

    mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getCommentedSubject(), ctx, DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT),
      "dataAccessRequestCommentAdded", ctx, Roles.MICA_DAO);
  }

  private List<SubjectAcl> getPrivateCommentsAcls() {
    return subjectAclService.findByResourceInstance("/data-access-request/private-comment", "*");
  }

  private List<String> getAclForType(List<SubjectAcl> acls, Type type) {
    return acls.stream().filter(acl -> type.equals(acl.getType())).map(SubjectAcl::getPrincipal).collect(toList());
  }

}
