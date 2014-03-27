package org.obiba.mica.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

/**
 * Persist AuditEvent managed by the Spring Boot actuator
 *
 * @see org.springframework.boot.actuate.audit.AuditEvent
 */

@Entity
@Table(name = "T_PERSISTENT_AUDIT_EVENT")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PersistentAuditEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE)
  @Column(name = "event_id")
  private long id;

  @NotNull
  @Column(name = "principal")
  private String principal;

  @Column(name = "event_date")
  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
  private LocalDateTime auditEventDate;

  @Column(name = "event_type")
  private String auditEventType;

  @ElementCollection
  @MapKeyColumn(name = "name")
  @Column(name = "value")
  @CollectionTable(name = "T_PERSISTENT_AUDIT_EVENT_DATA", joinColumns = @JoinColumn(name = "event_id"))
  private Map<String, String> data = new HashMap<>();

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public LocalDateTime getAuditEventDate() {
    return auditEventDate;
  }

  public void setAuditEventDate(LocalDateTime auditEventDate) {
    this.auditEventDate = auditEventDate;
  }

  public String getAuditEventType() {
    return auditEventType;
  }

  public void setAuditEventType(String auditEventType) {
    this.auditEventType = auditEventType;
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }
}
