/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContactTest {

  private static final String INSTITUTION_NAME = "Institution Name";

  @Test
  public void test_clean_contact_with_title() {
    Person contact = new Person();
    contact.setLastName("Prof. Foo Bar (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanPerson();
    assertThat(contact.getTitle()).isEqualTo("Prof.");
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isNullOrEmpty();
  }

  @Test
  public void test_clean_contact_without_title() {
    Person contact = new Person();
    contact.setLastName("Foo Bar (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanPerson();
    assertThat(contact.getTitle()).isNullOrEmpty();
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isNullOrEmpty();
  }

  @Test
  public void test_clean_contact_with_title_academic_level() {
    Person contact = new Person();
    contact.setLastName("Dr. Foo Bar, Ph.D. (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanPerson();
    assertThat(contact.getTitle()).isEqualTo("Dr.");
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isEqualTo("Ph.D.");
  }

  @Test
  public void test_clean_contact_with_academic_level() {
    Person contact = new Person();
    contact.setLastName("Foo Bar, Ph.D. (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanPerson();
    assertThat(contact.getTitle()).isNullOrEmpty();
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isEqualTo("Ph.D.");
  }

  private Person.Institution newInstitution() {
    Person.Institution institution = new Person.Institution();
    institution.setName(LocalizedString.en(INSTITUTION_NAME));
    return institution;
  }
}
