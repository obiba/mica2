/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDateTime;
import org.obiba.mica.config.audit.AuditEventConverter;
import org.obiba.mica.core.domain.PersistentAuditEvent;
import org.obiba.mica.core.repository.PersistenceAuditEventRepository;
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
