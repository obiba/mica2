package org.obiba.mica.security;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.obiba.mica.jpa.domain.Authority;
import org.obiba.mica.jpa.domain.User;
import org.obiba.mica.jpa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UserDetailsService.class);

  @Inject
  private UserRepository userRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String login) {
    log.debug("Authenticating {}", login);
    String lowercaseLogin = login.toLowerCase();

    User userFromDatabase = userRepository.findOne(login);
    if(userFromDatabase == null) {
      throw new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database");
    }

    Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    for(Authority authority : userFromDatabase.getAuthorities()) {
      GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority.getName());
      grantedAuthorities.add(grantedAuthority);
    }

    return new org.springframework.security.core.userdetails.User(lowercaseLogin, userFromDatabase.getPassword(),
        grantedAuthorities);
  }
}
