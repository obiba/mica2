package org.obiba.mica.search.csvexport.generators;

import au.com.bytecode.opencsv.CSVWriter;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Population;

import java.util.List;
import java.util.Map;

class StudyPopulationCsvReportGenerator extends DocumentCsvReportGenerator {

  private final List<BaseStudy> studies;

  private List<String> headers = Lists.newArrayList();

  public StudyPopulationCsvReportGenerator(List<BaseStudy> studies, String locale) {
    super(locale);
    this.studies = studies;
    initialize();
  }

  private void initialize() {
    headers.add("studyId");
    headers.add("id");
    headers.add("name");
    headers.add("description");
    studies.forEach(study -> study.getPopulationsSorted().forEach(pop -> appendModel(getIdKey(study, pop), pop)));
    headers.addAll(getModelKeys());
  }

  private String getIdKey(BaseStudy study, Population population) {
    return study.getId() + ":" + population.getId();
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    studies.forEach(study -> study.getPopulationsSorted().forEach(pop -> {
      List<String> line = Lists.newArrayList();
      line.add(study.getId());
      line.add(pop.getId());
      line.add(translate(pop.getName()));
      line.add(translate(pop.getDescription()));

      Map<String, Object> model = getModels().get(getIdKey(study, pop));
      for (String key : getModelKeys()) {
        Object value = model.get(key);
        line.add(value == null ? "" : value.toString());
      }
      writer.writeNext(line.toArray(new String[line.size()]));
    }));
  }
}
