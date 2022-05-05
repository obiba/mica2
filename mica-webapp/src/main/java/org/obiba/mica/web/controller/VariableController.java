package org.obiba.mica.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.controller.domain.Annotation;
import org.obiba.mica.web.controller.domain.HarmonizationAnnotations;
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
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class VariableController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(VariableController.class);

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private StudyService studyService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private Searcher searcher;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private TaxonomyService taxonomyService;

  @GetMapping("/variable/{id:.+}")
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

    DatasetVariable variable = resolver.getType().equals(DatasetVariable.Type.Harmonized) ? getHarmonizedDatasetVariable(resolver.getDatasetId(), id, variableName) : getDatasetVariable(id, variableName);
    params.put("variable", variable);
    params.put("type", resolver.getType().toString());

    addStudyTableParameters(params, variable);

    Map<String, Taxonomy> taxonomies = taxonomyService.getVariableTaxonomies().stream()
      .collect(Collectors.toMap(TaxonomyEntity::getName, e -> e));

    // annotations are attributes described by some taxonomies
    List<Annotation> annotations = variable.hasAttributes() ? variable.getAttributes().asAttributeList().stream()
      .filter(attr -> attr.hasNamespace() && taxonomies.containsKey(attr.getNamespace()) && taxonomies.get(attr.getNamespace()).hasVocabulary(attr.getName()))
      .map(attr -> new Annotation(attr, taxonomies.get(attr.getNamespace())))
      .collect(Collectors.toList()) : Lists.newArrayList();

    List<Annotation> harmoAnnotations = annotations.stream()
      .filter(annot -> annot.getTaxonomyName().equals("Mlstr_harmo"))
      .collect(Collectors.toList());

    List<Annotation> dataschemaAnnotations = annotations.stream()
      .filter(annot -> annot.getTaxonomyName().equals("Mlstr_dataschema"))
      .collect(Collectors.toList());

    List<String> exclusions = new ArrayList<String>() {{
      add("Mlstr_harmo");
      add("Mlstr_dataschema");
    }};
    annotations = annotations.stream()
      .filter(annot -> !exclusions.contains(annot.getTaxonomyName()))
      .collect(Collectors.toList());

    StringBuilder query = new StringBuilder();
    for (Annotation annot : annotations) {
      String expr = "in(" + annot.getTaxonomyName() + "." + annot.getVocabularyName() + "," + annot.getTermName() + ")";
      if (query.length() == 0)
        query = new StringBuilder(expr);
      else
        query = new StringBuilder("and(" + query + "," + expr + ")");
    }

    annotations.addAll(dataschemaAnnotations);
    params.put("annotations", annotations);
    params.put("harmoAnnotations", new HarmonizationAnnotations(harmoAnnotations));
    params.put("query", "variable(" + query.toString() + ")");

    params.put("showDatasetContingencyLink", showDatasetContingencyLink());

    return new ModelAndView("variable", params);
  }

  @ExceptionHandler(NoSuchDatasetException.class)
  public ModelAndView datasetNotFoundError(NoSuchDatasetException ex) {
    return makeErrorModelAndView("404", ex.getMessage());
  }

  @ExceptionHandler(NoSuchVariableException.class)
  public ModelAndView variableNotFoundError(NoSuchVariableException ex) {
    return makeErrorModelAndView("404", ex.getMessage());
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

  private DatasetVariable getHarmonizedDatasetVariable(String datasetId, String variableId, String variableName) {
    String dataSchemaVariableId = DatasetVariable.IdResolver
      .encode(datasetId, variableName, DatasetVariable.Type.Dataschema, null, null, null, null);
    DatasetVariable harmonizedDatasetVariable = getDatasetVariableInternal(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE,
      variableId, variableName);
    DatasetVariable dataSchemaVariable = getDatasetVariableInternal(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE,
      dataSchemaVariableId, variableName);

    dataSchemaVariable.getAttributes().asAttributeList().forEach(a -> {
      if (!a.getName().startsWith("Mlstr_harmo")) harmonizedDatasetVariable.addAttribute(a);
    });

    return harmonizedDatasetVariable;
  }

  private Map<String, Object> getInitiativeDigest(String studyId) {
    boolean published = studyService.isPublished(studyId);
    BaseStudy study = getStudy(studyId, published);
    Map<String, Object> params = newParameters();
    params.put("id", studyId);
    params.put("acronym", study.getAcronym());
    params.put("name", study.getName());
    params.put("published", published);
    return params;
  }

  private void addStudyTableParameters(Map<String, Object> params, DatasetVariable variable) {
    try {
      String studyId = variable.getStudyId();
      boolean published = studyService.isPublished(studyId);
      BaseStudy study = getStudy(studyId, published);
      params.put("studyPublished", published);
      params.put("study", study);
      Population population = study.findPopulation(variable.getPopulationId().replace(variable.getStudyId() + ":", ""));
      if (population != null) {
        params.put("population", population);
        DataCollectionEvent dce = population.findDataCollectionEvent(variable.getDceId().replace(variable.getPopulationId() + ":", ""));
        params.put("dce", dce);
      }
      if (DatasetVariable.Type.Harmonized.equals(variable.getVariableType())) {
        HarmonizationDataset dataset = getHarmonizationDataset(variable.getDatasetId());
        Map<String, Object> initiativeDigest = getInitiativeDigest(dataset.getHarmonizationTable().getStudyId());
        params.put("initiative", initiativeDigest);

        if (DatasetVariable.OpalTableType.Study.equals(variable.getOpalTableType())) {
          Optional<StudyTable> studyTable = dataset.getStudyTables().stream().filter(st ->
            variable.getStudyId().equals(st.getStudyId()) && variable.getProject().equals(st.getProject()) && variable.getTable().equals(st.getTable()))
            .findFirst();
          if (studyTable.isPresent())
            params.put("opalTable", studyTable.get());
        } else {
          Optional<HarmonizationStudyTable> harmoStudyTable = dataset.getHarmonizationTables().stream().filter(st ->
            variable.getStudyId().equals(st.getStudyId()) && variable.getProject().equals(st.getProject()) && variable.getTable().equals(st.getTable()))
            .findFirst();
          if (harmoStudyTable.isPresent())
            params.put("opalTable", harmoStudyTable.get());
        }
      }
    } catch (Exception e) {
      // ignore
      log.warn("Failed at retrieving collected dataset's study/population/dce", e);
    }
  }

  private BaseStudy getStudy(String id, boolean published) {
    BaseStudy study = published ? publishedStudyService.findById(id) : studyService.findStudy(id);
    if (study == null) throw NoSuchStudyException.withId(id);
    checkAccess((study instanceof Study) ? "/individual-study" : "/harmonization-study", id);
    return study;
  }

  private HarmonizationDataset getHarmonizationDataset(String id) {
    return harmonizedDatasetService.findById(id);
  }

}
