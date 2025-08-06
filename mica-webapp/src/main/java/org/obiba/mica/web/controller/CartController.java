package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import java.util.Map;
import java.util.Optional;

@Controller
public class CartController extends BaseController {

  @Inject
  private VariableSetService variableSetService;

  @Inject
  private DataAccessConfigService dataAccessConfigService;

  @Inject
  private Dtos dtos;

  @GetMapping("/cart")
  public ModelAndView get(@RequestParam(required = false) String type,
                          @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "${locale.validatedLocale:en}") String locale) {
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled()) {
      return new ModelAndView("redirect:/");
    }
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated() || config.isAnonymousCanCreateCart()) {
      // note: the cart will be populated by the SessionInterceptor
      Map<String, Object> params = newParameters();
      params.put("accessConfig", dataAccessConfigService.getOrCreateConfig());
      addShowCartTypeParameter(params, type, config);
      params.put("configJson", getMicaConfigAsJson(config, getLang(locale, null)));
      return new ModelAndView("cart", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/cart");
    }
  }

  private void addShowCartTypeParameter(Map<String, Object> params, String type, MicaConfig config) {
    boolean variableEnabledInConfig = config.isCartEnabled() && (config.isStudyDatasetEnabled() || config.isHarmonizationDatasetEnabled());
    boolean studyEnabledInConfig = config.isStudiesCartEnabled() && !config.isSingleStudyEnabled();
    boolean networkEnabledInConfig = config.isNetworksCartEnabled() && !config.isSingleNetworkEnabled();

    if (!Strings.isNullOrEmpty(type) &&
      ((type.equalsIgnoreCase("variables") && variableEnabledInConfig) ||
        (type.equalsIgnoreCase("studies") && studyEnabledInConfig) ||
        (type.equalsIgnoreCase("networks") && networkEnabledInConfig))) {
      params.put("showCartType", type.toLowerCase());
    } else {
      params.put("showCartType", variableEnabledInConfig ? "variables" : (studyEnabledInConfig ? "studies" : "networks"));
    }
  }

  @GetMapping("/lists")
  public ModelAndView lists() {
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled()) {
      return new ModelAndView("redirect:/");
    }
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Optional<DocumentSet> tentative = variableSetService.getAllCurrentUser().stream().filter(DocumentSet::hasName).findFirst();
      return tentative.map(documentSet -> new ModelAndView("redirect:/list/" + documentSet.getId())).orElseGet(() -> new ModelAndView("redirect:/search"));
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/lists");
    }
  }

  @GetMapping("/list/{id:.+}")
  public ModelAndView getNamed(@PathVariable String id, @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "${locale.validatedLocale:en}") String locale) {
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled()) {
      return new ModelAndView("redirect:/");
    }
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      DocumentSet documentSet = variableSetService.get(id);
      if (!subjectAclService.isCurrentUser(documentSet.getUsername()) && !subjectAclService.isAdministrator() && !subjectAclService.isDataAccessOfficer())
        throw new ForbiddenException();

      Map<String, Object> params = newParameters();
      params.put("set", documentSet);
      params.put("configJson", getMicaConfigAsJson(config, getLang(locale, null)));
      return new ModelAndView("list", params);
    } else {
      return new ModelAndView("redirect:/signin?redirect=" + micaConfigService.getContextPath() + "/list/" + id);
    }
  }

  private String getMicaConfigAsJson(MicaConfig config, String language) {
    try {
      Mica.MicaConfigDto dto = dtos.asDto(config, language);
      dto = dto.toBuilder().clearTranslations().build();
      return JsonFormat.printToString(dto);
    } catch (Exception ignore) {
    }
    return "{}";
  }

}
