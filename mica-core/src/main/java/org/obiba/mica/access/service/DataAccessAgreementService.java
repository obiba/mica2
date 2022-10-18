/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.obiba.mica.access.DataAccessAgreementRepository;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.access.event.DataAccessAgreementUpdatedEvent;
import org.obiba.mica.access.event.DataAccessCollaboratorDeletedEvent;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class DataAccessAgreementService extends DataAccessEntityService<DataAccessAgreement> {

  private static final Logger log = LoggerFactory.getLogger(DataAccessAgreementService.class);

  @Inject
  DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessAgreementRepository dataAccessAgreementRepository;

  @Inject
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  @Inject
  private SubjectAclService subjectAclService;

  @Override
  protected DataAccessEntityRepository<DataAccessAgreement> getRepository() {
    return dataAccessAgreementRepository;
  }

  @Override
  public DataAccessAgreement save(@NotNull DataAccessAgreement agreement) {
    DataAccessAgreement saved = agreement;
    DataAccessEntityStatus from = null;
    boolean agreementIsNew = agreement.isNew();

    if (agreementIsNew) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(makeAgreementId(agreement.getParentId(), agreement.getApplicant()));
    } else {
      saved = dataAccessAgreementRepository.findById(agreement.getId()).orElse(null);
      if (saved != null) {
        from = saved.getStatus();
        // validate the status
        dataAccessRequestUtilService.checkStatusTransition(saved, agreement.getStatus());
        saved.setStatus(agreement.getStatus());
        if (agreement.hasStatusChangeHistory()) saved.setStatusChangeHistory(agreement.getStatusChangeHistory());
        // merge beans
        BeanUtils.copyProperties(agreement, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "statusChangeHistory");
      } else {
        saved = agreement;
        setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      }
    }

    schemaFormContentFileService.save(saved, dataAccessAgreementRepository.findById(agreement.getId()),
      String.format("/data-access-request/%s/agreement/%s", saved.getParentId(), agreement.getId()));

    saved.setLastModifiedDate(LocalDateTime.now());

    if(agreementIsNew) dataAccessAgreementRepository.insert(saved);
    else dataAccessAgreementRepository.save(saved);
    eventBus.post(new DataAccessAgreementUpdatedEvent(saved));
    sendNotificationEmails(saved, from);
    return saved;
  }

  @Override
  protected void sendApprovedNotificationEmail(DataAccessAgreement request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyApproved()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToGroups(mailService.getSubject(dataAccessConfig.getApprovedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ApprovedDAOEmail", ctx,
        Roles.MICA_DAO);
    }
  }

  @Override
  protected void sendRejectedNotificationEmail(DataAccessAgreement request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyApproved()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToGroups(mailService.getSubject(dataAccessConfig.getApprovedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "RejectedDAOEmail", ctx,
        Roles.MICA_DAO);
    }
  }

  @Override
  protected String getMainRequestId(DataAccessAgreement request) {
    return request.getParentId();
  }

  public List<DataAccessAgreement> findByStatus(@Nullable String parentId, @Nullable List<String> status) {
    if (status == null || status.isEmpty()) return dataAccessAgreementRepository.findByParentId(parentId);

    List<DataAccessEntityStatus> statusList = status.stream().map(DataAccessEntityStatus::valueOf)
      .collect(Collectors.toList());

    return dataAccessAgreementRepository.findByParentId(parentId).stream()
      .filter(dar -> statusList.contains(dar.getStatus())).collect(Collectors.toList());
  }

  public List<DataAccessAgreement> getOrCreate(@NotNull DataAccessRequest dar) {
    Set<String> principals = dataAccessCollaboratorService.findByRequestId(dar.getId()).stream()
      .filter(DataAccessCollaborator::hasPrincipal)
      .map(DataAccessCollaborator::getPrincipal)
      .collect(Collectors.toSet());
    principals.add(dar.getApplicant());
    Map<String, DataAccessAgreement> agreementsMap = dataAccessAgreementRepository.findByParentId(dar.getId()).stream()
      .collect(Collectors.toMap(DataAccessEntity::getApplicant, Function.identity()));
    List<DataAccessAgreement> agreements = Lists.newArrayList();
    for (String principal : principals) {
      if (agreementsMap.containsKey(principal))
        agreements.add(agreementsMap.get(principal));
      else {
        DataAccessAgreement agreement = (DataAccessAgreement) DataAccessAgreement.newBuilder()
          .parentId(dar.getId()).applicant(principal).build();
        agreements.add(save(agreement));
        String resource = "/data-access-request/" + agreement.getParentId() + "/agreement";
        if (!subjectAclService.isPermitted(resource, "EDIT", agreement.getId())) {
          subjectAclService.addUserPermission(agreement.getApplicant(), resource, "EDIT", agreement.getId());
          subjectAclService.addUserPermission(agreement.getApplicant(), resource + "/" + agreement.getId(), "EDIT", "_status");
        }
      }
    }
    Collections.sort(agreements, Comparator.comparing(AbstractAuditableDocument::getId));
    // TODO delete agreements that do not apply any more (case of a collaborator was removed)
    return agreements;
  }

  public List<DataAccessAgreement> findByParentId(@NotNull String parentId) {
    return dataAccessAgreementRepository.findByParentId(parentId);
  }

  public int countByParentId(@NotNull String parentId) {
    return dataAccessAgreementRepository.countByParentId(parentId);
  }

  public int countPendingByParentId(@NotNull String parentId) {
    return dataAccessAgreementRepository.countPendingByParentId(parentId);
  }

  /**
   * Delete the {@link DataAccessAgreement} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  @Override
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    DataAccessAgreement agreement = findById(id);
    String resource = "/data-access-request/" + agreement.getParentId() + "/agreement";
    subjectAclService.removeUserPermission(agreement.getApplicant(), resource, "EDIT", agreement.getId());
    subjectAclService.removeUserPermission(agreement.getApplicant(), resource + "/" + agreement.getId(), "EDIT", "_status");
    schemaFormContentFileService.deleteFiles(agreement);
    dataAccessAgreementRepository.delete(agreement);
  }

  //
  // Events handling
  //

  @Subscribe
  public void dataAccessCollaboratorDeleted(DataAccessCollaboratorDeletedEvent event) {
    if (!event.getPersistable().hasPrincipal()) return;
    try {
      delete(makeAgreementId(event.getPersistable().getRequestId(), event.getPersistable().getPrincipal()));
    } catch (Exception e) {
      // ignore
    }
  }

  //
  // Private methods
  //

  private String makeAgreementId(String requestId, String applicant) {
    return String.format("%s-%s", requestId, applicant);
  }
}
