package org.obiba.mica.search.reports.generators;

import au.com.bytecode.opencsv.CSVWriter;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.search.reports.ReportGenerator;
import org.obiba.mica.study.domain.BaseStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class StudyCsvReportGenerator extends DocumentCsvReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(StudyCsvReportGenerator.class);

  private final List<BaseStudy> studies;

  private final PersonService personService;

  private List<String> headers = Lists.newArrayList();

  public StudyCsvReportGenerator(List<BaseStudy> studies, String locale, PersonService personService) {
    super(locale);
    this.studies = studies;
    this.personService = personService;
    initialize();
  }

  private void initialize() {
    headers.add("id");
    headers.add("type");
    headers.add("acronym");
    headers.add("name");
    headers.add("objectives");
    studies.forEach(this::appendModel);
    headers.addAll(getModelKeys());
  }

  @Override
  public void write(OutputStream outputStream) {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
      zipOutputStream.putNextEntry(new ZipEntry("studies.csv"));
      super.write(zipOutputStream);
      zipOutputStream.putNextEntry(new ZipEntry("populations.csv"));
      ReportGenerator reporter = new StudyPopulationCsvReportGenerator(studies, getLocale());
      reporter.write(zipOutputStream);
      zipOutputStream.putNextEntry(new ZipEntry("data-collection-events.csv"));
      reporter = new StudyPopulationDCECsvReportGenerator(studies, getLocale());
      reporter.write(zipOutputStream);
      zipOutputStream.putNextEntry(new ZipEntry("persons.csv"));
      for (int i = 0; i< studies.size(); i++) {
        BaseStudy study = studies.get(i);
        reporter = new PersonCsvReportGenerator(study.getId(), personService.getStudyMemberships(study.getId()), getLocale()) {
          @Override
          protected String getParentIdHeader() {
            return "studyId";
          }

          @Override
          protected List<Person.Membership> getMemberships(Person person) {
            return person.getStudyMemberships();
          }
        };
        reporter.write(zipOutputStream, i>0);
      }
    } catch (IOException e) {
      log.error("Error when reporting studies", e);
    }
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    studies.forEach(std -> {
      List<String> line = Lists.newArrayList();
      line.add(std.getId());
      line.add(std.getResourcePath().replace("-study", ""));
      line.add(translate(std.getAcronym()));
      line.add(translate(std.getName()));
      line.add(translate(std.getObjectives()));

      Map<String, Object> model = getModels().get(std.getId());
      for (String key : getModelKeys()) {
        Object value = model.get(key);
        line.add(value == null ? "" : value.toString());
      }
      writer.writeNext(line.toArray(new String[line.size()]));
    });
  }

}
