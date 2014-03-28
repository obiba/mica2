package org.obiba.mica.jpa.repository;

import org.obiba.mica.jpa.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the User entity.
 */
public interface UserRepository extends JpaRepository<User, String> {

}
