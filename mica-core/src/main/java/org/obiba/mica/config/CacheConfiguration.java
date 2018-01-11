/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import net.sf.ehcache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ehcache.InstrumentedEhcache;

@Configuration("cacheConfiguration")
@EnableCaching
@AutoConfigureAfter(value = { MetricsConfiguration.class })
public class CacheConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

  @Inject
  private MetricRegistry metricRegistry;

  @PreDestroy
  public void destroy() {
    log.info("Remove Cache Manager metrics");
    metricRegistry.getNames().forEach(metricRegistry::remove);
  }

  @Bean
  public EhCacheManagerFactoryBean cacheManagerFactory() {
    log.debug("Starting Ehcache");
    EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
    factoryBean.setCacheManagerName("mica");

    return factoryBean;
  }

  @Bean
  public CacheManager springCacheManager() {
    log.debug("Starting Spring Cache");
    net.sf.ehcache.CacheManager cacheManager = cacheManagerFactory().getObject();
    EhCacheCacheManager ehCacheManager = new EhCacheCacheManager();
    ehCacheManager.setCacheManager(cacheManager);
    String[] cacheNames = cacheManager.getCacheNames();
    for (String cacheName : cacheNames) {
      Cache cache = cacheManager.getCache(cacheName);
      cacheManager.replaceCacheWithDecoratedCache(cache, InstrumentedEhcache.instrument(metricRegistry, cache));
    }
    return ehCacheManager;
  }
}
