/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Mica;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

public class CsvPersonsWriter {

  private final List<String> headers = Lists
    .newArrayList("Title", "FirstName", "LastName", "AcademicLevel", "Email", "Phone", "InstitutionName",
      "InstitutionDepartment", "InstitutionStreet", "InstitutionCity", "InstitutionPostalCode", "InstitutionState",
      "InstitutionCountry", "StudyMemberships", "InitiativeMemberships", "NetworkMemberships");

  public ByteArrayOutputStream write(Mica.PersonsDto persons) throws IOException {

    ByteArrayOutputStream values = new ByteArrayOutputStream();
    CSVWriter writer = null;
    try {
      writer = new CSVWriter(new PrintWriter(values));
      writer.writeNext(headers.toArray(new String[headers.size()]));
      writeBody(writer, persons);
    } finally {
      if(writer != null) writer.close();
    }

    return values;
  }

  private void writeBody(CSVWriter writer, Mica.PersonsDto persons) {
    if(persons.getPersonsCount() == 0) return;
    persons.getPersonsList().forEach(p -> writeBody(writer, p));
  }

  private void writeBody(CSVWriter writer, Mica.PersonDto person) {
    List<String> row = Lists.newArrayList();
    row.add(person.hasTitle() ? person.getTitle() : "");
    row.add(person.hasFirstName() ? person.getFirstName() : "");
    row.add(person.getLastName());
    row.add(person.hasAcademicLevel() ? person.getAcademicLevel() : "");
    row.add(person.hasEmail() ? person.getEmail() : "");
    row.add(person.hasPhone() ? person.getPhone() : "");
    addInstitution(row, person);
    List<Mica.PersonDto.MembershipDto> studyMembershipsList = person.getStudyMembershipsList();
    List<Mica.PersonDto.MembershipDto> studiesMemberships = new ArrayList<>();
    List<Mica.PersonDto.MembershipDto> initiativesMemberships = new ArrayList<>();
      studyMembershipsList.stream()
      .forEach(m -> {
        if (Study.RESOURCE_PATH.equals(m.getExtension(Mica.PersonDto.StudyMembershipDto.meta).getType())) {
          studiesMemberships.add(m);
        } else {
          initiativesMemberships.add(m);
        }
      });

    addMemberships(row, studiesMemberships);
    addMemberships(row, initiativesMemberships);
    addMemberships(row, person.getNetworkMembershipsList());

    writer.writeNext(row.toArray(new String[row.size()]));
  }

  private void addInstitution(List<String> row, Mica.PersonDto person) {
    if(person.hasInstitution()) {
      Mica.PersonDto.InstitutionDto institution = person.getInstitution();
      row.add(institution.getNameCount() > 0 ? institution.getName(0).getValue() : "");
      row.add(institution.getDepartmentCount() > 0 ? institution.getDepartment(0).getValue() : "");
      addAddress(row, institution);
    } else {
      row.addAll(Lists.newArrayList("", "", "", "", "", "", ""));
    }
  }

  private void addAddress(List<String> row, Mica.PersonDto.InstitutionDto institution) {
    if(institution.hasAddress()) {
      Mica.AddressDto address = institution.getAddress();
      row.add(address.getStreetCount() > 0 ? address.getStreet(0).getValue() : "");
      row.add(address.getCityCount() > 0 ? address.getCity(0).getValue() : "");
      row.add(address.hasZip() ? address.getZip() : "");
      row.add(address.hasState() ? address.getState() : "");
      row.add(address.hasCountry() ? address.getCountry().getIso() : "");
    } else {
      row.addAll(Lists.newArrayList("", "", "", "", ""));
    }
  }

  private void addMemberships(List<String> row, List<Mica.PersonDto.MembershipDto> memberships) {
    if(memberships.size() > 0) {
      row.add(joinMemberShips(memberships));
    } else {
      row.add("");
    }
  }

  private String joinMemberShips(List<Mica.PersonDto.MembershipDto> memberships) {
    return Joiner.on(", ").join(memberships.stream().map(m -> m.getParentId() + " (" + m.getRole() + ")").iterator());
  }

}
