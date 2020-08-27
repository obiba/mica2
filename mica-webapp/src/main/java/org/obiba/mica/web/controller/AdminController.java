package org.obiba.mica.web.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;

@Controller
public class AdminController extends BaseController {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private NetworkService networkService;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private ProjectService projectService;

  @GetMapping("/admin")
  public ModelAndView admin() {
    Subject subject = SecurityUtils.getSubject();
    String contextPath = micaConfigService.getContextPath();
    if (!subject.isAuthenticated())
      return new ModelAndView("redirect:signin?redirect=" + contextPath + "/admin");

    if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO) || subject.hasRole(Roles.MICA_EDITOR) || subject.hasRole(Roles.MICA_REVIEWER))
      return new ModelAndView("admin");

    // Check if the user has permission on any draft document
    if (networkService.findAllIds().stream()
      .anyMatch(id -> isPermitted("/draft/network", "VIEW", id))
      || individualStudyService.findAllIds().stream()
      .anyMatch(id -> isPermitted("/draft/individual-study", "VIEW", id))
      || harmonizationStudyService.findAllIds().stream()
      .anyMatch(id -> isPermitted("/draft/harmonization-study", "VIEW", id))
      || collectedDatasetService.findAllIds().stream()
      .anyMatch(id -> isPermitted("/draft/collected-dataset", "VIEW", id))
      || harmonizedDatasetService.findAllIds().stream()
      .anyMatch(id -> isPermitted("/draft/harmonized-dataset", "VIEW", id))
      || projectService.findAllIds().stream()
      .anyMatch(id -> isPermitted("/draft/project", "VIEW", id)))
      return new ModelAndView("admin");

    return new ModelAndView("redirect:/");
  }

}
