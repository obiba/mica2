package org.obiba.mica.web.controller;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkSetService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.security.SubjectUtils;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudySetService;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@Scope("request")
public class CompareController extends BaseController {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Searcher searcher;

  @Inject
  private StudySetService studySetService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private NetworkSetService networkSetService;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @GetMapping("/compare")
  public ModelAndView get(HttpServletRequest request, @RequestParam(required = false) String type, @RequestParam(required = false) String ids, @RequestParam(required = false) String query) {
    Subject subject = SecurityUtils.getSubject();
    MicaConfig config = micaConfigService.getConfig();
    String documentType = Strings.isNullOrEmpty(type) ? "studies" : type.toLowerCase();
    Set<String> documentIds = Sets.newTreeSet();
    List<Study> individualStudies = Lists.newArrayList();
    List<HarmonizationStudy> harmonizationStudies = Lists.newArrayList();
    List<Network> networks = Lists.newArrayList();
    String searchQuery = "";
    Map<String, Object> params = newParameters();

    if (!Strings.isNullOrEmpty(ids))
      documentIds.addAll(Splitter.on(",").splitToList(ids));

    if (!Strings.isNullOrEmpty(query)) {
      if ("studies".equals(documentType) && config.isStudiesCompareEnabled()) {
        documentIds.addAll(queryStudyIds(query));
      } else if ("networks".equals(documentType) && config.isStudiesCompareEnabled()) {
        documentIds.addAll(queryNetworkIds(query));
      }
    }

    if (documentIds.isEmpty() && (subject.isAuthenticated() || micaConfigService.getConfig().isAnonymousCanCreateCart())) {
      if ("studies".equals(documentType)) {
        documentIds.addAll(getStudiesCartIds(request));
      } else if ("networks".equals(documentType)) {
        documentIds.addAll(getNetworksCartIds(request));
      }
    }
    params.put("type", documentType);
    params.put("ids", documentIds);

    if (!documentIds.isEmpty()) {
      if ("studies".equals(documentType) && config.isStudiesCompareEnabled()) {
        List<BaseStudy> studies = publishedStudyService.findByIds(Lists.newArrayList(documentIds).subList(0, Math.min(documentIds.size(), config.getMaxItemsPerCompare())));
        individualStudies = studies.stream().filter(study -> study instanceof Study).map(study -> (Study)study).collect(Collectors.toList());
        harmonizationStudies = studies.stream().filter(study -> study instanceof HarmonizationStudy).map(study -> (HarmonizationStudy)study).collect(Collectors.toList());
        searchQuery = String.format("study(in(Mica_study.id,(%s)))", Joiner.on(",").join(documentIds));
      } else if ("networks".equals(documentType) && config.isNetworksCompareEnabled()) {
        networks = publishedNetworkService.findByIds(Lists.newArrayList(documentIds).subList(0, Math.min(documentIds.size(), config.getMaxItemsPerCompare())));
        searchQuery = String.format("network(in(Mica_network.id,(%s)))", Joiner.on(",").join(documentIds));
      }
    }
    params.put("individualStudies", individualStudies);
    params.put("harmonizationStudies", harmonizationStudies);
    params.put("networks", networks);
    params.put("query", searchQuery);

    return new ModelAndView("compare", params);
  }

  private Set<String> getStudiesCartIds(HttpServletRequest request) {
    return SecurityUtils.getSubject().isAuthenticated() ?
      studySetService.getCartCurrentUser().getIdentifiers() :
      studySetService.getCartAnonymousUser(SubjectUtils.getAnonymousUserId(request)).getIdentifiers();
  }

  private Set<String> getNetworksCartIds(HttpServletRequest request) {
    return SecurityUtils.getSubject().isAuthenticated() ?
      networkSetService.getCartCurrentUser().getIdentifiers() :
      networkSetService.getCartAnonymousUser(SubjectUtils.getAnonymousUserId(request)).getIdentifiers();
  }

  private List<String> queryStudyIds(String query) {
    MicaSearch.JoinQueryResultDto result = makeQuery(QueryType.STUDY, query);
    if (result.hasStudyResultDto() && result.getStudyResultDto().getTotalHits() > 0) {
      return result.getStudyResultDto().getExtension(MicaSearch.StudyResultDto.result).getSummariesList().stream()
        .map(Mica.StudySummaryDto::getId).collect(Collectors.toList());
    }
    return Lists.newArrayList();
  }

  private List<String> queryNetworkIds(String query) {
    MicaSearch.JoinQueryResultDto result = makeQuery(QueryType.NETWORK, query);
    if (result.hasNetworkResultDto() && result.getNetworkResultDto().getTotalHits() > 0) {
      return result.getNetworkResultDto().getExtension(MicaSearch.NetworkResultDto.result).getNetworksList().stream()
        .map(Mica.NetworkDto::getId).collect(Collectors.toList());
    }
    return Lists.newArrayList();
  }

  private MicaSearch.JoinQueryResultDto makeQuery(QueryType type, String query) {
    return joinQueryExecutor.query(type, searcher.makeJoinQuery(query));
  }
}
