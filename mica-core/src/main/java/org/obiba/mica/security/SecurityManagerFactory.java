/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.mica.security;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import net.sf.ehcache.CacheManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.PermissionResolverAware;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.RolePermissionResolverAware;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.util.LifecycleUtils;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.obiba.shiro.SessionStorageEvaluator;
import org.obiba.shiro.realm.ObibaRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityManagerFactory implements FactoryBean<SessionsSecurityManager> {

  public static final String INI_REALM = "mica-ini-realm";

  private static final long SESSION_VALIDATION_INTERVAL = 300000l; // 5 minutes

  private static final Logger log = LoggerFactory.getLogger(SecurityManagerFactory.class);

  private final Environment environment;

  private final Set<Realm> realms;

  private final RolePermissionResolver rolePermissionResolver;

  private final PermissionResolver permissionResolver;

  private final CacheManager cacheManager;

  private SessionsSecurityManager securityManager;

  @Inject
  public SecurityManagerFactory(
    Environment environment,
    Set<Realm> realms,
    RolePermissionResolver rolePermissionResolver,
    PermissionResolver permissionResolver,
    CacheManager cacheManager) {
    this.environment = environment;
    this.realms = realms;
    this.rolePermissionResolver = rolePermissionResolver;
    this.permissionResolver = permissionResolver;
    this.cacheManager = cacheManager;
  }

  @Override
  public SessionsSecurityManager getObject() throws Exception {
    if(securityManager == null) {
      securityManager = doCreateSecurityManager();
      SecurityUtils.setSecurityManager(securityManager);
    }
    return securityManager;
  }

  @Override
  public Class<?> getObjectType() {
    return SessionsSecurityManager.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @PreDestroy
  public void destroySecurityManager() {
    log.debug("Shutdown SecurityManager");
    // Destroy the security manager.
    SecurityUtils.setSecurityManager(null);
    LifecycleUtils.destroy(securityManager);
    securityManager = null;
  }

  private SessionsSecurityManager doCreateSecurityManager() {

    ImmutableList.Builder<Realm> builder = ImmutableList.<Realm>builder().add(micaIniRealm());

    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "agate.");
    String obibaRealmUrl = propertyResolver.getProperty("url");
    String serviceName = propertyResolver.getProperty("application.name");
    String serviceKey = propertyResolver.getProperty("application.key");

    if(!Strings.isNullOrEmpty(obibaRealmUrl)) {
      builder.add(obibaRealm(obibaRealmUrl, serviceName, serviceKey));
    }

    builder.addAll(realms);

    DefaultWebSecurityManager manager = new DefaultWebSecurityManager(builder.build());

    initializeCacheManager(manager);
    initializeSessionManager(manager);
    initializeSubjectDAO(manager);
    initializeAuthorizer(manager);
    initializeAuthenticator(manager);

    return manager;
  }

  private Realm micaIniRealm() {
    IniRealm iniRealm = new IniRealm("classpath:shiro.ini");
    iniRealm.setName(INI_REALM);
    iniRealm.setRolePermissionResolver(rolePermissionResolver);
    iniRealm.setPermissionResolver(permissionResolver);
    iniRealm.setCredentialsMatcher(new PasswordMatcher());

    return iniRealm;
  }

  private Realm obibaRealm(String obibaRealmUrl, String serviceName, String serviceKey) {
    ObibaRealm oRealm = new ObibaRealm();
    oRealm.setRolePermissionResolver(rolePermissionResolver);
    oRealm.setBaseUrl(obibaRealmUrl);
    oRealm.setServiceName(serviceName);
    oRealm.setServiceKey(serviceKey);
    // Note: authentication caching is not enabled because it makes the SSO fail

    return oRealm;
  }

  private void initializeCacheManager(DefaultWebSecurityManager dsm) {
    if(dsm.getCacheManager() == null) {
      EhCacheManager ehCacheManager = new EhCacheManager();
      ehCacheManager.setCacheManager(cacheManager);
      dsm.setCacheManager(ehCacheManager);
    }
  }

  private void initializeSessionManager(DefaultWebSecurityManager dsm) {
    DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
    sessionManager.setSessionDAO(new EnterpriseCacheSessionDAO());
    sessionManager.setSessionValidationInterval(SESSION_VALIDATION_INTERVAL);
    sessionManager.setSessionValidationSchedulerEnabled(true);
    dsm.setSessionManager(sessionManager);
  }

  private void initializeSubjectDAO(DefaultWebSecurityManager dsm) {
    if(dsm.getSubjectDAO() instanceof DefaultSubjectDAO) {
      ((DefaultSubjectDAO) dsm.getSubjectDAO()).setSessionStorageEvaluator(new SessionStorageEvaluator());
    }
  }

  private void initializeAuthorizer(DefaultWebSecurityManager dsm) {
    if(dsm.getAuthorizer() instanceof ModularRealmAuthorizer) {
      ((RolePermissionResolverAware) dsm.getAuthorizer()).setRolePermissionResolver(rolePermissionResolver);
      ((PermissionResolverAware) dsm.getAuthorizer()).setPermissionResolver(permissionResolver);
    }
  }

  private void initializeAuthenticator(DefaultWebSecurityManager dsm) {
    if(dsm.getAuthenticator() instanceof ModularRealmAuthenticator) {
      ((ModularRealmAuthenticator) dsm.getAuthenticator()).setAuthenticationStrategy(new FirstSuccessfulStrategy());
    }
  }
}
