package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
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
  public ModelAndView get(@RequestParam(required = false) String type, @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale) {
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled() && !config.isStudiesCartEnabled() && !config.isNetworksCartEnabled()) {
      return new ModelAndView("redirect:/");
    }
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      // make sure cart exists
      variableSetService.getAllCurrentUser().stream().filter(set -> !set.hasName())
        .findFirst().orElseGet(() -> variableSetService.create("", Lists.newArrayList()));
      // note: the cart will be populated by the SessionInterceptor
      Map<String, Object> params = newParameters();
      params.put("accessConfig", dataAccessConfigService.getOrCreateConfig());
      if (!Strings.isNullOrEmpty(type) &&
        ((type.equalsIgnoreCase("variables") && config.isCartEnabled()) ||
          (type.equalsIgnoreCase("studies") && config.isStudiesCartEnabled()) ||
          (type.equalsIgnoreCase("networks") && config.isNetworksCartEnabled()))) {
        params.put("showCartType", type.toLowerCase());
      } else {
        params.put("showCartType", config.isCartEnabled() ? "variables" : (config.isStudiesCartEnabled() ? "studies" : "networks"));
      }

      params.put("configJson", getMicaConfigAsJson(config, getLang(locale, null)));
      return new ModelAndView("cart", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/cart");
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
  public ModelAndView getNamed(@PathVariable String id, @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale) {
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
