package org.obiba.mica.search.csvexport.generators;

import au.com.bytecode.opencsv.CSVWriter;
import jersey.repackaged.com.google.common.base.Joiner;
import jersey.repackaged.com.google.common.collect.Lists;
import org.obiba.mica.core.domain.Address;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;

import java.util.List;
import java.util.Map;

abstract class PersonCsvReportGenerator extends DocumentCsvReportGenerator {

  private final String parentId;

  private final List<Person> persons;

  private List<String> headers = Lists.newArrayList();

  public PersonCsvReportGenerator(String parentId, List<Person> persons, String locale) {
    super(locale);
    this.parentId = parentId;
    this.persons = persons;
    initialize();
  }

  private void initialize() {
    headers.add(getParentIdHeader());
    headers.add("roles");
    headers.add("firstName");
    headers.add("lastName");
    headers.add("title");
    headers.add("academicLevel");
    headers.add("email");
    headers.add("phone");
    headers.add("institution.name");
    headers.add("institution.department");
    headers.add("institution.address.street");
    headers.add("institution.address.city");
    headers.add("institution.address.zip");
    headers.add("institution.address.state");
    headers.add("institution.address.country");
  }

  @Override
  protected void writeHeader(CSVWriter writer) {
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  @Override
  protected void writeEachLine(CSVWriter writer) {
    persons.forEach(person -> {
      List<String> line = Lists.newArrayList();
      line.add(parentId);
      line.add(getMemberships(person).stream()
        .filter(mb -> mb.getParentId().equals(parentId))
        .map(Person.Membership::getRole)
        .distinct().sorted().reduce("", (partial, current) -> partial.length()>0 ? partial + "|" + current : current));
      line.add(person.getFirstName());
      line.add(person.getLastName());
      line.add(person.getTitle());
      line.add(person.getAcademicLevel());
      line.add(person.getEmail());
      line.add(person.getPhone());
      Person.Institution institution = person.getInstitution();
      if (institution != null) {
        line.add(translate(institution.getName()));
        line.add(translate(institution.getDepartment()));
        Address address = institution.getAddress();
        if (address != null) {
          line.add(translate(address.getStreet()));
          line.add(translate(address.getCity()));
          line.add(address.getZip());
          line.add(address.getState());
          line.add(address.getCountryIso());
        }
      }
      writer.writeNext(line.toArray(new String[line.size()]));
    });
  }

  protected abstract String getParentIdHeader();

  protected abstract List<Person.Membership> getMemberships(Person person);

}
