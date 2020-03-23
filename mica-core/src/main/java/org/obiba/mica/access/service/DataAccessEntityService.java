package org.obiba.mica.access.service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.itextpdf.text.DocumentException;
import com.jayway.jsonpath.*;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.PdfUtils;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.DataAccessRequestGenerationException;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
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

  private static final String EXCLUSION_IDS_YAML_RESOURCE_PATH = "config/data-access-form/data-access-request-exclusion-ids-list.yml";

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
    if (Strings.isNullOrEmpty(applicant)) return applyDefaultSort(getRepository().findAll());
    return applyDefaultSort(getRepository().findByApplicant(applicant));
  }

  /**
   * Get all {@link DataAccessRequest}s, optionally filtered by status.
   *
   * @param status
   * @return
   */
  public List<T> findByStatus(@Nullable List<String> status) {
    List<T> list = getRepository().findAll();
    if (status != null && !status.isEmpty()) {
      list = list.stream().filter(dar -> status.contains(dar.getStatus().name())).collect(Collectors.toList());
    }
    return applyDefaultSort(list);
  }

  //
  // Private methods
  //

  /**
   * Sort by last modified first.
   *
   * @param list
   * @return
   */
  private List<T> applyDefaultSort(List<T> list) {
    list.sort(Comparator.comparing(AbstractAuditableDocument::getLastModifiedDate).reversed());
    return list;
  }

  /**
   * Send a notification email, depending on the status of the request.
   *
   * @param request
   * @param from
   */
  protected void sendNotificationEmails(final T request, final @Nullable DataAccessEntityStatus from) {
    Executors.newCachedThreadPool().execute(() -> {
      try {
        if (from == null) { // check is new request
          sendCreatedNotificationEmail(request);
        } else if (request.getStatus() != from) { // check there is a transition
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
      } catch (Exception e) {
        log.error("Failed at sending data access notification email", e);
      }
    });
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
    ctx.put("type", request.getClass().getSimpleName());
    if (request instanceof DataAccessAmendment)
      ctx.put("parentId", ((DataAccessAmendment) request).getParentId());
    if (request instanceof DataAccessFeasibility)
      ctx.put("parentId", ((DataAccessFeasibility) request).getParentId());
    if (Strings.isNullOrEmpty(title)) title = id;
    ctx.put("title", title);
    ctx.put("applicant", request.getApplicant());
    ctx.put("status", request.getStatus().name());

    return ctx;
  }

  protected void sendCreatedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyCreated()) {
      Map<String, String> ctx = getNotificationEmailContext(request);
      if (ctx.get("parentId") == null) { // only original request, not amendments
        mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getCreatedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestCreatedDAOEmail", ctx,
          Roles.MICA_DAO);
      }
    }
  }

  protected void sendSubmittedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifySubmitted()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getSubmittedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "SubmittedApplicantEmail", ctx,
        request.getApplicant());
      mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getSubmittedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "SubmittedDAOEmail", ctx,
        Roles.MICA_DAO);
    }
  }

  protected void sendConditionallyApprovedEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyConditionallyApproved()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getConditionallyApprovedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ConditionallyApprovedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendReviewedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyReviewed() && dataAccessForm.isWithReview()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getReviewedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ReviewedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendOpenedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyReopened()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getReopenedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ReopenedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendApprovedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyApproved()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getApprovedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ApprovedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendRejectedNotificationEmail(T request) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isNotifyRejected()) {
      Map<String, String> ctx = getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getRejectedSubject(), ctx,
        DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "RejectedApplicantEmail", ctx,
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
      .size(dataAccessForm.getIdLength());

    if (dataAccessForm.isAllowIdWithLeadingZeros()) {
      builder.zeros();
    }

    if (exclusions instanceof List) {
      log.info("Using exclusions {} to generate DAR id", exclusions.toString());
      builder.exclusions((List) exclusions);
    }

    IdentifierGenerator idGenerator = builder.build();
    int tries = 0;
    while (tries < 100) {
      tries++;
      String id = idGenerator.generateIdentifier();
      if (getRepository().findOne(id) == null) return id;
    }

    throw new DataAccessRequestGenerationException("Exceeded 100 id generation tries");
  }

  private String getTemplatePrefix(Map<String, String> ctx) {
    if (isDataAccessAmendmentContext(ctx))
      return "dataAccessAmendment";
    if (isDataAccessFeasibilityContext(ctx))
      return "dataAccessFeasibility";
    return "dataAccessRequest";
  }

  private boolean isDataAccessAmendmentContext(Map<String, String> ctx) {
    return ctx.containsKey("type") && ctx.get("type").equals(DataAccessAmendment.class.getSimpleName());
  }

  private boolean isDataAccessFeasibilityContext(Map<String, String> ctx) {
    return ctx.containsKey("type") && ctx.get("type").equals(DataAccessFeasibility.class.getSimpleName());
  }
}
