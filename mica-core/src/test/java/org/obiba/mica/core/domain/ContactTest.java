package org.obiba.mica.core.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContactTest {

  private static final String INSTITUTION_NAME = "Institution Name";

  @Test
  public void test_clean_contact_with_title() {
    Contact contact = new Contact();
    contact.setLastName("Prof. Foo Bar (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanContact();
    assertThat(contact.getTitle()).isEqualTo("Prof.");
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isNullOrEmpty();
  }

  @Test
  public void test_clean_contact_without_title() {
    Contact contact = new Contact();
    contact.setLastName("Foo Bar (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanContact();
    assertThat(contact.getTitle()).isNullOrEmpty();
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isNullOrEmpty();
  }

  @Test
  public void test_clean_contact_with_title_academic_level() {
    Contact contact = new Contact();
    contact.setLastName("Dr. Foo Bar, Ph.D. (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanContact();
    assertThat(contact.getTitle()).isEqualTo("Dr.");
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isEqualTo("Ph.D.");
  }

  @Test
  public void test_clean_contact_with_academic_level() {
    Contact contact = new Contact();
    contact.setLastName("Foo Bar, Ph.D. (" + INSTITUTION_NAME + ")");
    contact.setInstitution(newInstitution());

    contact.cleanContact();
    assertThat(contact.getTitle()).isNullOrEmpty();
    assertThat(contact.getFirstName()).isEqualTo("Foo");
    assertThat(contact.getLastName()).isEqualTo("Bar");
    assertThat(contact.getAcademicLevel()).isEqualTo("Ph.D.");
  }

  private Contact.Institution newInstitution() {
    Contact.Institution institution = new Contact.Institution();
    institution.setName(LocalizedString.en(INSTITUTION_NAME));
    return institution;
  }
}
