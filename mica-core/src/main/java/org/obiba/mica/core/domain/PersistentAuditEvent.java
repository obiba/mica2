/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import org.joda.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Persist AuditEvent managed by the Spring Boot actuator
 */
@Document(collection = "T_PERSISTENT_AUDIT_EVENT")
public class PersistentAuditEvent {

  @Id
  @Field("event_id")
  private long id;

  @NotNull
  private String principal;

  private LocalDateTime auditEventDate;

  @Field("event_type")
  private String auditEventType;

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
