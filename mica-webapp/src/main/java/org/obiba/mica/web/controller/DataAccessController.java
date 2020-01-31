package org.obiba.mica.web.controller;

import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Map;

@Controller
public class DataAccessController {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @GetMapping("/data-access/{id}")
  public ModelAndView get(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = Maps.newHashMap();
      params.put("dar", getDataAccessRequest(id));
      return new ModelAndView("data-access", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=data-access/" + id);
    }
  }

  private DataAccessRequest getDataAccessRequest(String id) {
    return dataAccessRequestService.findById(id);
  }

  @GetMapping("/data-access/{id}/form")
  public ModelAndView getForm(@PathVariable String id) {
    return new ModelAndView("data-access-form");
  }

  @GetMapping("/data-access/{id}/amendment/{aid}/form")
  public ModelAndView getAmendmentForm(@PathVariable String id, @PathVariable String aid) {
    return new ModelAndView("data-access-amendment-form");
  }

  @GetMapping("/data-access/{id}/comments")
  public ModelAndView getComments(@PathVariable String id) {
    return new ModelAndView("data-access-comments");
  }

  @GetMapping("/data-access/{id}/history")
  public ModelAndView getHistory(@PathVariable String id) {
    return new ModelAndView("data-access-history");
  }

}
