package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.domain.ContactAware;

import com.google.common.collect.Sets;

public interface ContactAwareRepository<T extends ContactAware> {

  ContactRepository getContactRepository();

  default void saveContacts(T obj) {
    obj.getAllContacts().forEach(c -> obj.addToContact(c));
    ContactRepository contactRepository = getContactRepository();
    contactRepository.save(obj.getAllContacts());
  }

  default void deleteContacts(T obj) {
    obj.getAllContacts().forEach(c -> obj.removeFromContact(c));
    getContactRepository().save(obj.getAllContacts());
  }

  default void updateRemovedContacts(T obj) {
    List<Contact> contacts = findAllContactsByParent(obj);
    Sets.SetView<Contact> removedContacts = Sets
      .difference(Sets.newHashSet(contacts), Sets.newHashSet(obj.getAllContacts()));
    removedContacts.forEach(c -> obj.removeFromContact(c));
    getContactRepository().save(removedContacts);
  }

  List<Contact> findAllContactsByParent(T obj);
}
