package org.obiba.mica.web.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import org.joda.time.LocalDateTime;
import org.obiba.mica.security.AuthoritiesConstants;
import org.obiba.mica.service.AuditEventService;
import org.obiba.mica.web.propertyeditors.LocaleDateTimeEditor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for getting the audit events.
 */
@RestController
@RequestMapping("/app")
public class AuditResource {

  @Inject
  private AuditEventService auditEventService;

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(LocalDateTime.class, new LocaleDateTimeEditor("yyyy-MM-dd", false));
  }

  @RequestMapping(value = "/rest/audits/all",
      method = RequestMethod.GET,
      produces = "application/json")
  @RolesAllowed(AuthoritiesConstants.ADMIN)
  public List<AuditEvent> findAll() {
    return auditEventService.findAll();
  }

  @RequestMapping(value = "/rest/audits/byDates",
      method = RequestMethod.GET,
      produces = "application/json")
  @RolesAllowed(AuthoritiesConstants.ADMIN)
  public List<AuditEvent> findByDates(@RequestParam(value = "fromDate") LocalDateTime fromDate,
      @RequestParam(value = "toDate") LocalDateTime toDate) {
    return auditEventService.findByDates(fromDate, toDate);
  }
}
