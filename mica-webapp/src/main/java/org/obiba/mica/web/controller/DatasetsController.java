package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DatasetsController extends BaseController {

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @GetMapping("/datasets")
  public ModelAndView list() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("datasets", getDatasets());
    return new ModelAndView("datasets", params);
  }

  @GetMapping("/collected-datasets")
  public ModelAndView listCollectedDatasets() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("datasets", getDatasets().stream().filter(d -> d instanceof StudyDataset).collect(Collectors.toList()));
    params.put("type", "Collected");
    return new ModelAndView("datasets", params);
  }

  @GetMapping("/harmonized-datasets")
  public ModelAndView listHarmonizedDatasets() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("datasets", getDatasets().stream().filter(d -> d instanceof HarmonizationDataset).collect(Collectors.toList()));
    params.put("type", "Harmonized");
    return new ModelAndView("datasets", params);
  }

  private List<Dataset> getDatasets() {
    try {
      return publishedDatasetService.findAll().stream()
        .filter(d -> isAccessible((d instanceof StudyDataset) ? "/collected-dataset" : "/harmonized-dataset", d.getId()))
        .sorted(Comparator.comparing(AbstractGitPersistable::getId))
        .collect(Collectors.toList());
    } catch (Exception e) {
      return Lists.newArrayList();
    }
  }

}
