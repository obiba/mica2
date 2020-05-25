package org.obiba.mica.network;

import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.study.EntityStateRepositoryImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class NetworkStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public NetworkStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, NetworkState.class.getSimpleName());
  }
}
