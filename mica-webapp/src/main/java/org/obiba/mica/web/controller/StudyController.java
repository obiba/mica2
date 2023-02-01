package org.obiba.mica.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import com.googlecode.protobuf.format.JsonFormat;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.date.PersistableYearMonth;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.LocalizedStringDtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class StudyController extends BaseController {

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyService draftStudyService;

  @Inject
  private PersonService personService;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private ObjectMapper objectMapper;

  @GetMapping("/study/{id:.+}")
  public ModelAndView study(@PathVariable String id, @RequestParam(value = "draft", required = false) String shareKey) {
    Map<String, Object> params = newParameters();
    BaseStudy study = getStudy(id, shareKey);
    params.put("study", study);
    params.put("type", (study instanceof Study) ? "Individual" : "Harmonization");
    params.put("draft", !Strings.isNullOrEmpty(shareKey));

    if (study instanceof Study) {
      String timelineData = asJSONTimelineData((Study) study);
      params.put("timelineData", timelineData);
    }

    // Extract inferred attributes (variable based attributes)
    Map<String, Map<String, List<LocalizedString>>> collect = study.getInferredAttributes().stream()
      .collect(
        Collectors.groupingBy(Attribute::getNamespace,
          Collectors.groupingBy(Attribute::getName, Collectors.mapping(Attribute::getValues, Collectors.toList()))));

    return new ModelAndView("study", params);
  }

  private BaseStudy getStudy(String id, String shareKey) {
    BaseStudy study;
    if (Strings.isNullOrEmpty(shareKey)) {
      if ("_".equals(id))
        study = publishedStudyService.findAll().stream().findFirst().orElse(null);
      else
        study = publishedStudyService.findById(id);
      if (study == null) throw NoSuchStudyException.withId(id);
      checkAccess((study instanceof Study) ? "/individual-study" : "/harmonization-study", id);
    } else {
      study = draftStudyService.findStudy(id);
      if (study == null) throw NoSuchStudyException.withId(id);
      checkPermission("/draft/" + ((study instanceof Study) ? "individual-study" : "harmonization-study"), "VIEW", id, shareKey);
    }

    study.setMemberships(personService.setMembershipOrder(study.getMembershipSortOrder(), personService.getStudyMembershipMap(id)));

    return study;
  }

  private String asJSONTimelineData(Study study) {
    Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
    builder.setId(study.getId())
      .addAllName(localizedStringDtos.asDto(study.getName()));
    if (study.hasPopulations()) {
      study.getPopulations().stream().sorted(Comparator.comparingInt(Population::getWeight)).forEach(population -> builder.addPopulations(asDto(population)));
    }
    return JsonFormat.printToString(builder.build());
  }

  private Mica.PopulationDto asDto(Population population) {
    Mica.PopulationDto.Builder builder = Mica.PopulationDto.newBuilder();
    builder.setId(population.getId())
      .addAllName(localizedStringDtos.asDto(population.getName()));
    if (population.hasDataCollectionEvents()) {
      population.getDataCollectionEvents().forEach(dce -> builder.addDataCollectionEvents(asDto(dce)));
    }
    return builder.build();
  }

  private Mica.PopulationDto.DataCollectionEventDto asDto(DataCollectionEvent dce) {
    Mica.PopulationDto.DataCollectionEventDto.Builder builder = Mica.PopulationDto.DataCollectionEventDto.newBuilder();
    builder.setId(dce.getId())
      .addAllName(localizedStringDtos.asDto(dce.getName()));

    if (dce.getStart() != null) {
      PersistableYearMonth.YearMonthData startData = dce.getStart().getYearMonthData();
      builder.setStartYear(startData.getYear());
      if (startData.getMonth() != 0) {
        builder.setStartMonth(startData.getMonth());
      }
      if (startData.getDay() != null) {
        builder.setStartDay(startData.getDay().format(DateTimeFormatter.ISO_DATE));
      }
    }

    if (dce.getEnd() != null) {
      PersistableYearMonth.YearMonthData endData = dce.getEnd().getYearMonthData();
      builder.setEndYear(endData.getYear());
      if (endData.getMonth() != 0) {
        builder.setEndMonth(endData.getMonth());
      }
      if (endData.getDay() != null) {
        builder.setEndDay(endData.getDay().format(DateTimeFormatter.ISO_DATE));
      }
    }

    builder.setWeight(dce.getWeight());

    return builder.build();
  }
}
