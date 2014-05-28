/*******************************************************************************
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.mica.security.realm;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.util.SimpleByteSource;
import org.obiba.mica.domain.Authority;
import org.obiba.mica.domain.User;
import org.obiba.mica.repository.UserRepository;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;

/**
 * Realm for users defined in mica-server's own users database.
 */
//@Component
//@Transactional
public class MicaUserRealm extends AuthorizingRealm {

  public static final String MICA_REALM = "mica-user-realm";

  @Inject
  private UserRepository userRepository;

  @Inject
  private Environment env;

  /**
   * Number of times the user password is hashed for attack resiliency
   */
  private int nbHashIterations;

  private String salt;

  @PostConstruct
  public void postConstruct() {

    setCacheManager(new MemoryConstrainedCacheManager());

    HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher(Sha512Hash.ALGORITHM_NAME);
    credentialsMatcher.setHashIterations(nbHashIterations);
    setCredentialsMatcher(credentialsMatcher);

    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "shiro.password.");
    nbHashIterations = propertyResolver.getProperty("nbHashIterations", Integer.class);
    salt = propertyResolver.getProperty("salt");
  }

  @Override
  public String getName() {
    return MICA_REALM;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;
    String username = upToken.getUsername();

    // Null username is invalid
    if(username == null) {
      throw new AccountException("Null usernames are not allowed by this realm.");
    }

    User user = userRepository.findOne(username);
    if(user == null /*|| !subjectCredentials.isEnabled()*/) {
      throw new UnknownAccountException("No account found for user [" + username + "]");
    }
    SimpleAuthenticationInfo authInfo = new SimpleAuthenticationInfo(username, user.getPassword(), getName());
    authInfo.setCredentialsSalt(new SimpleByteSource(salt));
    return authInfo;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    Collection<?> thisPrincipals = principals.fromRealm(getName());
    if(thisPrincipals != null && !thisPrincipals.isEmpty()) {
      Object primary = thisPrincipals.iterator().next();
      PrincipalCollection simplePrincipals = new SimplePrincipalCollection(primary, getName());
      String username = (String) getAvailablePrincipal(simplePrincipals);
      User user = userRepository.findOne(username);
      return new SimpleAuthorizationInfo(user == null
          ? Collections.emptySet()
          : user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet()));
    }
    return new SimpleAuthorizationInfo();

  }

}
