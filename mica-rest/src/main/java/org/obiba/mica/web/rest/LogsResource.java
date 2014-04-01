package org.obiba.mica.web.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.obiba.mica.web.rest.dto.LoggerDTO;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * Controller for view and managing Log Level at runtime.
 */
@Component
@Path("/logs")
public class LogsResource {

  @GET
  @Timed
  public List<LoggerDTO> getList() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    return context.getLoggerList().stream().map(LoggerDTO::new).collect(Collectors.toList());
  }

  @PUT
  @Timed
  public HttpStatus changeLevel(LoggerDTO jsonLogger) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.getLogger(jsonLogger.getName()).setLevel(Level.valueOf(jsonLogger.getLevel()));
    return HttpStatus.NO_CONTENT;
  }
}
