package org.obiba.mica.config;

import java.util.Set;
import java.util.SortedSet;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ehcache.InstrumentedEhcache;

@Configuration
@EnableCaching
@AutoConfigureAfter(value = { MetricsConfiguration.class, DatabaseConfiguration.class })
public class CacheConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

  @PersistenceContext
  private EntityManager entityManager;

  @Inject
  private Environment env;

  @Inject
  private MetricRegistry metricRegistry;

  private net.sf.ehcache.CacheManager cacheManager;

  @PreDestroy
  public void destroy() {
    log.info("Remove Cache Manager metrics");
    SortedSet<String> names = metricRegistry.getNames();
    names.forEach(metricRegistry::remove);
    log.info("Closing Cache Manager");
    cacheManager.shutdown();
  }

  @Bean
  public CacheManager cacheManager() {
    log.debug("Starting Ehcache");
    cacheManager = net.sf.ehcache.CacheManager.create();
    cacheManager.getConfiguration()
        .setMaxBytesLocalHeap(env.getProperty("cache.ehcache.maxBytesLocalHeap", String.class, "16M"));
    log.debug("Registering Ehcache Metrics gauges");
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
    for(EntityType<?> entity : entities) {

      String name = entity.getName();
      if(name == null) {
        name = entity.getJavaType().getName();
      }
      Assert.notNull(name, "entity cannot exist without a identifier");

      Cache cache = cacheManager.getCache(name);
      if(cache != null) {
        cache.getCacheConfiguration()
            .setTimeToLiveSeconds(env.getProperty("cache.timeToLiveSeconds", Integer.class, 3600));
        Ehcache decoratedCache = InstrumentedEhcache.instrument(metricRegistry, cache);
        cacheManager.replaceCacheWithDecoratedCache(cache, decoratedCache);
      }
    }
    EhCacheCacheManager ehCacheManager = new EhCacheCacheManager();
    ehCacheManager.setCacheManager(cacheManager);
    return ehCacheManager;
  }
}
