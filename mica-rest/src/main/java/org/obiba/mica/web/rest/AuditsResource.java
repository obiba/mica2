/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.obiba.mica.security.AuthoritiesConstants;
import org.obiba.mica.core.service.AuditEventService;
import org.springframework.boot.actuate.audit.AuditEvent;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST controller for getting the audit events.
 */
@Path("/audits")
public class AuditsResource {

  @Inject
  private AuditEventService auditEventService;

  //TODO joda DateTime conversion (ie PropertyEditor in Spring MVC)

  @GET
  @Path("/all")
  @Produces(APPLICATION_JSON)
  @RolesAllowed(AuthoritiesConstants.ADMIN)
  public List<AuditEvent> findAll() {
    return auditEventService.findAll();
  }

//  @GET
//  @Path("/byDates")
//  @RolesAllowed(AuthoritiesConstants.ADMIN)
//  public List<AuditEvent> findByDates(@QueryParam("fromDate") LocalDateTime fromDate,
//      @QueryParam("toDate") LocalDateTime toDate) {
//    return auditEventService.findByDates(fromDate, toDate);
//  }
}
