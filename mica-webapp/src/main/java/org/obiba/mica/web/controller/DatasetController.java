package org.obiba.mica.web.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.*;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DatasetController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private CollectedDatasetService draftCollectedDatasetService;

  @Inject
  private HarmonizedDatasetService draftHarmonizedDatasetService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyService studyService;

  @GetMapping("/dataset/{id:.+}")
  public ModelAndView dataset(@PathVariable String id, @RequestParam(value = "draft", required = false) String shareKey) {
    boolean shared = !Strings.isNullOrEmpty(shareKey);
    Map<String, Object> params = newParameters();
    Dataset dataset = getDataset(id, shareKey);
    params.put("dataset", dataset);
    params.put("type", (dataset instanceof StudyDataset) ? "Collected" : "Harmonized");
    params.put("draft", !Strings.isNullOrEmpty(shareKey));

    if (dataset instanceof StudyDataset) {
      addStudyTableParameters(params, ((StudyDataset) dataset).getStudyTable(), shared);
    } else {
      HarmonizationDataset harmoDataset = (HarmonizationDataset) dataset;
      addHarmonizationTableParameters(params, harmoDataset.getHarmonizationTable(), shared);
      Map<String, BaseStudy> allStudies = Maps.newHashMap();
      List<BaseStudyTable> allTables = Lists.newArrayList();
      List<Map<String, Object>> studyTables = Lists.newArrayList();
      List<String> ids = Lists.newArrayList();
      for (StudyTable sTable : harmoDataset.getStudyTables()) {
        Map<String, Object> p = new HashMap<>();
        if (addStudyTableParameters(p, sTable, ids, shared))
          allTables.add(sTable);
        if (!p.isEmpty()) {
          studyTables.add(p);
          allStudies.put(sTable.getStudyId(), (BaseStudy) p.get("study"));
        }
      }
      params.put("studyTables", studyTables);
      List<Map<String, Object>> harmonizationTables = Lists.newArrayList();
      ids.clear();
      for (HarmonizationStudyTable hTable : harmoDataset.getHarmonizationTables()) {
        Map<String, Object> p = new HashMap<>();
        if (addHarmonizationTableParameters(p, hTable, ids, shared))
          allTables.add(hTable);
        if (!p.isEmpty()) {
          harmonizationTables.add(p);
          allStudies.put(hTable.getStudyId(), (BaseStudy) p.get("study"));
        }
      }
      params.put("harmonizationTables", harmonizationTables);
      params.put("allTables", allTables);
      params.put("allStudies", allStudies);
    }

    params.put("showDatasetContingencyLink", showDatasetContingencyLink());

    return new ModelAndView("dataset", params);
  }

  private void addStudyTableParameters(Map<String, Object> params, StudyTable studyTable, boolean shared) {
    addStudyTableParameters(params, studyTable, Lists.newArrayList(), shared);
  }

  private boolean addStudyTableParameters(Map<String, Object> params, StudyTable studyTable, List<String> ids, boolean shared) {
    try {
      Study study = (Study) getStudy(studyTable.getStudyId(), shared);
      Population population = study.findPopulation(studyTable.getPopulationId());
      DataCollectionEvent dce = null;
      String id = study.getId();

      if (population != null) {
        id += ":" + population.getId();
        dce = population.findDataCollectionEvent(studyTable.getDataCollectionEventId());
        if (dce != null) {
          id += ":" + dce.getId();
        }
      }

      if (!ids.contains(id)) { // could be different datasets from same dce
        params.put("study", study);
        params.put("population", population);
        params.put("dce", dce);
        ids.add(id);
      }
      return true;
    } catch (Exception e) {
      // ignore
      log.warn("Failed at retrieving collected dataset's study/population/dce", e);
    }
    return false;
  }

  private void addHarmonizationTableParameters(Map<String, Object> params, HarmonizationStudyTable studyTable, boolean shared) {
    addHarmonizationTableParameters(params, studyTable, Lists.newArrayList(), shared);
  }

  private boolean addHarmonizationTableParameters(Map<String, Object> params, HarmonizationStudyTable studyTable, List<String> ids, boolean shared) {
    try {
      HarmonizationStudy study = (HarmonizationStudy) getStudy(studyTable.getStudyId(), shared);
      String id = study.getId();
      if (!ids.contains(id)) { // could be different datasets from same population
        params.put("study", study);
        ids.add(id);
      }
      return true;
    } catch (Exception e) {
      // ignore
      log.warn("Failed at retrieving harmonized dataset's study/population", e);
    }
    return false;
  }

  private Dataset getDataset(String id, String shareKey) {
    Dataset dataset;
    if (Strings.isNullOrEmpty(shareKey)) {
      dataset = publishedDatasetService.findById(id);
      if (dataset == null) throw NoSuchDatasetException.withId(id);
      checkAccess((dataset instanceof StudyDataset) ? "/collected-dataset" : "/harmonized-dataset", id);
    } else {
      try {
        dataset = draftCollectedDatasetService.findById(id);
        checkPermission("/draft/collected-dataset", "VIEW", id, shareKey);
      } catch (NoSuchDatasetException ex) {
        dataset = draftHarmonizedDatasetService.findById(id);
        checkPermission("/draft/harmonized-dataset", "VIEW", id, shareKey);
      }
    }
    return dataset;
  }

  private BaseStudy getStudy(String id, boolean shared) {
    BaseStudy study = shared ? studyService.findDraft(id) : publishedStudyService.findById(id);
    if (study == null) throw NoSuchStudyException.withId(id);
    checkAccess((study instanceof Study) ? "/individual-study" : "/harmonization-study", id);
    return study;
  }

}
