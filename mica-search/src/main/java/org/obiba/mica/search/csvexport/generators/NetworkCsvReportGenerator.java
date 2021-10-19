package org.obiba.mica.search.csvexport.generators;

import au.com.bytecode.opencsv.CSVWriter;
import jersey.repackaged.com.google.common.base.Joiner;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.network.domain.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class NetworkCsvReportGenerator extends DocumentCsvReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(NetworkCsvReportGenerator.class);

  private final List<Network> networks;

  private final PersonService personService;

  private List<String> headers = Lists.newArrayList();

  public NetworkCsvReportGenerator(List<Network> networks, String locale, PersonService personService) {
    super(locale);
    this.networks = networks;
    this.personService = personService;
    initialize();
  }

  private void initialize() {
    headers.add("id");
    headers.add("acronym");
    headers.add("name");
    headers.add("description");
    headers.add("studyIds");
    headers.add("networkIds");
    networks.forEach(this::appendModel);
    headers.addAll(getModelKeys());
  }

  @Override
  public void write(OutputStream outputStream) {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
      zipOutputStream.putNextEntry(new ZipEntry("networks.csv"));
      super.write(zipOutputStream);
      zipOutputStream.putNextEntry(new ZipEntry("persons.csv"));
      for (int i = 0; i< networks.size(); i++) {
        Network network = networks.get(i);
        PersonCsvReportGenerator reporter = new PersonCsvReportGenerator(network.getId(), personService.getNetworkMemberships(network.getId()), getLocale()) {
          @Override
          protected String getParentIdHeader() {
            return "networkId";
          }

          @Override
          protected List<Person.Membership> getMemberships(Person person) {
            return person.getNetworkMemberships();
          }
        };
        reporter.write(zipOutputStream, i>0);
      }
    } catch (IOException e) {
      log.error("Error when reporting networks", e);
    }
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    networks.forEach(ntw -> {
      List<String> line = Lists.newArrayList();
      line.add(ntw.getId());
      line.add(translate(ntw.getAcronym()));
      line.add(translate(ntw.getName()));
      line.add(translate(ntw.getDescription()));
      line.add(Joiner.on("|").join(ntw.getStudyIds()));
      line.add(Joiner.on("|").join(ntw.getNetworkIds()));

      Map<String, Object> model = getModels().get(ntw.getId());
      for (String key : getModelKeys()) {
        Object value = model.get(key);
        line.add(value == null ? "" : value.toString());
      }
      writer.writeNext(line.toArray(new String[line.size()]));
    });
  }

}
