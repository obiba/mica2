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
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.domain.SubjectAcl.Type;
import org.obiba.mica.security.service.SubjectAclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

public abstract class DataAccessEntityService<T extends DataAccessEntity> {
  private static final Logger log = LoggerFactory.getLogger(DataAccessEntityService.class);


  private static final Configuration conf = defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  @Inject
  protected DataAccessConfigService dataAccessConfigService;

  @Inject
  protected DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  protected DataAccessCollaboratorService dataAccessCollaboratorService;

  @Inject
  protected SchemaFormContentFileService schemaFormContentFileService;

  @Inject
  protected EventBus eventBus;

  @Inject
  protected MailService mailService;

  @Inject
  protected MicaConfigService micaConfigService;

  @Inject
  protected VariableSetService variableSetService;

  @Inject
  protected SubjectAclService subjectAclService;

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
    Optional<T> request = getRepository().findById(id);
    if (!request.isPresent()) throw NoSuchDataAccessRequestException.withId(id);
    return request.get();
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
  // Protected methods
  //

  /**
   * Send a notification email, depending on the status of the request.
   *
   * @param request
   * @param from
   */
  protected void sendNotificationEmails(final T request, final @Nullable DataAccessEntityStatus from) {
    // no notification when administrator operates, make sure to use a DAO account so that applicant get informed
    if (SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN) && !SecurityUtils.getSubject().hasRole(Roles.MICA_DAO))
      return;

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

  protected void sendCreatedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyCreated()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);
      if (ctx.get("parentId") == null) { // only original request, not amendments
        mailService.sendEmailToGroups(mailService.getSubject(dataAccessConfig.getCreatedSubject(), ctx,
            DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestCreatedDAOEmail", ctx,
          Roles.MICA_DAO);

          sendNotificationToReaders(dataAccessConfig, request, ctx, "dataAccessRequestCreatedDAOEmail");
      }
    }
  }

  protected void sendSubmittedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifySubmitted()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getSubmittedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "SubmittedApplicantEmail", ctx,
        getApplicantAndCollaborators(request));
      mailService.sendEmailToGroups(mailService.getSubject(dataAccessConfig.getSubmittedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "SubmittedDAOEmail", ctx,
        Roles.MICA_DAO);

      sendNotificationToReaders(dataAccessConfig, request, ctx, "SubmittedDAOEmail");
    }
  }

  protected void sendConditionallyApprovedEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyConditionallyApproved()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getConditionallyApprovedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ConditionallyApprovedApplicantEmail", ctx,
        getApplicantAndCollaborators(request));
    }
  }

  protected void sendReviewedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyReviewed() && dataAccessConfig.isWithReview()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getReviewedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ReviewedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendOpenedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyReopened()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getReopenedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ReopenedApplicantEmail", ctx,
        request.getApplicant());
    }
  }

  protected void sendApprovedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyApproved()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getApprovedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "ApprovedApplicantEmail", ctx,
        getApplicantAndCollaborators(request));
    }
  }

  protected void sendRejectedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyRejected()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      String prefix = getTemplatePrefix(ctx);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getRejectedSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), prefix + "RejectedApplicantEmail", ctx,
        getApplicantAndCollaborators(request));
    }
  }

  protected void sendAttachmentsUpdatedNotificationEmail(T request) {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (dataAccessConfig.isNotifyAttachment()) {
      Map<String, String> ctx = dataAccessRequestUtilService.getNotificationEmailContext(request);

      mailService.sendEmailToUsers(mailService.getSubject(dataAccessConfig.getAttachmentSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestAttachmentsUpdated", ctx,
        request.getApplicant());

      mailService.sendEmailToGroups(mailService.getSubject(dataAccessConfig.getAttachmentSubject(), ctx,
          DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestAttachmentsUpdated", ctx,
        Roles.MICA_DAO);

      sendNotificationToReaders(dataAccessConfig, request, ctx, "dataAccessRequestAttachmentsUpdated");
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
    request.getStatusChangeHistory().add(
      StatusChange.newBuilder()
        .previous(from)
        .current(to)
        .content(request.getContent())
        .author(SecurityUtils.getSubject().getPrincipal().toString())
        .now().build()
    );
  }

  protected String generateId() {
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();

    Object exclusions = YamlClassPathResourceReader.read(EXCLUSION_IDS_YAML_RESOURCE_PATH, Map.class).get("exclusions");

    IdentifierGenerator.Builder builder = IdentifierGenerator.newBuilder().prefix(dataAccessConfig.getIdPrefix())
      .size(dataAccessConfig.getIdLength());

    if (dataAccessConfig.isAllowIdWithLeadingZeros()) {
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
      if (!getRepository().existsById(id) ) return id;
    }

    throw new DataAccessRequestGenerationException("Exceeded 100 id generation tries");
  }

  protected String getMainRequestId(T request) {
    return request.getId();
  }

  protected String getTemplatePrefix(Map<String, String> ctx) {
    if (isDataAccessAmendmentContext(ctx))
      return "dataAccessAmendment";
    if (isDataAccessFeasibilityContext(ctx))
      return "dataAccessFeasibility";
    if (isDataAccessAgreementContext(ctx))
      return "dataAccessAgreement";
    if (isDataAccessPreliminaryContext(ctx))
      return "dataAccessPreliminary";
    return "dataAccessRequest";
  }

  //
  // Private methods
  //

  /**
   * Get the users name of the applicant and of the accepted collaborators.
   *
   * @param request
   * @return
   */
  public String[] getApplicantAndCollaborators(T request) {
    List<DataAccessCollaborator> collaborators = dataAccessCollaboratorService.findByRequestId(getMainRequestId(request)).stream()
      .filter(DataAccessCollaborator::hasPrincipal)
      .collect(Collectors.toList());
    String[] usernames = new String[collaborators.size() + 1];
    usernames[0] = request.getApplicant();
    for (int i = 0; i < collaborators.size(); i++) {
      usernames[i + 1] = collaborators.get(i).getPrincipal();
    }
    return usernames;
  }

  /**
   * Sort by last modified first.
   *
   * @param list
   * @return
   */
  private List<T> applyDefaultSort(List<T> list) {
    list.sort(Comparator.comparing(item -> ((AbstractAuditableDocument) item).getLastModifiedDate().orElse(null), Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
    return list;
  }

  private boolean isDataAccessAmendmentContext(Map<String, String> ctx) {
    return ctx.containsKey("type") && ctx.get("type").equals(DataAccessAmendment.class.getSimpleName());
  }

  private boolean isDataAccessFeasibilityContext(Map<String, String> ctx) {
    return ctx.containsKey("type") && ctx.get("type").equals(DataAccessFeasibility.class.getSimpleName());
  }

  private boolean isDataAccessAgreementContext(Map<String, String> ctx) {
    return ctx.containsKey("type") && ctx.get("type").equals(DataAccessAgreement.class.getSimpleName());
  }

  private boolean isDataAccessPreliminaryContext(Map<String, String> ctx) {
    return ctx.containsKey("type") && ctx.get("type").equals(DataAccessPreliminary.class.getSimpleName());
  }

  /**
   * @return principals with READER permission (VIEW action) excluding applicant, collaborators, MICA_ADMIN and MICA_DAO
   */
  private Map<String, List<String>> getRequestReaders(Map<String, String> ctx, String[] applicantsAndCollaborators) {
    Map<String, List<String>> map = new HashMap<>();
    map.put("users", new ArrayList<>());
    map.put("groups", new ArrayList<>());

    List<String> excludedPrincipals = new ArrayList<>();
    excludedPrincipals.addAll(Arrays.asList(applicantsAndCollaborators));
    excludedPrincipals.add(Roles.MICA_ADMIN);
    excludedPrincipals.add(Roles.MICA_DAO);

    String darId = getTemplatePrefix(ctx).equals("dataAccessRequest") ? ctx.get("id") : ctx.get("parentId");

    List<SubjectAcl> foundAcls = subjectAclService.findByResourceInstance("/data-access-request", darId);
    foundAcls.stream().filter(acl -> acl.hasAction("VIEW")).filter(acl -> !excludedPrincipals.contains(acl.getPrincipal()))
      .forEach(acl -> map.get(acl.getType().equals(Type.GROUP) ? "groups" : "users").add(acl.getPrincipal()));

    return map;
  }

  private void sendNotificationToReaders(DataAccessConfig dataAccessConfig, T request, Map<String, String> ctx, String template) {
    Map<String, List<String>> requestReaders = getRequestReaders(ctx, getApplicantAndCollaborators(request));
    List<String> readerUsers = requestReaders.get("users");
    List<String> readerGroups = requestReaders.get("groups");

    String subject = mailService.getSubject(dataAccessConfig.getCreatedSubject(), ctx, DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT);

    if (readerUsers.size() > 0 && readerGroups.size() > 0) {
      mailService.sendEmailToGroupsAndUsers(subject, template, ctx, readerGroups, readerUsers);
    } else if(readerUsers.size() > 0) {
      mailService.sendEmailToUsers(subject, template, ctx, readerUsers.stream().toArray(String[]::new));
    } else if(readerGroups.size() > 0) {
      mailService.sendEmailToGroups(subject, template, ctx, readerGroups.stream().toArray(String[]::new));
    }
  }
}
