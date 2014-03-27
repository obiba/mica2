package org.obiba.mica.service;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDateTime;
import org.obiba.mica.config.audit.AuditEventConverter;
import org.obiba.mica.jpa.domain.PersistentAuditEvent;
import org.obiba.mica.jpa.repository.PersistenceAuditEventRepository;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing audit events.
 * <p/>
 * <p>
 * This is the default implementation to support SpringBoot Actuator AuditEventRepository
 * </p>
 */
@Service
@Transactional
public class AuditEventService {

  @Inject
  private PersistenceAuditEventRepository persistenceAuditEventRepository;

  @Inject
  private AuditEventConverter auditEventConverter;

  public List<AuditEvent> findAll() {
    return auditEventConverter.convertToAuditEvent(persistenceAuditEventRepository.findAll());
  }

  public List<AuditEvent> findByDates(LocalDateTime fromDate, LocalDateTime toDate) {
    List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findByDates(fromDate, toDate);

    return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
  }
}
