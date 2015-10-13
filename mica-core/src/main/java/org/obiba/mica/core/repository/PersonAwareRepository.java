package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.PersonAware;
import org.obiba.mica.core.domain.Person;

import com.google.common.collect.Sets;

import static java.util.stream.Collectors.toList;

public interface PersonAwareRepository<T extends PersonAware> {

  PersonRepository getPersonRepository();

  default void saveContacts(T obj) {
    obj.getAllPersons().forEach(c -> obj.addToPerson(c));
    PersonRepository personRepository = getPersonRepository();
    List<Person> tmp = obj.getAllPersons().stream().map(Membership::getPerson).collect(toList());
    personRepository.save(tmp);
  }

  default void deleteContacts(T obj) {
    obj.getAllPersons().forEach(c -> obj.removeFromPerson(c));
    getPersonRepository().save(obj.getAllPersons().stream().map(Membership::getPerson).collect(toList()));
  }

  default void updateRemovedContacts(T obj) {
    List<Person> persons = findAllPersonsByParent(obj);
    Sets.SetView<Person> removedPersons = Sets
      .difference(Sets.newHashSet(persons), Sets.newHashSet(obj.getAllPersons()));
    removedPersons.forEach(c -> obj.removeFromPerson(c));
    getPersonRepository().save(removedPersons);
  }

  List<Person> findAllPersonsByParent(T obj);
}
