/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.obiba.mica.core.domain.PersistentAuditEvent;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditEventConverter {

  /**
   * Convert a list of PersistentAuditEvent to a list of AuditEvent
   *
   * @param persistentAuditEvents the list to convert
   * @return the converted list.
   */
  public List<AuditEvent> convertToAuditEvent(Iterable<PersistentAuditEvent> persistentAuditEvents) {
    if(persistentAuditEvents == null) {
      return Collections.emptyList();
    }

    List<AuditEvent> auditEvents = new ArrayList<>();

    for(PersistentAuditEvent persistentAuditEvent : persistentAuditEvents) {
      AuditEvent auditEvent = new AuditEvent(persistentAuditEvent.getAuditEventDate().toDate(),
          persistentAuditEvent.getPrincipal(), persistentAuditEvent.getAuditEventType(),
          convertDataToObjects(persistentAuditEvent.getData()));
      auditEvents.add(auditEvent);
    }

    return auditEvents;
  }

  /**
   * Internal conversion. This is needed to support the current SpringBoot actuator AuditEventRepository interface
   *
   * @param data the data to convert
   * @return a map of String, Object
   */
  public Map<String, Object> convertDataToObjects(Map<String, String> data) {
    Map<String, Object> results = new HashMap<>();

    if(data != null) {
      for(String key : data.keySet()) {
        results.put(key, data.get(key));
      }
    }

    return results;
  }

  /**
   * Internal conversion. This method will allow to save additionnals data.
   * By default, it will save the object as string
   *
   * @param data the data to convert
   * @return a map of String, String
   */
  public Map<String, String> convertDataToStrings(Map<String, Object> data) {
    Map<String, String> results = new HashMap<>();

    if(data != null) {
      for(String key : data.keySet()) {
        Object object = data.get(key);

        // Extract the data that will be saved.
//        if(object instanceof WebAuthenticationDetails) {
        // TODO extract authentication info
//          WebAuthenticationDetails authenticationDetails = (WebAuthenticationDetails) object;
//          results.put("remoteAddress", authenticationDetails.getRemoteAddress());
//          results.put("sessionId", authenticationDetails.getSessionId());
//        } else {
        results.put(key, object.toString());
//        }
      }
    }

    return results;
  }
}
