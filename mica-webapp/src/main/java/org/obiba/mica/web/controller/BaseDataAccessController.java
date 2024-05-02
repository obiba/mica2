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

import com.google.common.collect.Lists;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.access.service.*;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.micaConfig.domain.AgreementOpenedPolicy;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.FormStatusChangeEvent;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseDataAccessController extends BaseController {

  @Inject
  private UserProfileService userProfileService;

  @Inject
  protected CommentsService commentsService;

  @Inject
  private DataAccessConfigService dataAccessConfigervice;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessPreliminaryService dataAccessPreliminaryService;

  @Inject
  protected DataAccessFeasibilityService dataAccessFeasibilityService;

  @Inject
  protected DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  protected DataAccessAgreementService dataAccessAgreementService;

  @Inject
  protected DataAccessRequestUtilService dataAccessRequestUtilService;

  protected DataAccessRequest getDataAccessRequest(Map<String, Object> params) {
    return (DataAccessRequest) params.get("dar");
  }

  protected Map<String, Object> newParameters(String id) {
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

    List<DataAccessAgreement> agreements = Lists.newArrayList();

    if (getConfig().isAgreementEnabled()) {
      if (AgreementOpenedPolicy.ALWAYS.equals(getConfig().getAgreementOpenedPolicy())
        || dar.getStatus().equals(DataAccessEntityStatus.APPROVED)
        || (AgreementOpenedPolicy.PRELIMINARY_APPROVED.equals(getConfig().getAgreementOpenedPolicy()) && getConfig().isPreliminaryEnabled() && getDataAccessPreliminary(params).getStatus().equals(DataAccessEntityStatus.APPROVED))) {
        agreements = dataAccessAgreementService.getOrCreate(dar);
      }
    }
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

  protected DataAccessRequest getDataAccessRequest(String id) {
    return dataAccessRequestService.findById(id);
  }

  protected DataAccessConfig getConfig() {
    return dataAccessConfigervice.getOrCreateConfig();
  }

  protected Map<String, Object> getUserProfileMap(String username) {
    return userProfileService.getProfileMap(username, true);
  }

  protected List<FormStatusChangeEvent> getFormStatusChangeEvents(Map<String, Object> params) {
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

  protected List<String> getPermissions(Map<String, Object> params) {
    return (List<String>) params.get("permissions");
  }

  @ExceptionHandler(NoSuchDataAccessRequestException.class)
  public ModelAndView notFoundError(HttpServletRequest request, NoSuchDataAccessRequestException ex) {
    return makeErrorModelAndView(request, "404", ex.getMessage());
  }

  //
  // Private methods
  //

  private boolean isPermitted(String action, String id) {
    return isPermitted("/data-access-request", action, id);
  }

  private boolean isPreliminaryPermitted(String action, String id) {
    return isPermitted("/data-access-request/" + id + "/preliminary", action, id);
  }

  protected DataAccessPreliminary getDataAccessPreliminary(Map<String, Object> params) {
    return (DataAccessPreliminary) params.get("preliminary");
  }

  private List<DataAccessFeasibility> getDataAccessFeasibilities(Map<String, Object> params) {
    return (List<DataAccessFeasibility>) params.get("feasibilities");
  }

  private List<DataAccessAgreement> getDataAccessAgreements(Map<String, Object> params) {
    return (List<DataAccessAgreement>) params.get("agreements");
  }

  private List<DataAccessAmendment> getDataAccessAmendments(Map<String, Object> params) {
    return (List<DataAccessAmendment>) params.get("amendments");
  }
}
