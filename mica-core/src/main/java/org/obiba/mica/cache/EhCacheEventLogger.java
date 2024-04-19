package org.obiba.mica.cache;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EhCacheEventLogger implements CacheEventListener<Object, Object> {

  private static final Logger logger = LoggerFactory.getLogger(EhCacheEventLogger.class);

  @Override
  public void onEvent(CacheEvent cacheEvent) {
    logger.info("Cache event = {}, Key = {},  Old value = {}, New value = {}", cacheEvent.getType(),
      cacheEvent.getKey(), cacheEvent.getOldValue(), cacheEvent.getNewValue());
  }
}
