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
import com.google.common.io.ByteStreams;
import com.itextpdf.text.DocumentException;
import org.apache.shiro.SecurityUtils;
import org.joda.time.DateTime;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestUpdatedEvent;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

@Service
@Validated
public class DataAccessRequestService extends DataAccessEntityService<DataAccessRequest> {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestService.class);

  public static final String DAR_ROOT_KEY =  "###ROOT###";

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

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
    DataAccessRequest saved = request;
    DataAccessEntityStatus from = null;
    Iterable<Attachment> attachmentsToDelete = null;
    Iterable<Attachment> attachmentsToSave = null;

    if(request.isNew()) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(generateId());
      attachmentsToSave = saved.getAttachments();
    } else {
      saved = dataAccessRequestRepository.findOne(request.getId());
      if(saved != null) {
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
        if(request.hasStatusChangeHistory()) saved.setStatusChangeHistory(request.getStatusChangeHistory());
        // merge beans
        BeanUtils.copyProperties(request, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "statusChangeHistory");
      } else {
        saved = request;
        setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      }
    }

    schemaFormContentFileService.save(saved, dataAccessRequestRepository.findOne(request.getId()),
        String.format("/data-access-request/%s", saved.getId()));

    if(attachmentsToSave != null) attachmentsToSave.forEach(a -> {
      fileStoreService.save(a.getId());
      a.setJustUploaded(false);
      attachmentRepository.save(a);
    });

    saved.setLastModifiedDate(DateTime.now());
    dataAccessRequestRepository.saveWithReferences(saved);

    if(attachmentsToDelete != null) attachmentsToDelete.forEach(a -> fileStoreService.delete(a.getId()));

    eventBus.post(new DataAccessRequestUpdatedEvent(saved));
    sendNotificationEmails(saved, from);
    return saved;
  }

  public Map<String, List<StatusChange>> getMergedStatusChangHistory(String dataAccessRequestId) {
    Map<String, List<StatusChange>> congregatedAmendmentStatusChanges = dataAccessAmendmentService
      .getCongregatedAmendmentStatusChangesFor(dataAccessRequestId);

    congregatedAmendmentStatusChanges.put(DAR_ROOT_KEY, findById(dataAccessRequestId).getStatusChangeHistory());
    return congregatedAmendmentStatusChanges;
  }

  public DataAccessRequest saveActionsLogs(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = findById(request.getId());
    saved.setActionLogHistory(request.getActionLogHistory());
    save(saved);
    return saved;
  }

  public DataAccessRequest saveAttachments(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = findById(request.getId());
    saved.setAttachments(request.getAttachments());
    save(saved);
    sendAttachmentsUpdatedNotificationEmail(request);
    return saved;
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

    attachments.forEach(a -> fileStoreService.delete(a.getId()));
    eventBus.post(new DataAccessRequestDeletedEvent(dataAccessRequest));
  }

  public byte[] getRequestPdf(String id, String lang) {
    DataAccessRequest dataAccessRequest = findById(id);
    ByteArrayOutputStream ba = new ByteArrayOutputStream();
    Object content = defaultConfiguration().jsonProvider().parse(dataAccessRequest.getContent());
    try {
      fillPdfTemplateFromRequest(getTemplate(Locale.forLanguageTag(lang)), ba, content);
    } catch(IOException | DocumentException e) {
      throw Throwables.propagate(e);
    }

    return ba.toByteArray();
  }

  public boolean isAmendmentEnabled() {
    return dataAccessFormService.find().map(DataAccessForm::isAmendmentsEnabled).orElse(false);
  }

  private void deleteAmendments(String id) {
    List<DataAccessAmendment> amendments = dataAccessAmendmentService.findByParentId(id);
    amendments.stream().forEach(dataAccessAmendmentService::delete);
  }

  private byte[] getTemplate(Locale locale) throws IOException {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
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
