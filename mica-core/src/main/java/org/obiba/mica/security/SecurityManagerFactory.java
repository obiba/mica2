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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import net.sf.ehcache.CacheManager;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.PermissionResolverAware;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.RolePermissionResolverAware;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.ExecutorServiceSessionValidationScheduler;
import org.apache.shiro.session.mgt.SessionValidationScheduler;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.util.LifecycleUtils;
import org.obiba.shiro.SessionStorageEvaluator;
import org.obiba.shiro.realm.ObibaRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import static org.obiba.mica.security.AuthoritiesConstants.ADMIN;

@Component
public class SecurityManagerFactory implements FactoryBean<SessionsSecurityManager> {

  public static final String INI_REALM = "mica-ini-realm";

  private static final long SESSION_VALIDATION_INTERVAL = 300000l; // 5 minutes

  private static final Logger log = LoggerFactory.getLogger(SecurityManagerFactory.class);

  @Inject
  private Environment env;

  @Inject
  private Set<Realm> realms;

//  @Inject
//  private Set<SessionListener> sessionListeners;

//  @Inject
//  private Set<AuthenticationListener> authenticationListeners;

  @Inject
  private RolePermissionResolver rolePermissionResolver;

  @Inject
  private PermissionResolver permissionResolver;

  @Inject
  private CacheManager cacheManager;

  private SessionsSecurityManager securityManager;

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
    return (SessionsSecurityManager) new CustomIniSecurityManagerFactory(getShiroIniPath()).createInstance();
  }

  private String getShiroIniPath() {
    try {
      return new DefaultResourceLoader().getResource("classpath:/shiro.ini").getFile().getAbsolutePath();
    } catch(IOException e) {
      throw new RuntimeException("Cannot load shiro.ini", e);
    }
  }

  private class CustomIniSecurityManagerFactory extends IniSecurityManagerFactory {

    private CustomIniSecurityManagerFactory(String resourcePath) {
      super(resourcePath);
    }

    @Override
    @SuppressWarnings("ChainOfInstanceofChecks")
    protected SecurityManager createDefaultInstance() {
      DefaultSecurityManager dsm = (DefaultSecurityManager) super.createDefaultInstance();

      initializeCacheManager(dsm);
      initializeSessionManager(dsm);
      initializeSubjectDAO(dsm);
      initializeAuthorizer(dsm);
      initializeAuthenticator(dsm);

      return dsm;
    }

    private void initializeCacheManager(DefaultSecurityManager dsm) {
      if(dsm.getCacheManager() == null) {
        EhCacheManager ehCacheManager = new EhCacheManager();
        ehCacheManager.setCacheManager(cacheManager);
        dsm.setCacheManager(ehCacheManager);
      }
    }

    private void initializeSessionManager(DefaultSecurityManager dsm) {
      if(dsm.getSessionManager() instanceof DefaultSessionManager) {
        DefaultSessionManager sessionManager = (DefaultSessionManager) dsm.getSessionManager();
//        sessionManager.setSessionListeners(sessionListeners);
        sessionManager.setSessionDAO(new EnterpriseCacheSessionDAO());
        SessionValidationScheduler sessionValidationScheduler = new ExecutorServiceSessionValidationScheduler();
        sessionValidationScheduler.enableSessionValidation();
        sessionManager.setSessionValidationScheduler(sessionValidationScheduler);
        sessionManager.setSessionValidationInterval(SESSION_VALIDATION_INTERVAL);
      }
    }

    private void initializeSubjectDAO(DefaultSecurityManager dsm) {
      if(dsm.getSubjectDAO() instanceof DefaultSubjectDAO) {
        ((DefaultSubjectDAO) dsm.getSubjectDAO()).setSessionStorageEvaluator(new SessionStorageEvaluator());
      }
    }

    private void initializeAuthorizer(DefaultSecurityManager dsm) {
      if(dsm.getAuthorizer() instanceof ModularRealmAuthorizer) {
        ((RolePermissionResolverAware) dsm.getAuthorizer()).setRolePermissionResolver(rolePermissionResolver);
        ((PermissionResolverAware) dsm.getAuthorizer()).setPermissionResolver(permissionResolver);
      }
    }

    private void initializeAuthenticator(DefaultSecurityManager dsm) {
      //((AbstractAuthenticator) dsm.getAuthenticator()).setAuthenticationListeners(authenticationListeners);

      if(dsm.getAuthenticator() instanceof ModularRealmAuthenticator) {
        ((ModularRealmAuthenticator) dsm.getAuthenticator()).setAuthenticationStrategy(new FirstSuccessfulStrategy());
      }
    }

    @Override
    protected void applyRealmsToSecurityManager(Collection<Realm> shiroRealms, @SuppressWarnings(
        "ParameterHidesMemberVariable") SecurityManager securityManager) {
      ImmutableList.Builder<Realm> builder = ImmutableList.<Realm>builder().addAll(shiroRealms).addAll(realms);
      RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "agate.");
      String obibaRealmUrl = propertyResolver.getProperty("url");
      String serviceName = propertyResolver.getProperty("application.name");
      String serviceKey = propertyResolver.getProperty("application.key");

      if(!Strings.isNullOrEmpty(obibaRealmUrl)) {
        ObibaRealm oRealm = new ObibaRealm();
        oRealm.setRolePermissionResolver(rolePermissionResolver);
        oRealm.setBaseUrl(obibaRealmUrl);
        oRealm.setServiceName(serviceName);
        oRealm.setServiceKey(serviceKey);
        // Note: authentication caching is not enabled because it makes the SSO fail

        builder.add(oRealm);
      }
      super.applyRealmsToSecurityManager(builder.build(), securityManager);
    }

    @Override
    protected Realm createRealm(Ini ini) {
      // Set the resolvers first, because IniRealm is initialized before the resolvers
      // are applied by the ModularRealmAuthorizer
      IniRealm realm = new IniRealm();
      realm.setName(INI_REALM);
      realm.setRolePermissionResolver(rolePermissionResolver);
      realm.setPermissionResolver(permissionResolver);
      realm.setResourcePath(getShiroIniPath());
      realm.setCredentialsMatcher(new PasswordMatcher());
      realm.setIni(ini);
      return realm;
    }
  }
}
