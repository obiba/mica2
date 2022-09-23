package org.obiba.mica.access.service;


import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.SecurityUtils;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.access.DataAccessCollaboratorRepository;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessCollaboratorDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ForbiddenException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Validated
public class DataAccessCollaboratorService {

  private static final Logger log = LoggerFactory.getLogger(DataAccessCollaboratorService.class);

  private static final String AUTHOR_KEY = "author";

  private static final String REQUEST_KEY = "request";

  private static final String EMAIL_KEY = "email";

  private static final String CREATED_KEY = "created";

  @Inject
  private DataAccessCollaboratorRepository dataAccessCollaboratorRepository;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private DataAccessConfigService dataAccessConfigService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private MailService mailService;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private EventBus eventBus;

  public List<DataAccessCollaborator> findByRequestId(@NotNull String requestId) {
    return dataAccessCollaboratorRepository.findByRequestId(requestId);
  }

  public Optional<DataAccessCollaborator> findByRequestIdAndEmail(@NotNull String requestId, String email) {
    return dataAccessCollaboratorRepository.findByRequestIdAndEmail(requestId, email);
  }

  /**
   * Send an invitation and returns an invitation key.
   *
   * @param dar
   * @param email
   * @return
   */
  public void inviteCollaborator(@NotNull DataAccessRequest dar, String email) {
    if (!dataAccessRequestUtilService.getDataAccessConfig().isCollaboratorsEnabled()) throw new ForbiddenException("Inviting collaborators is not enabled");
    Optional<DataAccessCollaborator> collaboratorOpt = dataAccessCollaboratorRepository.findByRequestIdAndEmail(dar.getId(), email);
    save(collaboratorOpt.orElseGet(() -> DataAccessCollaborator.newBuilder(dar.getId()).email(email).invited().author(SecurityUtils.getSubject().getPrincipal().toString()).build()));
    sendCollaboratorInvitation(dar, email, makeInvitationKey(dar, email));
  }

  /**
   * Verify the invitation key applies to current user and provided request ID
   * before accepting the collaborator.
   *
   * @param dar
   * @param invitation
   */
  public void acceptCollaborator(@NotNull DataAccessRequest dar, String invitation) {
    try {
      JSONObject jsonKey = new JSONObject(micaConfigService.decrypt(invitation));
      String darId = jsonKey.getString(REQUEST_KEY);
      if (!dar.getId().equals(darId))
        throw new IllegalArgumentException("Invitation does not apply to this data access request");
      String principal = SecurityUtils.getSubject().getPrincipal().toString();
      String email = jsonKey.getString(EMAIL_KEY);
      boolean found = false;
      ObibaRealm.Subject subject = userProfileService.getProfile(principal, true);
      if (subject.getAttributes() != null) {
        for (Map<String, String> map : subject.getAttributes()) {
          String key = map.get("key");
          String value = map.get("value");
          if ("email".equals(key) && email.equals(value)) {
            found = true;
            break;
          }
        }
      }
      if (!found) {
        throw new IllegalArgumentException("Invitation does not apply to current user");
      }
      String author = jsonKey.getString(AUTHOR_KEY);
      // check and register the collaborator
      Optional<DataAccessCollaborator> collaboratorOpt = dataAccessCollaboratorRepository.findByRequestIdAndEmail(darId, email);
      if (collaboratorOpt.isPresent() && collaboratorOpt.get().isBanned())
        throw new ForbiddenException("Invitation does not apply to a banned collaborator");
      DataAccessCollaborator collaborator = collaboratorOpt.orElseGet(() -> DataAccessCollaborator.newBuilder(dar.getId()).email(email).author(author).build());
      collaborator.setInvitationPending(false);
      collaborator.setPrincipal(principal);
      save(collaborator);
      // verify and set permissions
      if (!subjectAclService.isPermitted("/data-access-request", "VIEW", darId)) {
        subjectAclService.addUserPermission(principal, "/data-access-request", "VIEW", darId);
      }
      sendCollaboratorAcceptedNotification(dar, email);
    } catch (JSONException e) {
      log.warn("Invitation key is not valid");
      throw new IllegalArgumentException("Invitation format is not valid");
    }
  }

  public DataAccessCollaborator save(DataAccessCollaborator collaborator) {
    collaborator.setLastModifiedBy(SecurityUtils.getSubject().getPrincipal().toString());
    collaborator.setLastModifiedDate(DateTime.now());
    return dataAccessCollaboratorRepository.save(collaborator);
  }

  public void delete(DataAccessCollaborator collaborator) {
    dataAccessCollaboratorRepository.delete(collaborator);
    eventBus.post(new DataAccessCollaboratorDeletedEvent(collaborator));
  }

  //
  // Events handling
  //

  @Async
  @Subscribe
  public void dataAccessRequestDeleted(DataAccessRequestDeletedEvent event) {
    List<DataAccessCollaborator> collaborators = findByRequestId(event.getPersistable().getId());
    if (!collaborators.isEmpty())
      dataAccessCollaboratorRepository.delete(collaborators);
    // note: permissions are removed by the SubjectAclService
  }

  //
  // Private methods
  //

  /**
   * Send an invitation to a collaborator's email, providing a personal key.
   *
   * @param dar
   * @param email
   * @param key
   */
  private void sendCollaboratorInvitation(DataAccessRequest dar, String email, String key) {
    Map<String, String> context = dataAccessRequestUtilService.getNotificationEmailContext(dar);
    context.put("key", key);
    mailService.sendEmailToUsers(
      mailService.getSubject(dataAccessConfigService.getOrCreateConfig().getCollaboratorInvitationSubject(),
        context, DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT),
      "dataAccessRequestCollaboratorInvitation", context, email);
  }

  /**
   * Inform applicant that the collaborator invitation was accepted.
   *
   * @param dar
   * @param email
   */
  private void sendCollaboratorAcceptedNotification(DataAccessRequest dar, String email) {
    if (!dataAccessConfigService.getOrCreateConfig().isNotifyCollaboratorAccepted()) return;

    Map<String, String> context = dataAccessRequestUtilService.getNotificationEmailContext(dar);
    context.put("email", email);
    mailService.sendEmailToUsers(
      mailService.getSubject(dataAccessConfigService.getOrCreateConfig().getCollaboratorAcceptedSubject(),
        context, DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT),
      "dataAccessRequestCollaboratorAccepted", context, dar.getApplicant());
  }

  private String makeInvitationKey(@NotNull DataAccessRequest dar, String email) {
    JSONObject jsonKey = new JSONObject();
    try {
      jsonKey.put(AUTHOR_KEY, SecurityUtils.getSubject().getPrincipal().toString());
      jsonKey.put(REQUEST_KEY, dar.getId());
      jsonKey.put(EMAIL_KEY, email);
      jsonKey.put(CREATED_KEY, DateTime.now());
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
    return micaConfigService.encrypt(jsonKey.toString());
  }
}
