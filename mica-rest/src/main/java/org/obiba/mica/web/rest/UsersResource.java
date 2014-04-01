package org.obiba.mica.web.rest;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.mica.jpa.domain.User;
import org.obiba.mica.jpa.repository.UserRepository;
import org.obiba.mica.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

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
  @RolesAllowed(AuthoritiesConstants.ADMIN)
  @Timed
  public User getUser(@PathParam("login") String login, HttpServletResponse response) {
    log.debug("REST request to get User : {}", login);
    User user = userRepository.findOne(login);
    if(user == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return user;
  }
}
