package org.obiba.mica.web.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
  public String isAuthenticated(HttpServletRequest request) {
    log.debug("REST request to check if the current user is authenticated");
    return request.getRemoteUser();
  }

  /**
   * GET  /rest/account -> get the current user.
   */
  @GET
  @Path("/account")
  @Timed
  public UserDTO getAccount(HttpServletResponse response) {
    User user = userService.getUserWithAuthorities();
    if(user == null) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return null;
    }
    List<String> roles = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toList());
    return new UserDTO(user.getLogin(), user.getFirstName(), user.getLastName(), user.getEmail(), roles);
  }

  /**
   * POST  /rest/account -> update the current user information.
   */
  @POST
  @Path("/account")
  @Timed
  public void saveAccount(UserDTO userDTO) throws IOException {
    userService.updateUserInformation(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail());
  }

  /**
   * POST  /rest/change_password -> changes the current user's password
   */
  @POST
  @Path("/account/change_password")
  @Timed
  public void changePassword(String password, HttpServletResponse response) throws IOException {
    if(password == null || "".equals(password)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Password should not be empty");
    } else {
      userService.changePassword(password);
    }
  }

  /**
   * GET  /rest/account/sessions -> get the current open sessions.
   */
  @GET
  @Path("/account/sessions")
  @Timed
  public List<PersistentToken> getCurrentSessions(HttpServletResponse response) {
    User user = userRepository.findOne(SecurityUtils.getCurrentLogin());
    if(user == null) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    return persistentTokenRepository.findByUser(user);
  }

  /**
   * DELETE  /rest/account/sessions?series={series} -> invalidate an existing session.
   */
  @DELETE
  @Path("/account/sessions/{series}")
  @Timed
  public void invalidateSession(@PathParam("series") String series, HttpServletRequest request)
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
  }
}
