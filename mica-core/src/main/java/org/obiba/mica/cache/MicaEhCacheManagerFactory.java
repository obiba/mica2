package org.obiba.mica.cache;

import org.apache.shiro.cache.CacheException;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.ehcache.EhCacheCacheManager;

import java.net.MalformedURLException;
import java.net.URL;

public class MicaEhCacheManagerFactory {
  private static final Logger log = LoggerFactory.getLogger(MicaEhCacheManagerFactory.class);


  private static final String EHCACHE_CONFIG_FILE = "ehcache.xml";

  // Implement a EhCache 3 CacheManager to be used in Spring boot 3 application

  public CacheManager create() {
    // How to use Ehcache 3 in Spring boot 3
    // https://www.ehcache.org/documentation/3.8/getting-started.html

    // https://www.ehcache.org/documentation/3.8/getting-started.html



    log.info("Creating EhCache Manager");
    try {
      XmlConfiguration xmlConfiguration = new XmlConfiguration(new URL(EHCACHE_CONFIG_FILE));
      CacheManager cacheManager = CacheManagerBuilder.newCacheManager(xmlConfiguration);
      cacheManager.init();

      return cacheManager;
    } catch (MalformedURLException e) {
      throw new CacheException(e);
    }
  }
}
