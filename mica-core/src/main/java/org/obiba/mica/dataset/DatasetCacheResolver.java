package org.obiba.mica.dataset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;

import org.obiba.mica.dataset.domain.Dataset;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component("datasetVariablesCacheResolver")
public class DatasetCacheResolver implements CacheResolver{

  @Inject
  private net.sf.ehcache.CacheManager cacheManager;

  @Inject
  private CacheManager springCacheManager;

  @Override
  public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> cacheOperationInvocationContext) {
    Collection<Cache> res = Lists.newArrayList();

    Optional<Object> dataset = Arrays.asList(cacheOperationInvocationContext.getArgs()).stream()
      .filter(o -> o instanceof Dataset).findFirst();

    if(dataset.isPresent()) {
      String cacheName = "dataset-" + ((Dataset) dataset.get()).getId();
      Cache datasetCache = springCacheManager.getCache(cacheName);

      if (datasetCache == null) {
        CacheConfiguration conf = cacheManager.getCache("dataset-variables").getCacheConfiguration().clone();
        conf.setName(cacheName);
        cacheManager.addCache(new net.sf.ehcache.Cache(conf));
        datasetCache = new EhCacheCache(cacheManager.getCache(cacheName));
      }

      res.add(datasetCache);
    }

    return res;
  }
}
