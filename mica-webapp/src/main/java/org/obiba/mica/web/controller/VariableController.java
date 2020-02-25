package org.obiba.mica.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.controller.domain.Annotation;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
public class VariableController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(VariableController.class);

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private Searcher searcher;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private TaxonomyService taxonomyService;

  @GetMapping("/variable/{id}")
  public ModelAndView variable(@PathVariable String id) {
    Map<String, Object> params = newParameters();

    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(id);
    String datasetId = resolver.getDatasetId();
    String variableName = resolver.getName();

    switch (resolver.getType()) {
      case Collected:
        if (!collectedDatasetService.isPublished(datasetId)) throw NoSuchDatasetException.withId(datasetId);
        break;
      case Dataschema:
      case Harmonized:
        if (!harmonizedDatasetService.isPublished(datasetId)) throw NoSuchDatasetException.withId(datasetId);
        break;
    }

    DatasetVariable variable = getDatasetVariable(id, variableName);
    params.put("variable", variable);
    params.put("type", resolver.getType().toString());

    if (variable.hasAttribute("label", null)) {
      params.put("label", variable.getAttributes().getAttribute("label", null).getValues());
    }

    Map<String, Taxonomy> taxonomies = taxonomyService.getVariableTaxonomies().stream()
      .collect(Collectors.toMap(TaxonomyEntity::getName, e -> e));

    // annotations are attributes described by some taxonomies
    List<Annotation> annotations = variable.getAttributes().asAttributeList().stream()
      .filter(attr -> attr.hasNamespace() && taxonomies.containsKey(attr.getNamespace()) && taxonomies.get(attr.getNamespace()).hasVocabulary(attr.getName()))
      .map(attr -> new Annotation(attr, taxonomies.get(attr.getNamespace())))
      .collect(Collectors.toList());
    params.put("annotations", annotations);

    return new ModelAndView("variable", params);
  }

  @ExceptionHandler({NoSuchDatasetException.class, NoSuchVariableException.class})
  public ModelAndView notFoundError(NoSuchElementException ex) {
    ModelAndView model = new ModelAndView("error");
    model.addObject("status", 404);
    model.addObject("msg", ex.getMessage());
    return model;
  }

  //
  // Private methods
  //

  protected DatasetVariable getDatasetVariable(String variableId, String variableName) {
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

}
