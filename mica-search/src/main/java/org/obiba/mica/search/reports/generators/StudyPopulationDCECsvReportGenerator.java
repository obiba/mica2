package org.obiba.mica.search.reports.generators;

import au.com.bytecode.opencsv.CSVWriter;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.study.date.PersistableYearMonth;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

class StudyPopulationDCECsvReportGenerator extends DocumentCsvReportGenerator {

  private final List<BaseStudy> studies;

  private List<String> headers = Lists.newArrayList();

  public StudyPopulationDCECsvReportGenerator(List<BaseStudy> studies, String locale) {
    super(locale);
    this.studies = studies;
    initialize();
  }

  private void initialize() {
    headers.add("studyId");
    headers.add("populationId");
    headers.add("id");
    headers.add("name");
    headers.add("description");
    headers.add("start");
    headers.add("end");
    studies.forEach(study -> study.getPopulations().forEach(pop -> pop.getDataCollectionEvents().forEach(dce -> appendModel(getIdKey(study, pop, dce), dce))));
    headers.addAll(getModelKeys());
  }

  private String getIdKey(BaseStudy study, Population population, DataCollectionEvent dce) {
    return study.getId() + ":" + population.getId() + ":" + dce.getId();
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    studies.forEach(study -> study.getPopulationsSorted().forEach(pop -> pop.getDataCollectionEventsSorted().forEach(dce -> {
      List<String> line = Lists.newArrayList();
      line.add(study.getId());
      line.add(pop.getId());
      line.add(dce.getId());
      line.add(translate(dce.getName()));
      line.add(translate(dce.getDescription()));

      if (dce.hasStart()) {
        PersistableYearMonth start = dce.getStart();
        line.add(start.getDay() != null ? start.getDay().format(DateTimeFormatter.ISO_DATE) : dce.getStart().getYearMonth());
      } else {
        line.add("");
      }

      if (dce.hasEnd()) {
        PersistableYearMonth end = dce.getEnd();
        line.add(end.getDay() != null ? end.getDay().format(DateTimeFormatter.ISO_DATE) : dce.getEnd().getYearMonth());
      } else {
        line.add("");
      }

      Map<String, Object> model = getModels().get(getIdKey(study, pop, dce));
      for (String key : getModelKeys()) {
        Object value = model.get(key);
        line.add(value == null ? "" : value.toString());
      }
      writer.writeNext(line.toArray(new String[line.size()]));
    })));
  }
}
