package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.service.*;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.DataAccessRequestBundle;
import org.owasp.esapi.ESAPI;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
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

  @GetMapping("/data-accesses")
  public ModelAndView get(@RequestParam(value = "status", required = false) List<String> status) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters();
      addDataAccessConfiguration(params);
      List<DataAccessRequestBundle> dars = getDataAccessRequests(status);
      params.put("dars", dars);
      if (subjectAclService.isPermitted("/user", "VIEW"))
        params.put("users", userProfileService.getProfilesByGroup(Roles.MICA_USER).stream()
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
    return dataAccessRequestService.findByStatus(status).stream() //
      .filter(req -> isPermitted("/data-access-request", "VIEW", req.getId()))
      .map(req -> {
        DataAccessPreliminary preliminary = preliminaryEnabled ? dataAccessPreliminaryService.getOrCreate(req.getId()) : null;
        String title = dataAccessRequestUtilService.getRequestTitle(req);
        if (Strings.isNullOrEmpty(title) && preliminary != null) {
          title = dataAccessRequestUtilService.getRequestTitle(preliminary);
        }
        return new DataAccessRequestBundle(req, preliminary,
          ESAPI.encoder().encodeForHTML(title),
          dataAccessAmendmentService.countByParentId(req.getId()), dataAccessAmendmentService.countPendingByParentId(req.getId()),
          dataAccessFeasibilityService.countByParentId(req.getId()), dataAccessFeasibilityService.countPendingByParentId(req.getId()),
          dataAccessAgreementService.countByParentId(req.getId()),dataAccessAgreementService.countPendingByParentId(req.getId()));
      })
      .collect(Collectors.toList());
  }

  private void addDataAccessConfiguration(Map<String, Object> params) {
    params.put("accessConfig", dataAccessConfigService.getOrCreateConfig());
  }

}
