package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the {@link DataAccessCollaborator} entity.
 */
public interface DataAccessCollaboratorRepository extends MongoRepository<DataAccessCollaborator, String> {

  List<DataAccessCollaborator> findByRequestId(String requestId);

  Optional<DataAccessCollaborator> findByRequestIdAndEmail(String requestId, String email);

}
