package org.obiba.mica.repository;

import org.obiba.mica.domain.Network;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Network entity.
 */
public interface NetworkRepository extends MongoRepository<Network, String> {

}
