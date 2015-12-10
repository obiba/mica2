package org.obiba.mica.network;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonAwareRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.network.domain.Network;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
public class NetworkRepositoryImpl implements NetworkRepositoryCustom, PersonAwareRepository<Network> {

  @Inject
  MongoTemplate mongoTemplate;

  @Inject
  PersonRepository personRepository;

  @Inject
  EventBus eventBus;

  @Override
  public PersonRepository getPersonRepository() {
    return personRepository;
  }

  @Override
  public EventBus getEventBus() {
    return eventBus;
  }

  @Override
  public List<Person> findAllPersonsByParent(Network network) {
    return personRepository.findByNetworkMemberships(network.getId());
  }

  @Override
  public Network saveWithReferences(Network network) {
    saveContacts(network);
    mongoTemplate.save(network);
    updateRemovedContacts(network);

    return network;
  }

  @Override
  public void deleteWithReferences(Network network) {
    mongoTemplate.remove(network);
    deleteContacts(network);
  }
}
