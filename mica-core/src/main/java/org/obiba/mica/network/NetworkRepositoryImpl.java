package org.obiba.mica.network;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.repository.ContactAwareRepository;
import org.obiba.mica.core.repository.ContactRepository;
import org.obiba.mica.network.domain.Network;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class NetworkRepositoryImpl implements NetworkRepositoryCustom, ContactAwareRepository<Network> {

  @Inject
  MongoTemplate mongoTemplate;

  @Inject
  ContactRepository contactRepository;

  @Override
  public ContactRepository getContactRepository() {
    return contactRepository;
  }

  @Override
  public List<Contact> findAllContactsByParent(Network network) {
    return contactRepository.findByNetworkIds(network.getId());
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
