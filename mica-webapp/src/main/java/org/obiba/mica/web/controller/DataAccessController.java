/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.access.notification.DataAccessRequestReportNotificationService;
import org.obiba.mica.access.service.*;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.micaConfig.domain.*;
import org.obiba.mica.micaConfig.service.*;
import org.obiba.mica.security.Roles;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class DataAccessController extends BaseController {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessPreliminaryService dataAccessPreliminaryService;

  @Inject
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  @Inject
  private DataAccessAgreementService dataAccessAgreementService;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  @Inject
  private DataAccessConfigService dataAccessConfigervice;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private DataAccessPreliminaryFormService dataAccessPreliminaryFormService;

  @Inject
  private DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  @Inject
  private DataAccessAmendmentFormService dataAccessAmendmentFormService;

  @Inject
  private DataAccessAgreementFormService dataAccessAgreementFormService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private CommentsService commentsService;

  @Inject
  private DataAccessRequestReportNotificationService dataAccessRequestReportNotificationService;

  private Pattern feasibilityIdPattern = Pattern.compile("-F\\d+$");

  private Pattern amendmentIdPattern = Pattern.compile("-A\\d+$");

  @GetMapping("/data-access/{id:.+}")
  public ModelAndView get(@PathVariable String id, @RequestParam(value = "invitation", required = false) String invitation) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      if (!Strings.isNullOrEmpty(invitation) && getConfig().isCollaboratorsEnabled()) {
        // apply invitation if necessary, this will grant read access
        dataAccessCollaboratorService.acceptCollaborator(getDataAccessRequest(id), invitation);
      }
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      DataAccessRequestTimeline timeline = dataAccessRequestReportNotificationService.getReportsTimeline(getDataAccessRequest(params));
      params.put("reportTimeline", timeline);

      List<DataAccessCollaborator> collaborators = dataAccessCollaboratorService.findByRequestId(id);
      params.put("collaborators", collaborators.stream()
        .map(collaborator -> new DataAccessCollaboratorBundle(collaborator, userProfileService.getProfileMap(collaborator.hasPrincipal() ? collaborator.getPrincipal() : collaborator.getEmail(), true)))
        .collect(Collectors.toList()));
      List<String> collaboratorEmails = collaborators.stream().map(DataAccessCollaborator::getEmail).collect(Collectors.toList());
      params.put("suggestedCollaborators", getConfig().isCollaboratorsEnabled() ? dataAccessRequestUtilService.getEmails(getDataAccessRequest(params)).stream()
        .filter(email -> !collaboratorEmails.contains(email))
        .collect(Collectors.toList()) : Lists.newArrayList());

      List<String> permissions = getPermissions(params);
      if (isArchivePermitted(getDataAccessRequest(params), timeline))
        permissions.add("ARCHIVE");
      else if (isUnArchivePermitted(getDataAccessRequest(params)))
        permissions.add("UNARCHIVE");

      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      return new ModelAndView("data-access", params);
    } else {
      String path = micaConfigService.getContextPath() + "/data-access/" + id;
      if (!Strings.isNullOrEmpty(invitation) && getConfig().isCollaboratorsEnabled()) {
        path = path + "?invitation=" + invitation;
      }
      try {
        path = URLEncoder.encode(path, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // ignore
      }
      return new ModelAndView("redirect:../signin?redirect=" + path);
    }
  }

  @GetMapping("/data-access-form/{id:.+}")
  public ModelAndView getForm(@PathVariable String id,
                              @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                              @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                              @RequestParam(value = "language", required = false) String language) {
    if (amendmentIdPattern.matcher(id).find()) {
      return new ModelAndView("redirect:/data-access-amendment-form/" + id);
    } else if (feasibilityIdPattern.matcher(id).find()) {
      return new ModelAndView("redirect:/data-access-feasibility-form/" + id);
    }
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      String lg = getLang(locale, language);
      addDataAccessFormConfiguration(params, getDataAccessRequest(params), !edit, lg);

      DataAccessPreliminary preliminary = getDataAccessPreliminary(params);
      if (preliminary != null && !DataAccessEntityStatus.APPROVED.equals(preliminary.getStatus())) {
        return new ModelAndView("redirect:/data-access-preliminary-form/" + id);
      }

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = getDataAccessRequest(params).getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(getDataAccessRequest(params).getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = getDataAccessRequest(params).getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-form%2F" + id);
    }
  }

  @GetMapping("/data-access-history/{id:.+}")
  public ModelAndView getHistory(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");
      params.put("permissions", permissions);

      params.put("statusChangeEvents", getFormStatusChangeEvents(params));

      return new ModelAndView("data-access-history", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-history%2F" + id);
    }
  }

  @GetMapping("/data-access-preliminary-form/{id:.+}")
  public ModelAndView getPreliminaryForm(@PathVariable String id,
                                         @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                         @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                         @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      if (!getConfig().isPreliminaryEnabled()) {
        return new ModelAndView("redirect:/data-access-form/" + id);
      }
      Map<String, Object> params = newParameters(id);
      String lg = getLang(locale, language);
      DataAccessPreliminary preliminary = getDataAccessPreliminary(params);
      addDataAccessPreliminaryFormConfiguration(params, preliminary, !edit, lg);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = preliminary.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(preliminary.getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = preliminary.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-preliminary-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-preliminary-form%2F" + id);
    }
  }

  @GetMapping("/data-access-feasibility-form/{id:.+}")
  public ModelAndView getFeasibilityForm(@PathVariable String id,
                                         @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                         @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                         @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newFeasibilityParameters(id);
      String lg = getLang(locale, language);
      DataAccessFeasibility feasibility = getDataAccessFeasibility(params);
      addDataAccessFeasibilityFormConfiguration(params, feasibility, !edit, lg);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = feasibility.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(feasibility.getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = feasibility.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-feasibility-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-feasibility-form%2F" + id);
    }
  }

  @GetMapping("/data-access-agreement-form/{id:.+}")
  public ModelAndView getAgreementForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newAgreementParameters(id);
      String lg = getLang(locale, language);
      DataAccessAgreement agreement = getDataAccessAgreement(params);
      addDataAccessAgreementFormConfiguration(params, agreement, !edit, lg);

      params.put("applicant", userProfileService.getProfileMap(agreement.getApplicant(), true));

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      return new ModelAndView("data-access-agreement-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-agreement-form%2F" + id);
    }
  }

  @GetMapping("/data-access-amendment-form/{id:.+}")
  public ModelAndView getAmendmentForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newAmendmentParameters(id);
      String lg = getLang(locale, language);
      DataAccessAmendment amendment = getDataAccessAmendment(params);
      addDataAccessAmendmentFormConfiguration(params, amendment, !edit, lg);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = amendment.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(amendment.getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = amendment.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-amendment-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-amendment-form%2F" + id);
    }
  }

  @GetMapping("/data-access-documents/{id:.+}")
  public ModelAndView getDocuments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      return new ModelAndView("data-access-documents", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-documents%2F" + id);
    }
  }

  @GetMapping("/data-access-comments/{id:.+}")
  public ModelAndView getComments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);

      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      addTimelineItems(commentsService.findPublicComments("/data-access-request", id), params);

      params.put("permissions", permissions);

      return new ModelAndView("data-access-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-comments%2F" + id);
    }
  }

  @GetMapping("/data-access-private-comments/{id:.+}")
  public ModelAndView getPrivateComments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);

      if (!isPermitted("/data-access-request/private-comment", "VIEW", null))
        checkPermission("/private-comment/data-access-request", "VIEW", null);
      else
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      addTimelineItems(commentsService.findPrivateComments("/data-access-request", id), params);

      return new ModelAndView("data-access-private-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-private-comments%2F" + id);
    }
  }

  @ExceptionHandler(NoSuchDataAccessRequestException.class)
  public ModelAndView notFoundError(HttpServletRequest request, NoSuchDataAccessRequestException ex) {
    return makeErrorModelAndView(request, "404", ex.getMessage());
  }

  //
  // Private methods
  //

  private void addTimelineItems(List<Comment> comments, Map<String, Object> params) {
    params.put("comments", comments);
    List<TimelineItem> items = Lists.newArrayList();
    items.addAll(comments.stream().map(TimelineItem::new).collect(Collectors.toList()));
    items.addAll(getFormStatusChangeEvents(params).stream().map(TimelineItem::new).collect(Collectors.toList()));

    items = items.stream().sorted(Comparator.comparing(TimelineItem::getDate)).collect(Collectors.toList());
    // last comment may be removable
    DataAccessRequest dar = getDataAccessRequest(params);
    if (!dar.isArchived()) {

      TimelineItem item = items.stream().filter(TimelineItem::isCommentItem).reduce((first, second) -> second).orElse(null);
      if (item != null) {
        Subject subject = SecurityUtils.getSubject();
        item.setCanRemove(subject.getPrincipal().toString().equals(item.getAuthor()) || subject.hasRole(Roles.MICA_DAO) || subject.hasRole(Roles.MICA_ADMIN));
      }
    }
    params.put("items", items);
    params.put("authors", items.stream().map(TimelineItem::getAuthor).distinct()
      .collect(Collectors.toMap(u -> u, u -> userProfileService.getProfileMap(u, true))));
  }

  private List<FormStatusChangeEvent> getFormStatusChangeEvents(Map<String, Object> params) {
    DataAccessRequest dar = getDataAccessRequest(params);

    // merge change history from main form, feasibility and amendment forms
    final List<FormStatusChangeEvent> events = Lists.newArrayList(
      dar.getStatusChangeHistory().stream()
        .map(e -> new FormStatusChangeEvent(userProfileService, dar, e)).collect(Collectors.toList()));
    getDataAccessFeasibilities(params).forEach(a -> {
      a.getStatusChangeHistory().stream().map(e -> new FormStatusChangeEvent(userProfileService, a, e))
        .forEach(events::add);
    });
    getDataAccessAmendments(params).forEach(a -> {
      a.getStatusChangeHistory().stream().map(e -> new FormStatusChangeEvent(userProfileService, a, e))
        .forEach(events::add);
    });
    getDataAccessAgreements(params).forEach(a -> {
      a.getStatusChangeHistory().stream().map(e -> new FormStatusChangeEvent(userProfileService, a, e))
        .forEach(events::add);
    });
    DataAccessPreliminary preliminary = getDataAccessPreliminary(params);
    if (preliminary != null) {
      preliminary.getStatusChangeHistory().stream().map(e -> new FormStatusChangeEvent(userProfileService, preliminary, e)).forEach(events::add);
    }

    // order change events
    return events.stream().sorted(Comparator.comparing(FormStatusChangeEvent::getDate)).collect(Collectors.toList());
  }

  private Map<String, Object> newParameters(String id) {
    checkPermission("/data-access-request", "VIEW", id);
    Map<String, Object> params = newParameters();
    DataAccessRequest dar = getDataAccessRequest(id);
    params.put("dar", dar);
    params.put("applicant", userProfileService.getProfileMap(dar.getApplicant(), true));
    params.put("mainApplicant", params.get("applicant"));

    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isPermitted(action, id))
      .collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    if (!dar.isArchived()) {
      if (subjectAclService.isCurrentUser(dar.getApplicant()) || subjectAclService.isPermitted("/data-access-request", "EDIT", id)) {
        if (getConfig().isCollaboratorsEnabled()) {
          permissions.add("ADD_COLLABORATORS"); // invite
          permissions.add("DELETE_COLLABORATORS");
        } else {
          // not enabled but still allow to manage a not empty list of collaborators
          permissions.add("DELETE_COLLABORATORS");
        }
      }
    }
    params.put("permissions", permissions);

    List<DataAccessFeasibility> feasibilities = dataAccessRequestUtilService.getDataAccessConfig().isFeasibilityEnabled() ? dataAccessFeasibilityService.findByParentId(id) : Lists.newArrayList();
    params.put("feasibilities", feasibilities);
    DataAccessFeasibility lastFeasibility = feasibilities.stream().max(Comparator.comparing(item -> item.getLastModifiedDate().orElse(null), Comparator.nullsLast(LocalDateTime::compareTo))).orElse(null);
    params.put("lastFeasibility", lastFeasibility);

    if (getConfig().isPreliminaryEnabled()) {
      DataAccessPreliminary preliminary = dataAccessPreliminaryService.getOrCreate(id);
      params.put("preliminary", preliminary);
      List<String> preliminaryPermissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
        .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isPreliminaryPermitted(action, preliminary.getParentId())).collect(Collectors.toList());
      if (!dar.isArchived() && isPermitted("/data-access-request/" + preliminary.getParentId() + "/preliminary/" + id, "EDIT", "_status"))
        preliminaryPermissions.add("EDIT_STATUS");
      params.put("preliminaryPermissions", preliminaryPermissions);
    }

    List<DataAccessAgreement> agreements = dataAccessRequestUtilService.getDataAccessConfig().isAgreementEnabled() && dar.getStatus().equals(DataAccessEntityStatus.APPROVED) ?
      dataAccessAgreementService.getOrCreate(dar) : Lists.newArrayList();
    params.put("agreements", agreements);
    params.put("agreementsOpened", agreements.stream().filter(agreement -> agreement.getStatus().equals(DataAccessEntityStatus.OPENED)).collect(Collectors.toList()));
    params.put("agreementsApproved", agreements.stream().filter(agreement -> agreement.getStatus().equals(DataAccessEntityStatus.APPROVED)).collect(Collectors.toList()));
    params.put("agreementsRejected", agreements.stream().filter(agreement -> agreement.getStatus().equals(DataAccessEntityStatus.REJECTED)).collect(Collectors.toList()));

    List<DataAccessAmendment> amendments = dataAccessRequestUtilService.getDataAccessConfig().isAmendmentsEnabled() ? dataAccessAmendmentService.findByParentId(id) : Lists.newArrayList();
    params.put("amendments", amendments);
    DataAccessAmendment lastAmendment = amendments.stream().max(Comparator.comparing(item -> item.getLastModifiedDate().orElse(null), Comparator.nullsLast(LocalDateTime::compareTo))).orElse(null);
    params.put("lastAmendment", lastAmendment);

    params.put("commentsCount", commentsService.countPublicComments("/data-access-request", id));
    params.put("privateCommentsCount", commentsService.countPrivateComments("/data-access-request", id));

    return params;
  }

  private Map<String, Object> newFeasibilityParameters(String id) {
    DataAccessFeasibility feasibility = dataAccessFeasibilityService.findById(id);
    Map<String, Object> params = newParameters(feasibility.getParentId());
    params.put("feasibility", feasibility);

    DataAccessRequest dar = getDataAccessRequest(params);
    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isFeasibilityPermitted(action, feasibility.getParentId(), id)).collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + feasibility.getParentId() + "/feasibility/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("feasibilityPermissions", permissions);

    return params;
  }

  private Map<String, Object> newAgreementParameters(String id) {
    DataAccessAgreement agreement = dataAccessAgreementService.findById(id);
    Map<String, Object> params = newParameters(agreement.getParentId());
    params.put("agreement", agreement);

    DataAccessRequest dar = getDataAccessRequest(params);
    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isAgreementPermitted(action, agreement.getParentId(), id)).collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + agreement.getParentId() + "/agreement/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("agreementPermissions", permissions);

    return params;
  }

  private Map<String, Object> newAmendmentParameters(String id) {
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    Map<String, Object> params = newParameters(amendment.getParentId());
    params.put("amendment", amendment);

    DataAccessRequest dar = getDataAccessRequest(params);
    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isAmendmentPermitted(action, amendment.getParentId(), id)).collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + amendment.getParentId() + "/amendment/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("amendmentPermissions", permissions);

    return params;
  }

  private boolean isPermitted(String action, String id) {
    return isPermitted("/data-access-request", action, id);
  }

  private boolean isArchivePermitted(DataAccessRequest dar, DataAccessRequestTimeline timeline) {
    return !dar.isArchived() && (SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) || SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN));
//    if (dar.isArchived()
//      || !DataAccessEntityStatus.APPROVED.equals(dar.getStatus())
//      || (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN))
//      || !timeline.hasEndDate()) return false;
//    return new Date().after(timeline.getEndDate());
  }

  private boolean isUnArchivePermitted(DataAccessRequest dar) {
    return dar.isArchived() && SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN);
  }

  private boolean isFeasibilityPermitted(String action, String id, String feasibilityId) {
    return isPermitted("/data-access-request/" + id + "/feasibility", action, feasibilityId);
  }

  private boolean isPreliminaryPermitted(String action, String id) {
    return isPermitted("/data-access-request/" + id + "/preliminary", action, id);
  }

  private boolean isAgreementPermitted(String action, String id, String agreementId) {
    return isPermitted("/data-access-request/" + id + "/agreement", action, agreementId);
  }

  private boolean isAmendmentPermitted(String action, String id, String amendmentId) {
    return isPermitted("/data-access-request/" + id + "/amendment", action, amendmentId);
  }

  private DataAccessRequest getDataAccessRequest(Map<String, Object> params) {
    return (DataAccessRequest) params.get("dar");
  }

  private List<String> getPermissions(Map<String, Object> params) {
    return (List<String>) params.get("permissions");
  }

  private DataAccessPreliminary getDataAccessPreliminary(Map<String, Object> params) {
    return (DataAccessPreliminary) params.get("preliminary");
  }

  private DataAccessFeasibility getDataAccessFeasibility(Map<String, Object> params) {
    return (DataAccessFeasibility) params.get("feasibility");
  }

  private List<DataAccessFeasibility> getDataAccessFeasibilities(Map<String, Object> params) {
    return (List<DataAccessFeasibility>) params.get("feasibilities");
  }

  private DataAccessAgreement getDataAccessAgreement(Map<String, Object> params) {
    return (DataAccessAgreement) params.get("agreement");
  }

  private List<DataAccessAgreement> getDataAccessAgreements(Map<String, Object> params) {
    return (List<DataAccessAgreement>) params.get("agreements");
  }

  private DataAccessAmendment getDataAccessAmendment(Map<String, Object> params) {
    return (DataAccessAmendment) params.get("amendment");
  }

  private List<DataAccessAmendment> getDataAccessAmendments(Map<String, Object> params) {
    return (List<DataAccessAmendment>) params.get("amendments");
  }

  private DataAccessRequest getDataAccessRequest(String id) {
    return dataAccessRequestService.findById(id);
  }

  private void addDataAccessConfiguration(Map<String, Object> params) {
    params.put("accessConfig", getConfig());
  }

  private void addDataAccessFormConfiguration(Map<String, Object> params, DataAccessRequest request, boolean readOnly, String locale) {
    DataAccessForm form = getDataAccessForm(request);
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), request.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfigBundle(getConfig(), form));
  }

  private void addDataAccessPreliminaryFormConfiguration(Map<String, Object> params, DataAccessPreliminary preliminary, boolean readOnly, String locale) {
    DataAccessPreliminaryForm form = dataAccessPreliminaryFormService.findByRevision("latest").get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), preliminary.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private void addDataAccessFeasibilityFormConfiguration(Map<String, Object> params, DataAccessFeasibility feasibility, boolean readOnly, String locale) {
    DataAccessFeasibilityForm form = dataAccessFeasibilityFormService.findByRevision("latest").get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), feasibility.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private void addDataAccessAmendmentFormConfiguration(Map<String, Object> params, DataAccessAmendment amendment, boolean readOnly, String locale) {
    DataAccessAmendmentForm dataAccessAmendmentForm = getDataAccessAmendmentForm(amendment);
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessAmendmentForm.getSchema(), dataAccessAmendmentForm.getDefinition(), amendment.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private void addDataAccessAgreementFormConfiguration(Map<String, Object> params, DataAccessAgreement agreement, boolean readOnly, String locale) {
    DataAccessAgreementForm form = dataAccessAgreementFormService.findByRevision("latest").get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), agreement.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private DataAccessForm getDataAccessForm(DataAccessRequest request) {
    Optional<DataAccessForm> form = dataAccessFormService.findByRevision(request.hasFormRevision() ? request.getFormRevision().toString() : "latest");
    return form.orElseGet(() -> dataAccessFormService.findByRevision("latest").get());
  }

  private DataAccessAmendmentForm getDataAccessAmendmentForm(DataAccessAmendment amendment) {
    Optional<DataAccessAmendmentForm> form = dataAccessAmendmentFormService.findByRevision(amendment.hasFormRevision() ? amendment.getFormRevision().toString() : "latest");
    return form.orElseGet(() -> dataAccessAmendmentFormService.findByRevision("latest").get());
  }

  private DataAccessFeasibilityForm getDataAccessFeasibilityForm(DataAccessFeasibility feasibility) {
    Optional<DataAccessFeasibilityForm> form = dataAccessFeasibilityFormService.findByRevision(feasibility.hasFormRevision() ? feasibility.getFormRevision().toString() : "latest");
    return form.orElseGet(() -> dataAccessFeasibilityFormService.findByRevision("latest").get());
  }

  private DataAccessConfig getConfig() {
    return dataAccessConfigervice.getOrCreateConfig();
  }

}
