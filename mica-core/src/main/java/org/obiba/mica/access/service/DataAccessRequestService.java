/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.ByteStreams;
import com.itextpdf.text.DocumentException;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.*;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.event.CommentDeletedEvent;
import org.obiba.mica.core.event.CommentUpdatedEvent;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

@Service
@Validated
public class DataAccessRequestService extends DataAccessEntityService<DataAccessRequest> {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestService.class);

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  @Inject
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Value("classpath:config/data-access-form/data-access-request-template.pdf")
  private Resource defaultTemplateResource;

  @Override
  protected DataAccessEntityRepository<DataAccessRequest> getRepository() {
    return dataAccessRequestRepository;
  }

  @Override
  public DataAccessRequest save(@NotNull DataAccessRequest request) {
    return save(request, LocalDateTime.now());
  }

  public DataAccessRequest archive(@NotNull DataAccessRequest request, boolean archived) {
    request.setArchived(archived);
    request.getActionLogHistory().add(ActionLog.newBuilder().action(archived ? "Archived" : "Unarchived")
      .changedOn(LocalDateTime.now())
      .author(SecurityUtils.getSubject().getPrincipal().toString()).build());
    return save(request, null);
  }

  public DataAccessRequest saveActionsLogs(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = findById(request.getId());
    saved.setActionLogHistory(request.getActionLogHistory());
    save(saved, null);
    return saved;
  }

  public DataAccessRequest saveAttachments(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = findById(request.getId());
    saved.setAttachments(request.getAttachments());
    save(saved);
    sendAttachmentsUpdatedNotificationEmail(request);
    return saved;
  }

  public DataAccessRequest changeApplicantAndSave(@NotNull DataAccessRequest request, String applicant) {
    if (!request.getApplicant().equals(applicant)) {
      String originalApplicant = request.getApplicant();
      request.setApplicant(applicant);
      save(request);

      dataAccessAmendmentService.findByParentId(request.getId()).forEach(amendment -> {
        dataAccessAmendmentService.changeApplicantAndSave(amendment, applicant);
      });
      dataAccessFeasibilityService.findByParentId(request.getId()).forEach(feasibility -> {
        dataAccessFeasibilityService.changeApplicantAndSave(feasibility, applicant);
      });

      subjectAclService.applyPrincipal(
        subjectAclService.findBySubject(originalApplicant, SubjectAcl.Type.USER).stream()
          .filter(acl -> (acl.getResource().equals("/data-access-request") && acl.getInstance().equals(request.getId()))
            || acl.getResource().startsWith(String.format("/data-access-request/%s/", request.getId()))).collect(Collectors.toList()),
        SubjectAcl.Type.USER, applicant);
    }
    return request;
  }

  /**
   * Delete the {@link DataAccessRequest} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  @Override
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    DataAccessRequest dataAccessRequest = findById(id);
    List<Attachment> attachments = dataAccessRequest.getAttachments();

    dataAccessRequestRepository.deleteWithReferences(dataAccessRequest);
    schemaFormContentFileService.deleteFiles(dataAccessRequest);
    deleteAmendments(id);
    deleteFeasibilities(id);
    deleteCollaborators(id);

    attachments.forEach(a -> fileStoreService.delete(a.getId()));
    eventBus.post(new DataAccessRequestDeletedEvent(dataAccessRequest));
  }

  public byte[] getRequestPdf(String id, String lang) {
    DataAccessRequest dataAccessRequest = findById(id);
    ByteArrayOutputStream ba = new ByteArrayOutputStream();
    Object content = defaultConfiguration().jsonProvider().parse(dataAccessRequest.getContent());
    try {
      fillPdfTemplateFromRequest(getTemplate(dataAccessRequest, Locale.forLanguageTag(lang)), ba, content);
    } catch (IOException | DocumentException e) {
      throw Throwables.propagate(e);
    }

    return ba.toByteArray();
  }

  public boolean isPreliminaryEnabled() {
    return dataAccessConfigService.getOrCreateConfig().isPreliminaryEnabled();
  }

  public boolean isFeasibilityEnabled() {
    return dataAccessConfigService.getOrCreateConfig().isFeasibilityEnabled();
  }

  public boolean isAgreementEnabled() {
    return dataAccessConfigService.getOrCreateConfig().isAgreementEnabled();
  }

  public boolean isAmendmentsEnabled() {
    return dataAccessConfigService.getOrCreateConfig().isAmendmentsEnabled();
  }

  //
  // Event handling
  //

  @Async
  @Subscribe
  public void dataAccessFeasibilityUpdated(DataAccessFeasibilityUpdatedEvent event) {
    touch(findById(event.getPersistable().getParentId()));
  }

  @Async
  @Subscribe
  public void dataAccessFeasibilityDeleted(DataAccessFeasibilityDeletedEvent event) {
    touch(findById(event.getPersistable().getParentId()));
  }

  @Async
  @Subscribe
  public void dataAccessAmendmentUpdated(DataAccessAmendmentUpdatedEvent event) {
    touch(findById(event.getPersistable().getParentId()));
  }

  @Async
  @Subscribe
  public void dataAccessAmendmentDeleted(DataAccessAmendmentDeletedEvent event) {
    touch(findById(event.getPersistable().getParentId()));
  }

  @Async
  @Subscribe
  public void commentUpdated(CommentUpdatedEvent event) {
    Comment comment = event.getComment();
    if (comment.getResourceId().equals("/data-access-request") && !comment.getAdmin()) {
      touch(findById(comment.getInstanceId()));
    }
  }

  @Async
  @Subscribe
  public void commentDeleted(CommentDeletedEvent event) {
    Comment comment = event.getComment();
    if (comment.getResourceId().equals("/data-access-request") && !comment.getAdmin()) {
      touch(findById(comment.getInstanceId()));
    }
  }

  @Subscribe
  public void dataAccessCollaboratorAccepted(DataAccessCollaboratorAcceptedEvent event) {
    DataAccessRequest request = findById(event.getPersistable().getRequestId());
    request.addAcceptedCollaboratorInvitation(event.getKey());
    save(request);
  }

  //
  // Private methods
  //

  private void touch(@NotNull DataAccessRequest request) {
    request.setLastModifiedDate(LocalDateTime.now());
    dataAccessRequestRepository.saveWithReferences(request);
  }

  private DataAccessRequest save(@NotNull DataAccessRequest request, LocalDateTime lastModifiedDate) {
    DataAccessRequest saved = request;
    DataAccessEntityStatus from = null;
    Iterable<Attachment> attachmentsToDelete = null;
    Iterable<Attachment> attachmentsToSave = null;
    boolean isNew = request.isNew();
    if (isNew) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(generateId());
      attachmentsToSave = saved.getAttachments();
    } else {
      Optional<DataAccessRequest> found = dataAccessRequestRepository.findById(request.getId());
      if (found.isPresent()) {
        saved = found.get();
        if (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) &&
          !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
          // preserve current actionLogs as no other user role can add or remove them
          request.setActionLogHistory(saved.getActionLogHistory());
        }

        attachmentsToDelete = Sets.difference(Sets.newHashSet(saved.getAttachments()), Sets.newHashSet(request.getAttachments()));
        attachmentsToSave = Sets.difference(Sets.newHashSet(request.getAttachments()), Sets.newHashSet(saved.getAttachments()));

        from = saved.getStatus();
        // validate the status
        dataAccessRequestUtilService.checkStatusTransition(saved, request.getStatus());
        saved.setStatus(request.getStatus());
        if (request.hasStatusChangeHistory()) saved.setStatusChangeHistory(request.getStatusChangeHistory());
        // merge beans
        BeanUtils.copyProperties(request, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "statusChangeHistory");
      } else {
        saved = request;
        setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      }
    }

    schemaFormContentFileService.save(saved, dataAccessRequestRepository.findById(request.getId()),
      String.format("/data-access-request/%s", saved.getId()));

    if (attachmentsToSave != null) attachmentsToSave.forEach(a -> {
      fileStoreService.save(a.getId());
      a.setJustUploaded(false);
      attachmentRepository.insert(a);
    });

    if (lastModifiedDate != null)
      saved.setLastModifiedDate(lastModifiedDate);

    if (isNew) {
      dataAccessRequestRepository.insertWithReferences(saved);
    } else {
      dataAccessRequestRepository.saveWithReferences(saved);
    }

    if (attachmentsToDelete != null) attachmentsToDelete.forEach(a -> fileStoreService.delete(a.getId()));

    if (saved.hasVariablesSet() && DataAccessEntityStatus.OPENED.equals(saved.getStatus())) {
      variableSetService.setLock(saved.getVariablesSet(), false);
    }

    eventBus.post(new DataAccessRequestUpdatedEvent(saved));
    sendNotificationEmails(saved, from);
    return saved;
  }

  private void deleteAmendments(String id) {
    dataAccessAmendmentService.findByParentId(id).forEach(dataAccessAmendmentService::delete);
  }

  private void deleteFeasibilities(String id) {
    dataAccessFeasibilityService.findByParentId(id).forEach(dataAccessFeasibilityService::delete);
  }

  private void deleteCollaborators(String id) {
    dataAccessCollaboratorService.deleteAll(id);
  }

  private byte[] getTemplate(DataAccessRequest request, Locale locale) throws IOException {
    DataAccessForm dataAccessForm = dataAccessFormService.findByRevision(request.hasFormRevision() ? request.getFormRevision().toString() : "latest").get();
    Attachment pdfTemplate = dataAccessForm.getPdfTemplates().get(locale);

    if (pdfTemplate == null) {
      Map<Locale, Attachment> pdfTemplates = dataAccessForm.getPdfTemplates();
      if (!pdfTemplates.isEmpty()) {
        pdfTemplate = dataAccessForm.getPdfTemplates().get(Locale.ENGLISH);
        if (pdfTemplate == null) pdfTemplate = dataAccessForm.getPdfTemplates().values().stream().findFirst().get();
      }
    }

    return pdfTemplate != null ? ByteStreams.toByteArray(fileStoreService.getFile(pdfTemplate.getFileReference())) :
      ByteStreams.toByteArray(defaultTemplateResource.getInputStream());
  }

}
