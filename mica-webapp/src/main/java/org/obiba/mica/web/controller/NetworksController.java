package org.obiba.mica.web.controller;

import org.obiba.mica.network.service.PublishedNetworkService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class NetworksController extends EntityController {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @GetMapping("/networks")
  public ModelAndView list() {

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("networks", publishedNetworkService.findAll().stream()
      .filter(n -> isAccessible("/network", n.getId()))
      .collect(Collectors.toList()));

    return new ModelAndView("networks", params);
  }

}
