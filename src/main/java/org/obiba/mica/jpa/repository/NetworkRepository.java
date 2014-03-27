package org.obiba.mica.jpa.repository;

import org.obiba.mica.jpa.domain.Network;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Network entity.
 */
public interface NetworkRepository extends JpaRepository<Network, Long> {

}
