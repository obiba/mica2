package org.obiba.mica.web.controller;

import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.study.NoSuchStudyException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DatasetController extends EntityController {

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @GetMapping("/dataset/{id}")
  public ModelAndView study(@PathVariable String id) {
    Map<String, Object> params = new HashMap<String, Object>();
    Dataset dataset = getDataset(id);
    params.put("dataset", dataset);
    params.put("type", (dataset instanceof StudyDataset) ? "Collected" : "Harmonized");
    return new ModelAndView("dataset", params);
  }

  private Dataset getDataset(String id) {
    Dataset dataset = publishedDatasetService.findById(id);
    if (dataset == null) throw NoSuchDatasetException.withId(id);
    checkAccess((dataset instanceof StudyDataset) ? "/collected-dataset" : "/harmonized-dataset", id);
    return dataset;
  }

}
