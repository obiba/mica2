package org.obiba.mica.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriUtils;
import sun.net.util.URLUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Controller
public class DatasetAnalysisController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(DatasetAnalysisController.class);

  @Inject
  private Searcher searcher;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private Dtos dtos;

  @Inject
  private OpalService opalService;

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @GetMapping("/dataset-crosstab/{id:.+}")
  public ModelAndView crosstab(@PathVariable String id,
                               @RequestParam(value = "var1", required = false) String var1,
                               @RequestParam(value = "var2", required = false) String var2) {

    Subject subject = SecurityUtils.getSubject();
    String contextPath = micaConfigService.getContextPath();
    if (!subject.isAuthenticated()) {
      String query = "";
      if (!Strings.isNullOrEmpty(var1))
        query = "var1=" + var1;
      if (!Strings.isNullOrEmpty(var2))
        query = (Strings.isNullOrEmpty(query) ? "" : query + "&") + "var2=" + var2;
      String redirect = encode(contextPath + "/dataset-crosstab/" + id + (Strings.isNullOrEmpty(query) ? "" : "?") + query);
      return new ModelAndView("redirect:/signin?redirect=" + redirect);
    }

    ModelAndView mv = new ModelAndView("dataset-crosstab");
    Dataset dataset = getDataset(id);
    mv.getModelMap().addAttribute("dataset", dataset);
    String type = (dataset instanceof StudyDataset) ? "Collected" : "Harmonized";
    String variableType = (dataset instanceof StudyDataset) ? "Collected" : "Dataschema";
    mv.getModelMap().addAttribute("type", type);

    if (!Strings.isNullOrEmpty(var1)) {
      mv.getModelMap().addAttribute("var1", var1);
      DatasetVariable variable1 = getDatasetVariable(id + ":" + var1 + ":" + variableType, var1);
      mv.getModelMap().addAttribute("variable1", getDatasetVariableJSON(variable1));
    }

    if (!Strings.isNullOrEmpty(var2)) {
      mv.getModelMap().addAttribute("var2", var2);
      DatasetVariable variable2 = getDatasetVariable(id + ":" + var2 + ":" + variableType, var2);
      mv.getModelMap().addAttribute("variable2", getDatasetVariableJSON(variable2));
    }

    return mv;
  }

  //
  // Private methods
  //

  private DatasetVariable getDatasetVariable(String variableId, String variableName) {
    return getDatasetVariableInternal(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, variableId, variableName);
  }

  private DatasetVariable getDatasetVariableInternal(String indexName, String indexType, String variableId, String variableName) {
    InputStream inputStream = searcher.getDocumentById(indexName, indexType, variableId);
    if (inputStream == null) throw new NoSuchVariableException(variableName);
    try {
      return objectMapper.readValue(inputStream, DatasetVariable.class);
    } catch (IOException e) {
      log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      throw new NoSuchVariableException(variableName);
    }
  }

  private Dataset getDataset(String id) {
    Dataset dataset = publishedDatasetService.findById(id);
    if (dataset == null) throw NoSuchDatasetException.withId(id);
    checkAccess((dataset instanceof StudyDataset) ? "/collected-dataset" : "/harmonized-dataset", id);
    return dataset;
  }

  private List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch (Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  private String getDatasetVariableJSON(DatasetVariable variable) {
    return JsonFormat.printToString(dtos.asDto(variable, getTaxonomies()));
  }
}
