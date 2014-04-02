package org.obiba.mica.web.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.obiba.mica.jpa.domain.Authority;
import org.obiba.mica.jpa.domain.PersistentToken;
import org.obiba.mica.jpa.domain.User;
import org.obiba.mica.jpa.repository.PersistentTokenRepository;
import org.obiba.mica.jpa.repository.UserRepository;
import org.obiba.mica.security.SecurityUtils;
import org.obiba.mica.service.UserService;
import org.obiba.mica.web.rest.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST controller for managing the current user's account.
 */
@Component
public class AccountResource {

  private static final Logger log = LoggerFactory.getLogger(AccountResource.class);

  @Inject
  private UserRepository userRepository;

  @Inject
  private UserService userService;

  @Inject
  private PersistentTokenRepository persistentTokenRepository;

  /**
   * GET  /rest/authenticate -> check if the user is authenticated, and return its login.
   */
  @GET
  @Path("/authenticate")
  @Timed
  public String isAuthenticated(@Context HttpServletRequest request) {
    log.info(">> REST request to check if the current user is authenticated: {}", request.getRemoteUser());
    return request.getRemoteUser();
  }

  /**
   * GET  /rest/account -> get the current user.
   */
  @GET
  @Path("/account")
  @Produces(APPLICATION_JSON)
  @Timed
  public UserDTO getAccount() {
    log.info(">> get the current user");
    User user = userService.getUserWithAuthorities();
    List<String> roles = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toList());
    UserDTO userDTO = new UserDTO(user.getLogin(), user.getFirstName(), user.getLastName(), user.getEmail(), roles);
    log.info(">> current userDTO: {}", userDTO);
    return userDTO;
  }

  /**
   * POST  /rest/account -> update the current user information.
   */
  @POST
  @Path("/account")
  @Timed
  public Response saveAccount(UserDTO userDTO) throws IOException {
    userService.updateUserInformation(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail());
    return Response.noContent().build();
  }

  /**
   * POST  /rest/change_password -> changes the current user's password
   */
  @POST
  @Path("/account/change_password")
  @Timed
  public Response changePassword(String password) throws IOException {
    if(password == null || "".equals(password)) {
      return Response.status(Response.Status.FORBIDDEN).entity("Password should not be empty").build();
    }
    userService.changePassword(password);
    return Response.noContent().build();
  }

  /**
   * GET  /rest/account/sessions -> get the current open sessions.
   */
  @GET
  @Path("/account/sessions")
  @Produces(APPLICATION_JSON)
  @Timed
  public List<PersistentToken> getCurrentSessions() {
    User user = userRepository.findOne(SecurityUtils.getCurrentLogin());
    return persistentTokenRepository.findByUser(user);
  }

  /**
   * DELETE  /rest/account/sessions?series={series} -> invalidate an existing session.
   */
  @DELETE
  @Path("/account/sessions/{series}")
  @Timed
  public Response invalidateSession(@PathParam("series") String series, @Context HttpServletRequest request)
      throws UnsupportedEncodingException {
    String decodedSeries = URLDecoder.decode(series, "UTF-8");

    // Check if the session to invalidate if the current user session.
    // If so, the security session will be invalidated too
    User user = userRepository.findOne(SecurityUtils.getCurrentLogin());
    List<PersistentToken> persistentTokens = persistentTokenRepository.findByUser(user);

    persistentTokens.stream().filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), decodedSeries))
        .forEach(persistentToken -> {
          request.getSession().invalidate();
          SecurityContextHolder.clearContext();
        });

    persistentTokenRepository.delete(decodedSeries);
    return Response.noContent().build();
  }
}
