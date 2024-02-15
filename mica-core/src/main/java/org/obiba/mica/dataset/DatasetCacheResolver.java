/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.ehcache.config.CacheConfiguration;

import org.obiba.mica.dataset.domain.Dataset;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ehcache.InstrumentedEhcache;
import com.google.common.collect.Lists;

@Component("datasetVariablesCacheResolver")
public class DatasetCacheResolver implements CacheResolver {

  @Inject
  private org.ehcache.CacheManager cacheManager;

  @Inject
  private MetricRegistry metricRegistry;

  @Inject
  private CacheManager springCacheManager;

  @Override
  public synchronized Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> cacheOperationInvocationContext) {
    Collection<Cache> res = Lists.newArrayList();

    Optional<Object> dataset = Arrays.stream(cacheOperationInvocationContext.getArgs())
      .filter(o -> o instanceof Dataset)
      .findFirst();

    if(dataset.isPresent()) {
      String cacheName = "dataset-" + ((Dataset) dataset.get()).getId();
      Cache datasetCache = springCacheManager.getCache(cacheName);

      if (datasetCache == null) {
        CacheConfiguration conf = cacheManager.getEhcache("dataset-variables").getCacheConfiguration().clone();
        conf.setName(cacheName);
        cacheManager.addCache(new org.ehcache.Cache(conf));
        org.ehcache.Cache cache = cacheManager.getCache(cacheName);
        cacheManager.replaceCacheWithDecoratedCache(cache, InstrumentedEhcache.instrument(metricRegistry, cache));
        datasetCache = new EhCacheCache(cacheManager.getEhcache(cacheName));
      }

      res.add(datasetCache);
    }

    return res;
  }
}
