package org.obiba.mica.repository;

import java.util.List;

import org.joda.time.LocalDate;
import org.obiba.mica.domain.PersistentToken;
import org.obiba.mica.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the User entity.
 */
public interface PersistentTokenRepository extends JpaRepository<PersistentToken, String> {

  List<PersistentToken> findByUser(User user);

  List<PersistentToken> findByTokenDateBefore(LocalDate localDate);

}
