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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.io.ByteStreams;
import com.itextpdf.text.DocumentException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.shiro.SecurityUtils;
import org.joda.time.DateTime;
import org.obiba.mica.PdfUtils;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestUpdatedEvent;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.core.service.SchemaFormContentFileService;
import org.obiba.mica.core.support.IdentifierGenerator;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

@Service
@Validated
public class DataAccessRequestService {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestService.class);

  private static final Configuration conf = defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private SchemaFormContentFileService schemaFormContentFileService;

  @Inject
  private EventBus eventBus;

  @Inject
  private MailService mailService;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private MicaConfigService micaConfigService;

  @Value("classpath:config/data-access-form/data-access-request-template.pdf")
  private Resource defaultTemplateResource;

  public DataAccessRequest save(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = request;
    DataAccessRequest.Status from = null;
    Iterable<Attachment> attachmentsToDelete = null;
    Iterable<Attachment> attachmentsToSave = null;

    if(request.isNew()) {
      setAndLogStatus(saved, DataAccessRequest.Status.OPENED);
      saved.setId(generateId());
      attachmentsToSave = saved.getAttachments();
    } else {
      saved = dataAccessRequestRepository.findOne(request.getId());
      if(saved != null) {
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
        setAndLogStatus(saved, DataAccessRequest.Status.OPENED);
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
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    DataAccessRequest dataAccessRequest = findById(id);
    List<Attachment> attachments = dataAccessRequest.getAttachments();

    dataAccessRequestRepository.deleteWithReferences(dataAccessRequest);
    schemaFormContentFileService.deleteFiles(dataAccessRequest);

    attachments.forEach(a -> fileStoreService.delete(a.getId()));
    eventBus.post(new DataAccessRequestDeletedEvent(dataAccessRequest));
  }

  /**
   * Update the status of the {@link DataAccessRequest} matching the identifier.
   *
   * @param id
   * @param status
   * @throws NoSuchDataAccessRequestException
   */
  public DataAccessRequest updateStatus(@NotNull String id, @NotNull DataAccessRequest.Status status)
    throws NoSuchDataAccessRequestException {
    DataAccessRequest request = findById(id);
    setAndLogStatus(request, status);
    save(request);
    return request;
  }

  /**
   * Update the content of the {@link DataAccessRequest} matching the identifier.
   *
   * @param id
   * @param content
   */
  public void updateContent(@NotNull String id, String content) {
    DataAccessRequest request = findById(id);
    if(request.getStatus() != DataAccessRequest.Status.OPENED)
      throw new IllegalArgumentException("Data access request content can only be modified when status is draft");
    request.setContent(content);
    save(request);
  }

  //
  // Finders
  //

  /**
   * Get the {@link DataAccessRequest} matching the identifier.
   *
   * @param id
   * @return
   * @throws NoSuchDataAccessRequestException
   */
  @NotNull
  public DataAccessRequest findById(@NotNull String id) throws NoSuchDataAccessRequestException {
    DataAccessRequest request = dataAccessRequestRepository.findOne(id);
    if(request == null) throw NoSuchDataAccessRequestException.withId(id);
    return request;
  }

  /**
   * Get all {@link DataAccessRequest}s, optionally filtered by applicant.
   *
   * @param applicant
   * @return
   */
  public List<DataAccessRequest> findAll(@Nullable String applicant) {
    if(Strings.isNullOrEmpty(applicant)) return dataAccessRequestRepository.findAll();
    return dataAccessRequestRepository.findByApplicant(applicant);
  }

  public List<DataAccessRequest> findByStatus(@Nullable List<String> status) {
    if(status == null || status.isEmpty()) return dataAccessRequestRepository.findAll();
    List<DataAccessRequest.Status> statusList = status.stream().map(DataAccessRequest.Status::valueOf)
      .collect(Collectors.toList());

    return dataAccessRequestRepository.findAll().stream().filter(dar -> statusList.contains(dar.getStatus()))
      .collect(Collectors.toList());
  }

  //
  // Private methods
  //

  /**
   * Send a notification email, depending on the status of the request.
   *
   * @param request
   * @param from
   */
  private void sendNotificationEmails(DataAccessRequest request, @Nullable DataAccessRequest.Status from) {
    // check is new request
    if(from == null) return;

    // check no transition
    if (request.getStatus() == from) return;

    switch(request.getStatus()) {
      case SUBMITTED:
        sendSubmittedNotificationEmail(request);
        break;
      case REVIEWED:
        sendReviewedNotificationEmail(request);
        break;
      case OPENED:
        sendOpenedNotificationEmail(request);
        break;
      case CONDITIONALLY_APPROVED:
        sendConditionallyApprovedEmail(request);
        break;
      case APPROVED:
        sendApprovedNotificationEmail(request);
        break;
      case REJECTED:
        sendRejectedNotificationEmail(request);
        break;
    }
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

  //
  // Private methods
  //

  private Map<String, String> getNotificationEmailContext(DataAccessRequest request) {
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

    return ctx;
  }

  private void sendSubmittedNotificationEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if(dataAccessForm.isNotifySubmitted()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getSubmittedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestSubmittedApplicantEmail", ctx,
        request.getApplicant());
      mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getSubmittedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestSubmittedDAOEmail", ctx,
        Roles.MICA_DAO);
    }
  }

  private void sendConditionallyApprovedEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyConditionallyApproved()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getConditionallyApprovedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestConditionallyApprovedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  private void sendReviewedNotificationEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if(dataAccessForm.isNotifyReviewed() && dataAccessForm.isWithReview()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getReviewedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestReviewedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  private void sendOpenedNotificationEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if(dataAccessForm.isNotifyReopened()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getReopenedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestReopenedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  private void sendApprovedNotificationEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if(dataAccessForm.isNotifyApproved()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getApprovedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestApprovedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  private void sendRejectedNotificationEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if(dataAccessForm.isNotifyRejected()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getRejectedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestRejectedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  private void sendAttachmentsUpdatedNotificationEmail(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if(dataAccessForm.isNotifyAttachment()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getAttachmentSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestAttachmentsUpdated", ctx,
        request.getApplicant());

      mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getAttachmentSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestAttachmentsUpdated", ctx,
        Roles.MICA_DAO);
    }
  }

  private byte[] getTemplate(Locale locale) throws IOException {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    Attachment pdfTemplate = dataAccessForm.getPdfTemplates().get(locale);

    if(pdfTemplate == null) {
      Map<Locale, Attachment> pdfTemplates = dataAccessForm.getPdfTemplates();
      if(!pdfTemplates.isEmpty()) {
        pdfTemplate = dataAccessForm.getPdfTemplates().get(Locale.ENGLISH);
        if(pdfTemplate == null) pdfTemplate = dataAccessForm.getPdfTemplates().values().stream().findFirst().get();
      }
    }

    return pdfTemplate != null ? ByteStreams.toByteArray(fileStoreService.getFile(pdfTemplate.getFileReference())) :
      ByteStreams.toByteArray(defaultTemplateResource.getInputStream());
  }

  private void fillPdfTemplateFromRequest(byte[] template, OutputStream output, Object content)
    throws IOException, DocumentException {
    Map<String, Object> requestValues = PdfUtils.getFieldNames(template).stream()
      .map(k -> getMapEntryFromContent(content, k)) //
      .filter(e -> e != null && !e.getValue().isEmpty()) //
      .map(e -> Maps.immutableEntry(e.getKey(), e.getValue().get(0))) //
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    PdfUtils.fillOutForm(template, output, requestValues);
  }

  private Map.Entry<String, List<Object>> getMapEntryFromContent(Object content, String jsonPath) {
    try {
      List<Object> values = JsonPath.using(conf).parse(content).read(jsonPath);
      return Maps.immutableEntry(jsonPath, values);
    } catch(PathNotFoundException ex) {
      //ignore
    } catch(InvalidPathException e) {
      log.warn("Invalid jsonpath {}", jsonPath);
    }

    return null;
  }

  private void setAndLogStatus(DataAccessRequest request, DataAccessRequest.Status to) {
    DataAccessRequest.Status from = request.getStatus();
    dataAccessRequestUtilService.checkStatusTransition(request, to);
    request.setStatus(to);
    request.getStatusChangeHistory().add( //
      StatusChange.newBuilder() //
        .previous(from) //
        .current(to) //
        .author(SecurityUtils.getSubject().getPrincipal().toString()) //
        .now().build() //
    ); //
  }

  private String generateId() {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    IdentifierGenerator idGenerator = IdentifierGenerator.newBuilder().prefix(dataAccessForm.getIdPrefix())
      .size(dataAccessForm.getIdLength()).zeros().build();
    while(true) {
      String id = idGenerator.generateIdentifier();
      if(dataAccessRequestRepository.findOne(id) == null) return id;
    }
  }

}
