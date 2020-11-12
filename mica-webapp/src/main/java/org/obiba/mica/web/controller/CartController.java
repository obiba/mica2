package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

@Controller
public class CartController extends BaseController {

  @Inject
  private VariableSetService variableSetService;

  @GetMapping("/cart")
  public ModelAndView get() {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      DocumentSet cart = variableSetService.getAllCurrentUser().stream().filter(set -> !set.hasName())
        .findFirst().orElseGet(() -> variableSetService.create("", Lists.newArrayList()));
      // note: the cart will be populated by the SessionInterceptor
      return new ModelAndView("cart");
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/cart");
    }
  }

  @GetMapping("/lists")
  public ModelAndView lists() {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Optional<DocumentSet> tentative = variableSetService.getAllCurrentUser().stream().filter(DocumentSet::hasName).findFirst();
      return tentative.map(documentSet -> new ModelAndView("redirect:/list/" + documentSet.getId())).orElseGet(() -> new ModelAndView("redirect:/search"));
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/lists");
    }
  }

  @GetMapping("/list/{id:.+}")
  public ModelAndView getNamed(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters();
      params.put("set", variableSetService.get(id));
      return new ModelAndView("list", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=" + micaConfigService.getContextPath() + "/list/" + id);
    }
  }

}
