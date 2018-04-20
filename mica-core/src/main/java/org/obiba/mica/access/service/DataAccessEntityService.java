package org.obiba.mica.access.service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.itextpdf.text.DocumentException;
import com.jayway.jsonpath.*;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.PdfUtils;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.core.service.SchemaFormContentFileService;
import org.obiba.mica.core.support.IdentifierGenerator;
import org.obiba.mica.core.support.YamlClassPathResourceReader;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

public abstract class DataAccessEntityService<T extends DataAccessEntity> {
  private static final Logger log = LoggerFactory.getLogger(DataAccessEntityService.class);


  private static final Configuration conf = defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  @Inject
  protected DataAccessFormService dataAccessFormService;

  @Inject
  protected DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  protected SchemaFormContentFileService schemaFormContentFileService;

  @Inject
  protected EventBus eventBus;

  @Inject
  protected MailService mailService;

  @Inject
  protected MicaConfigService micaConfigService;

  private static final String EXCLUSION_IDS_YAML_RESOURCE_PATH = "/config/data-access-form/data-access-request-exclusion-ids-list.yml";

  abstract protected DataAccessEntityRepository<T> getRepository();

  abstract public T save(@NotNull T request);

  /**
   * Delete the {@link DataAccessEntity} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    T dataAccessRequest = findById(id);
    schemaFormContentFileService.deleteFiles(dataAccessRequest);
  }

  /**
   * Update the status of the {@link DataAccessRequest} matching the identifier.
   *
   * @param id
   * @param status
   * @throws NoSuchDataAccessRequestException
   */
  public T updateStatus(@NotNull String id, @NotNull DataAccessEntityStatus status)
    throws NoSuchDataAccessRequestException {
    T request = findById(id);
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
    T request = findById(id);
    if (request.getStatus() != DataAccessEntityStatus.OPENED)
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
  public T findById(@NotNull String id) throws NoSuchDataAccessRequestException {
    T request = getRepository().findOne(id);
    if (request == null) throw NoSuchDataAccessRequestException.withId(id);
    return request;
  }

  /**
   * Get all {@link DataAccessRequest}s, optionally filtered by applicant.
   *
   * @param applicant
   * @return
   */
  public List<T> findAll(@Nullable String applicant) {
    if (Strings.isNullOrEmpty(applicant)) return getRepository().findAll();
    return getRepository().findByApplicant(applicant);
  }

  public List<T> findByStatus(@Nullable List<String> status) {
    if (status == null || status.isEmpty()) return getRepository().findAll();
    List<DataAccessEntityStatus> statusList = status.stream().map(DataAccessEntityStatus::valueOf)
      .collect(Collectors.toList());

    return getRepository().findAll().stream().filter(dar -> statusList.contains(dar.getStatus()))
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
  protected void sendNotificationEmails(T request, @Nullable DataAccessEntityStatus from) {
    // check is new request
    if (from == null) return;

    // check no transition
    if (request.getStatus() == from) return;

    switch (request.getStatus()) {
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

  //
  // Private methods
  //

  protected Map<String, String> getNotificationEmailContext(T request) {
    Map<String, String> ctx = Maps.newHashMap();
    String organization = micaConfigService.getConfig().getName();
    String id = request.getId();
    String title = dataAccessRequestUtilService.getRequestTitle(request);

    ctx.put("organization", organization);
    ctx.put("publicUrl", micaConfigService.getPublicUrl());
    ctx.put("id", id);
    if (Strings.isNullOrEmpty(title)) title = id;
    ctx.put("title", title);
    ctx.put("applicant", request.getApplicant());
    ctx.put("status", request.getStatus().name());

    return ctx;
  }

  protected void sendSubmittedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifySubmitted()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getSubmittedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestSubmittedApplicantEmail", ctx,
        request.getApplicant());
      mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getSubmittedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestSubmittedDAOEmail", ctx,
        Roles.MICA_DAO);
    }
  }

  protected void sendConditionallyApprovedEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyConditionallyApproved()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getConditionallyApprovedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestConditionallyApprovedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendReviewedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyReviewed() && dataAccessForm.isWithReview()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getReviewedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestReviewedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendOpenedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyReopened()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getReopenedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestReopenedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendApprovedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyApproved()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getApprovedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestApprovedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendRejectedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyRejected()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getRejectedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestRejectedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendAttachmentsUpdatedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyAttachment()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getAttachmentSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestAttachmentsUpdated", ctx,
        request.getApplicant());

      mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getAttachmentSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestAttachmentsUpdated", ctx,
        Roles.MICA_DAO);
    }
  }

  protected void fillPdfTemplateFromRequest(byte[] template, OutputStream output, Object content)
    throws IOException, DocumentException {
    Map<String, Object> requestValues = PdfUtils.getFieldNames(template).stream()
      .map(k -> getMapEntryFromContent(content, k)) //
      .filter(e -> e != null && !e.getValue().isEmpty()) //
      .map(e -> Maps.immutableEntry(e.getKey(), e.getValue().get(0))) //
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    PdfUtils.fillOutForm(template, output, requestValues);
  }

  protected Map.Entry<String, List<Object>> getMapEntryFromContent(Object content, String jsonPath) {
    try {
      List<Object> values = JsonPath.using(conf).parse(content).read(jsonPath);
      return Maps.immutableEntry(jsonPath, values);
    } catch (PathNotFoundException ex) {
      //ignore
    } catch (InvalidPathException e) {
      log.warn("Invalid jsonpath {}", jsonPath);
    }

    return null;
  }

  protected void setAndLogStatus(T request, DataAccessEntityStatus to) {
    DataAccessEntityStatus from = request.getStatus();
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

  protected String generateId() {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();

    Object exclusions = YamlClassPathResourceReader.read(EXCLUSION_IDS_YAML_RESOURCE_PATH, Map.class).get("exclusions");

    IdentifierGenerator.Builder builder = IdentifierGenerator.newBuilder().prefix(dataAccessForm.getIdPrefix())
      .size(dataAccessForm.getIdLength()).zeros();

    if (exclusions instanceof List) {
      builder.exclusions((List) exclusions);
    }

    IdentifierGenerator idGenerator = builder.build();
    while (true) {
      String id = idGenerator.generateIdentifier();
      if (getRepository().findOne(id) == null) return id;
    }
  }

}
//  public DataAccessRequest save(@NotNull DataAccessRequest request) {
