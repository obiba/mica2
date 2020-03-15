package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.*;
import org.obiba.mica.study.service.PublishedStudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DatasetController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @GetMapping("/dataset/{id}")
  public ModelAndView study(@PathVariable String id) {
    Map<String, Object> params = newParameters();
    Dataset dataset = getDataset(id);
    params.put("dataset", dataset);
    params.put("type", (dataset instanceof StudyDataset) ? "Collected" : "Harmonized");
    if (dataset instanceof StudyDataset) {
      addStudyTableParameters(params, ((StudyDataset) dataset).getStudyTable());
    } else {
      HarmonizationDataset harmoDataset = (HarmonizationDataset) dataset;
      addHarmonizationTableParameters(params, harmoDataset.getHarmonizationTable());
      Map<String, BaseStudy> allStudies = Maps.newHashMap();
      List<BaseStudyTable> allTables = Lists.newArrayList();
      List<Map<String, Object>> studyTables = Lists.newArrayList();
      List<String> ids = Lists.newArrayList();
      for (StudyTable sTable : harmoDataset.getStudyTables()) {
        Map<String, Object> p = new HashMap<String, Object>();
        addStudyTableParameters(p, sTable, ids);
        if (!p.isEmpty()) {
          studyTables.add(p);
          allStudies.put(sTable.getStudyId(), (BaseStudy) p.get("study"));
        }
        allTables.add(sTable);
      }
      params.put("studyTables", studyTables);
      List<Map<String, Object>> harmonizationTables = Lists.newArrayList();
      ids.clear();
      for (HarmonizationStudyTable hTable : harmoDataset.getHarmonizationTables()) {
        Map<String, Object> p = new HashMap<String, Object>();
        addHarmonizationTableParameters(p, hTable, ids);
        if (!p.isEmpty()) {
          harmonizationTables.add(p);
          allStudies.put(hTable.getStudyId(), (BaseStudy) p.get("study"));
        }
        allTables.add(hTable);
      }
      params.put("harmonizationTables", harmonizationTables);
      params.put("allTables", allTables);
      params.put("allStudies", allStudies);
    }
    return new ModelAndView("dataset", params);
  }

  private void addStudyTableParameters(Map<String, Object> params, StudyTable studyTable) {
    addStudyTableParameters(params, studyTable, Lists.newArrayList());
  }

  private void addStudyTableParameters(Map<String, Object> params, StudyTable studyTable, List<String> ids) {
    try {
      Study study = (Study) getStudy(studyTable.getStudyId());
      Population population = study.findPopulation(studyTable.getPopulationId());
      DataCollectionEvent dce = population.findDataCollectionEvent(studyTable.getDataCollectionEventId());
      String id = study.getId() + ":" + population.getId() + ":" + dce.getId();
      if (!ids.contains(id)) { // could be different datasets from same dce
        params.put("study", study);
        params.put("population", population);
        params.put("dce", dce);
        ids.add(id);
      }
    } catch (Exception e) {
      // ignore
      log.warn("Failed at retrieving collected dataset's study/population/dce", e);
    }
  }

  private void addHarmonizationTableParameters(Map<String, Object> params, HarmonizationStudyTable studyTable) {
    addHarmonizationTableParameters(params, studyTable, Lists.newArrayList());
  }

  private void addHarmonizationTableParameters(Map<String, Object> params, HarmonizationStudyTable studyTable, List<String> ids) {
    try {
      HarmonizationStudy study = (HarmonizationStudy) getStudy(studyTable.getStudyId());
      Population population = study.findPopulation(studyTable.getPopulationId());
      String id = study.getId() + ":" + population.getId();
      if (!ids.contains(id)) { // could be different datasets from same population
        params.put("study", study);
        params.put("population", population);
        ids.add(id);
      }
    } catch (Exception e) {
      // ignore
      log.warn("Failed at retrieving harmonized dataset's study/population", e);
    }
  }

  private Dataset getDataset(String id) {
    Dataset dataset = publishedDatasetService.findById(id);
    if (dataset == null) throw NoSuchDatasetException.withId(id);
    checkAccess((dataset instanceof StudyDataset) ? "/collected-dataset" : "/harmonized-dataset", id);
    return dataset;
  }

  private BaseStudy getStudy(String id) {
    BaseStudy study = publishedStudyService.findById(id);
    if (study == null) throw NoSuchStudyException.withId(id);
    checkAccess((study instanceof Study) ? "/individual-study" : "/harmonization-study", id);
    return study;
  }

}
