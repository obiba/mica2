package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class NetworksController extends BaseController {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @GetMapping("/networks")
  public ModelAndView list() {

    Map<String, Object> params = newParameters();
    List<Network> networks;
    try {
      networks = publishedNetworkService.findAll().stream()
        .filter(n -> isAccessible("/network", n.getId()))
        .sorted(Comparator.comparing(AbstractGitPersistable::getId))
        .collect(Collectors.toList());
    } catch (Exception e) {
      networks = Lists.newArrayList();
    }
    params.put("networks", networks);

    return new ModelAndView("networks", params);
  }

}
