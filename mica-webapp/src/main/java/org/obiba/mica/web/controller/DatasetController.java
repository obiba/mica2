package org.obiba.mica.web.controller;

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
import java.util.Map;

@Controller
public class DatasetController extends EntityController {

  private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @GetMapping("/dataset/{id}")
  public ModelAndView study(@PathVariable String id) {
    Map<String, Object> params = new HashMap<String, Object>();
    Dataset dataset = getDataset(id);
    params.put("dataset", dataset);
    params.put("type", (dataset instanceof StudyDataset) ? "Collected" : "Harmonized");
    if (dataset instanceof StudyDataset) {
      StudyTable studyTable = ((StudyDataset) dataset).getStudyTable();
      try {
        Study study = (Study) getStudy(studyTable.getStudyId());
        Population population = study.findPopulation(studyTable.getPopulationId());
        DataCollectionEvent dce = population.findDataCollectionEvent(studyTable.getDataCollectionEventId());
        params.put("study", study);
        params.put("population", population);
        params.put("dce", dce);
      } catch (Exception e) {
        // ignore
        log.warn("Failed at retrieving collected dataset's study/population/dce", e);
      }
    } else {
      HarmonizationStudyTable studyTable = ((HarmonizationDataset) dataset).getHarmonizationTable();
      try {
        HarmonizationStudy study = (HarmonizationStudy) getStudy(studyTable.getStudyId());
        Population population = study.findPopulation(studyTable.getPopulationId());
        params.put("study", study);
        params.put("population", population);
      } catch (Exception e) {
        // ignore
        log.warn("Failed at retrieving harmonized dataset's study/population", e);
      }
    }
    return new ModelAndView("dataset", params);
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
