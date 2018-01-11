/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.obiba.mica.web.rest.dto.LoggerDTO;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.codahale.metrics.annotation.Timed;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Controller for view and managing Log Level at runtime.
 */
@Path("/logs")
public class LogsResource {

  @GET
  @Produces(APPLICATION_JSON)
  @Timed
  public List<LoggerDTO> getList() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    return context.getLoggerList().stream().map(LoggerDTO::new).collect(Collectors.toList());
  }

  @PUT
  @Timed
  public HttpStatus changeLevel(LoggerDTO jsonLogger) {
    @SuppressWarnings("TypeMayBeWeakened")
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.getLogger(jsonLogger.getName()).setLevel(Level.valueOf(jsonLogger.getLevel()));
    return HttpStatus.NO_CONTENT;
  }
}
