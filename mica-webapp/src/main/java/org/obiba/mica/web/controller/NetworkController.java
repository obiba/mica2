package org.obiba.mica.web.controller;

import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class NetworkController extends EntityController {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @GetMapping("/network/{id}")
  public ModelAndView network(@PathVariable String id) {

    Map<String, Object> params = new HashMap<String, Object>();
    Network network = getNetwork(id);
    params.put("network", network);

    List<Network> networks = publishedNetworkService.findByIds(network.getNetworkIds());
    params.put("networks", networks.stream()
      .filter(n -> subjectAclService.isAccessible("/network", n.getId()))
      .collect(Collectors.toList()));

    List<BaseStudy> studies = publishedStudyService.findByIds(network.getStudyIds());
    params.put("individualStudies", studies.stream()
      .filter(s -> (s instanceof Study) && subjectAclService.isAccessible("/individual-study", s.getId()))
      .collect(Collectors.toList()));
    params.put("harmonizationStudies", studies.stream()
      .filter(s -> (s instanceof HarmonizationStudy) && subjectAclService.isAccessible("/harmonization-study", s.getId()))
      .collect(Collectors.toList()));

    return new ModelAndView("network", params);
  }

  private Network getNetwork(String id) {
    Network network = publishedNetworkService.findById(id);
    if (network == null) throw NoSuchNetworkException.withId(id);
    checkAccess("/network", id);
    return network;
  }

}
