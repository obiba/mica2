package org.obiba.mica.web.controller;

import com.google.common.base.Joiner;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.service.PersonService;
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
public class NetworkController extends BaseController {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PersonService personService;

  @GetMapping("/network/{id}")
  public ModelAndView network(@PathVariable String id) {

    Map<String, Object> params = newParameters();
    Network network = getNetwork(id);
    params.put("network", network);

    List<Network> networks = publishedNetworkService.findByIds(network.getNetworkIds());
    params.put("networks", networks.stream()
      .filter(n -> subjectAclService.isAccessible("/network", n.getId()))
      .collect(Collectors.toList()));

    List<BaseStudy> studies = publishedStudyService.findByIds(network.getStudyIds());
    List<BaseStudy> individualStudies = studies.stream()
      .filter(s -> (s instanceof Study) && subjectAclService.isAccessible("/individual-study", s.getId()))
      .collect(Collectors.toList());
    params.put("individualStudies", individualStudies);
    List<BaseStudy> harmonizationStudies = studies.stream()
      .filter(s -> (s instanceof HarmonizationStudy) && subjectAclService.isAccessible("/harmonization-study", s.getId()))
      .collect(Collectors.toList());
    params.put("harmonizationStudies", harmonizationStudies);

    List<String> ids = individualStudies.stream().map(AbstractGitPersistable::getId).collect(Collectors.toList());
    ids.addAll(harmonizationStudies.stream().map(AbstractGitPersistable::getId).collect(Collectors.toList()));
    if (!ids.isEmpty()) {
      params.put("affiliatedMembersQuery","studyMemberships.parentId:(" + Joiner.on(" ").join(ids) + ")");
    }

    Map<String, List<Membership>> membershipMap = personService.getNetworkMembershipMap(network.getId());
    params.put("memberships", membershipMap);

    return new ModelAndView("network", params);
  }

  private Network getNetwork(String id) {
    Network network;
    if ("_".equals(id)) {
      network = publishedNetworkService.findAll().stream().findFirst().orElse(null);
    } else {
      network = publishedNetworkService.findById(id);
    }
    if (network == null) throw NoSuchNetworkException.withId(id);
    checkAccess("/network", id);
    return network;
  }

}
