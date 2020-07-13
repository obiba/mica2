package org.obiba.mica.web.controller;

import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;

@Controller
public class DatasetAnalysisController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(DatasetAnalysisController.class);

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @GetMapping("/dataset/{id:.+}/crosstab")
  public ModelAndView crosstab(@PathVariable String id) {
    ModelAndView mv = new ModelAndView("dataset-crosstab");
    Dataset dataset = getDataset(id);
    mv.getModelMap().addAttribute("dataset", dataset);
    mv.getModelMap().addAttribute("type", (dataset instanceof StudyDataset) ? "Collected" : "Harmonized");
    return mv;
  }

  private Dataset getDataset(String id) {
    Dataset dataset = publishedDatasetService.findById(id);
    if (dataset == null) throw NoSuchDatasetException.withId(id);
    checkAccess((dataset instanceof StudyDataset) ? "/collected-dataset" : "/harmonized-dataset", id);
    return dataset;
  }
}
