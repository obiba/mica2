package org.obiba.mica.repository;

import org.obiba.mica.domain.Network;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Network entity.
 */
public interface NetworkRepository extends JpaRepository<Network, Long> {

}
