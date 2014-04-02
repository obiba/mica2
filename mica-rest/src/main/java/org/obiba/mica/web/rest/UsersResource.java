package org.obiba.mica.web.rest;

import java.util.NoSuchElementException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.obiba.mica.jpa.domain.User;
import org.obiba.mica.jpa.repository.UserRepository;
import org.obiba.mica.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST controller for managing users.
 */
@Component
@Path("/users")
public class UsersResource {

  private static final Logger log = LoggerFactory.getLogger(UsersResource.class);

  @Inject
  private UserRepository userRepository;

  /**
   * GET  /rest/users/:login -> get the "login" user.
   */
  @GET
  @Path("/{login}")
  @Produces(APPLICATION_JSON)
  @RolesAllowed(AuthoritiesConstants.ADMIN)
  @Timed
  public User getUser(@PathParam("login") String login) {
    log.debug("REST request to get User : {}", login);
    User user = userRepository.findOne(login);
    if(user == null) {
      throw new NoSuchElementException("User " + login + " does not exist");
    }
    return user;
  }
}
