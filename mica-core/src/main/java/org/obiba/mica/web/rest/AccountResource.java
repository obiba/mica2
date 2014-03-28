package org.obiba.mica.web.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/app")
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
  @RequestMapping(value = "/rest/authenticate",
      method = RequestMethod.GET,
      produces = "application/json")
  @Timed
  public String isAuthenticated(HttpServletRequest request) {
    log.debug("REST request to check if the current user is authenticated");
    return request.getRemoteUser();
  }

  /**
   * GET  /rest/account -> get the current user.
   */
  @RequestMapping(value = "/rest/account",
      method = RequestMethod.GET,
      produces = "application/json")
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
  @RequestMapping(value = "/rest/account",
      method = RequestMethod.POST,
      produces = "application/json")
  @Timed
  public void saveAccount(@RequestBody UserDTO userDTO) throws IOException {
    userService.updateUserInformation(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail());
  }

  /**
   * POST  /rest/change_password -> changes the current user's password
   */
  @RequestMapping(value = "/rest/account/change_password",
      method = RequestMethod.POST,
      produces = "application/json")
  @Timed
  public void changePassword(@RequestBody String password, HttpServletResponse response) throws IOException {
    if(password == null || "".equals(password)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Password should not be empty");
    } else {
      userService.changePassword(password);
    }
  }

  /**
   * GET  /rest/account/sessions -> get the current open sessions.
   */
  @RequestMapping(value = "/rest/account/sessions",
      method = RequestMethod.GET,
      produces = "application/json")
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
  @RequestMapping(value = "/rest/account/sessions/{series}",
      method = RequestMethod.DELETE)
  @Timed
  public void invalidateSession(@PathVariable String series, HttpServletRequest request)
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
