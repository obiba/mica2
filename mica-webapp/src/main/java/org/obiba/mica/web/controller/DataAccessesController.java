package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.*;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.MicaGroupsToRolesMapper;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;
import org.obiba.mica.web.controller.domain.DataAccessRequestBundle;
import org.owasp.esapi.ESAPI;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DataAccessesController extends BaseController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessAgreementService dataAccessAgreementService;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  @Inject
  private DataAccessPreliminaryService dataAccessPreliminaryService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  DataAccessConfigService dataAccessConfigService;

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private MicaGroupsToRolesMapper groupsToRolesMapper;

  @GetMapping("/data-accesses")
  public ModelAndView get(@RequestParam(value = "status", required = false) List<String> status) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters();
      addDataAccessConfiguration(params);
      List<DataAccessRequestBundle> dars = getDataAccessRequests(status);
      params.put("dars", dars);
      params.put("canAddDar", subjectAclService.isPermitted("/data-access-request", "ADD"));
      if (subjectAclService.isPermitted("/user", "VIEW"))
        params.put("users", groupsToRolesMapper.toGroups(Roles.MICA_USER).stream()
          .flatMap(group -> userProfileService.getProfilesByGroup(group).stream())
          .collect(Collectors.toMap(ObibaRealm.Subject::getUsername, s -> s, (s1, s2) -> s1, LinkedHashMap::new))
          .values().stream()
          .map(s -> userProfileService.asMap(s)).collect(Collectors.toList()));
      params.put("applicants", dars.stream().map(DataAccessRequestBundle::getApplicant).distinct()
        .collect(Collectors.toMap(u -> u, u -> userProfileService.getProfileMap(u, true))));
      return new ModelAndView("data-accesses", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/data-accesses");
    }
  }

  private List<DataAccessRequestBundle> getDataAccessRequests(List<String> status) {
    boolean preliminaryEnabled = dataAccessConfigService.getOrCreateConfig().isPreliminaryEnabled();
    List<DataAccessRequest> requests = dataAccessRequestService.findByStatus(status).stream() //
      .filter(req -> isPermitted("/data-access-request", "VIEW", req.getId()))
      .collect(Collectors.toList());
    if (requests.isEmpty()) return new ArrayList<>();
    // fetch child entities counts and forms once for all requests, instead of querying them for each request
    List<String> ids = requests.stream().map(DataAccessRequest::getId).collect(Collectors.toList());
    Map<String, DataAccessEntityCounts> amendmentCounts = dataAccessAmendmentService.countByParentIds(ids);
    Map<String, DataAccessEntityCounts> feasibilityCounts = dataAccessFeasibilityService.countByParentIds(ids);
    Map<String, DataAccessEntityCounts> agreementCounts = dataAccessAgreementService.countByParentIds(ids);
    Map<String, DataAccessPreliminary> preliminaries = preliminaryEnabled ? dataAccessPreliminaryService.getOrCreate(ids) : Map.of();
    Map<String, AbstractDataAccessEntityForm> forms = new HashMap<>();
    return requests.stream()
      .map(req -> {
        DataAccessPreliminary preliminary = preliminaries.get(req.getId());
        String title = getRequestTitle(req, forms);
        if (Strings.isNullOrEmpty(title) && preliminary != null) {
          title = getRequestTitle(preliminary, forms);
        }
        DataAccessEntityCounts amendments = amendmentCounts.getOrDefault(req.getId(), DataAccessEntityCounts.NONE);
        DataAccessEntityCounts feasibilities = feasibilityCounts.getOrDefault(req.getId(), DataAccessEntityCounts.NONE);
        DataAccessEntityCounts agreements = agreementCounts.getOrDefault(req.getId(), DataAccessEntityCounts.NONE);
        return new DataAccessRequestBundle(req, preliminary,
          ESAPI.encoder().encodeForHTML(title),
          amendments.total(), amendments.pending(),
          feasibilities.total(), feasibilities.pending(),
          agreements.total(), agreements.pending());
      })
      .collect(Collectors.toList());
  }

  /**
   * Get the request title, looking up its form only once per entity type and form revision.
   */
  private String getRequestTitle(DataAccessEntity entity, Map<String, AbstractDataAccessEntityForm> forms) {
    String key = entity.getClass().getSimpleName() + ":" + (entity.hasFormRevision() ? entity.getFormRevision() : "latest");
    AbstractDataAccessEntityForm form = forms.computeIfAbsent(key, k -> dataAccessRequestUtilService.getDataAccessForm(entity));
    return dataAccessRequestUtilService.getRequestTitle(entity, form);
  }

  private void addDataAccessConfiguration(Map<String, Object> params) {
    params.put("accessConfig", dataAccessConfigService.getOrCreateConfig());
  }

}
