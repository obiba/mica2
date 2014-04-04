package org.obiba.mica.service;

import java.util.List;

import javax.inject.Inject;

import org.apache.shiro.SecurityUtils;
import org.joda.time.LocalDate;
import org.obiba.mica.jpa.domain.PersistentToken;
import org.obiba.mica.jpa.domain.User;
import org.obiba.mica.jpa.repository.PersistentTokenRepository;
import org.obiba.mica.jpa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  @Inject
  private UserRepository userRepository;

  @Inject
  private PersistentTokenRepository persistentTokenRepository;

  public void updateUserInformation(String firstName, String lastName, String email) {
    User currentUser = userRepository.findOne(SecurityUtils.getSubject().getPrincipal().toString());
    currentUser.setFirstName(firstName);
    currentUser.setLastName(lastName);
    currentUser.setEmail(email);
    userRepository.save(currentUser);
    log.debug("Changed Information for User: {}", currentUser);
  }

  @Transactional(readOnly = true)
  public User getUserWithAuthorities() {
    User currentUser = userRepository.findOne(SecurityUtils.getSubject().getPrincipal().toString());
    currentUser.getAuthorities().size(); // eagerly load the association
    return currentUser;
  }

  /**
   * Persistent Token are used for providing automatic authentication, they should be automatically deleted after
   * 30 days.
   * <p/>
   * <p>
   * This is scheduled to get fired everyday, at midnight.
   * </p>
   */
  @Scheduled(cron = "0 0 0 * * ?")
  public void removeOldPersistentTokens() {
    LocalDate now = new LocalDate();
    List<PersistentToken> tokens = persistentTokenRepository.findByTokenDateBefore(now.minusMonths(1));
    for(PersistentToken token : tokens) {
      log.debug("Deleting token {}", token.getSeries());
      User user = token.getUser();
      user.getPersistentTokens().remove(token);
      persistentTokenRepository.delete(token);
    }
  }
}
