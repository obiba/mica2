package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.core.domain.Contact;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContactRepository extends MongoRepository<Contact, String> {
  List<Contact> findByStudyIds(String studyId);

  List<Contact> findByNetworkIds(String networkId);

  Contact findOneByEmail(String email);
}
