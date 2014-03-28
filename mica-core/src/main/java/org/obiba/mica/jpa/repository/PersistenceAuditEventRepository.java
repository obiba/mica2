package org.obiba.mica.jpa.repository;

import java.util.List;

import org.joda.time.LocalDateTime;
import org.obiba.mica.jpa.domain.PersistentAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for the PersistentAuditEvent entity.
 */
public interface PersistenceAuditEventRepository extends JpaRepository<PersistentAuditEvent, String> {

  List<PersistentAuditEvent> findByPrincipal(String principal);

  List<PersistentAuditEvent> findByPrincipalAndAuditEventDateGreaterThan(String principal, LocalDateTime after);

  @Query("select p from PersistentAuditEvent p where p.auditEventDate >= ?1 and p.auditEventDate <= ?2")
  List<PersistentAuditEvent> findByDates(LocalDateTime fromDate, LocalDateTime toDate);
}
