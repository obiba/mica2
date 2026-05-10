package org.obiba.mica.web.controller;

import jakarta.inject.Inject;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.service.AgateServerConfigService;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Controller
public class AdminController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  protected AgateServerConfigService agateServerConfigService;

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

  private boolean hasPermissionOnAnyDraftDocument() {
    return networkService.findAllIds().stream()
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
      .anyMatch(id -> isPermitted("/draft/project", "VIEW", id));
  }

  @GetMapping("/admin")
  public ModelAndView admin() {
    Subject subject = SecurityUtils.getSubject();
    String contextPath = micaConfigService.getContextPath();
    if (!subject.isAuthenticated())
      return new ModelAndView("redirect:/signin?redirect=" + contextPath + "/admin");

    if (subject.hasRole(Roles.MICA_ADMIN)
      || subject.hasRole(Roles.MICA_DAO)
      || subject.hasRole(Roles.MICA_EDITOR)
      || subject.hasRole(Roles.MICA_REVIEWER)
      || hasPermissionOnAnyDraftDocument())
      return new ModelAndView("admin");

    return new ModelAndView("redirect:/");
  }

  @GetMapping("/administration")
  public ModelAndView administration() {
    Subject subject = SecurityUtils.getSubject();
    String contextPath = micaConfigService.getContextPath();
    if (!subject.isAuthenticated())
      return new ModelAndView("redirect:/signin?redirect=" + contextPath + "/administration");
    if (subject.hasRole(Roles.MICA_ADMIN)
      || subject.hasRole(Roles.MICA_DAO)
      || subject.hasRole(Roles.MICA_EDITOR)
      || subject.hasRole(Roles.MICA_REVIEWER)
      || hasPermissionOnAnyDraftDocument()) {
      String agateUrl = agateServerConfigService.getAgateUrl();
      ModelAndView mv = new ModelAndView("administration");
      mv.getModel().put("agateUrl", agateUrl);
      return mv;
    }

    return new ModelAndView("redirect:/");
  }

  @GetMapping("/admin2")
  public ModelAndView admin2() {
    Subject subject = SecurityUtils.getSubject();
    String contextPath = micaConfigService.getContextPath();
    if (!subject.isAuthenticated())
      return new ModelAndView("redirect:signin?redirect=" + contextPath + "/admin2");

    if (subject.hasRole(Roles.MICA_ADMIN)
      || subject.hasRole(Roles.MICA_DAO)
      || subject.hasRole(Roles.MICA_EDITOR)
      || subject.hasRole(Roles.MICA_REVIEWER)
      || hasPermissionOnAnyDraftDocument()) {
      ModelAndView mv = new ModelAndView("admin2");
      try {
        includeEntryPoints(mv);
      } catch (IOException e) {
        log.error("Error while reading SPA entry points", e);
      }
      return mv;
    }

    return new ModelAndView("redirect:/");
  }

  private void includeEntryPoints(ModelAndView mv) throws IOException {
    var resolver = new PathMatchingResourcePatternResolver();
    String folderPath = "classpath:/static/admin/assets/*";

    // Resolve all resources under the folder
    Resource[] resources = resolver.getResources(folderPath);

    for (Resource resource : resources) {
      String fileName = resource.getFilename();
      if (fileName != null && fileName.startsWith("index-")) {
        log.debug("Quasar entrypoint: {}", fileName);
        if (fileName.endsWith(".js")) {
          mv.getModel().put("entryPointJS", fileName);
        } else if (fileName.endsWith(".css")) {
          mv.getModel().put("entryPointCSS", fileName);
        }
      }
    }
  }

}
