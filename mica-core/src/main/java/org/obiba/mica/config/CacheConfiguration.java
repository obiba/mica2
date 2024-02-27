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

import org.springframework.beans.factory.DisposableBean;

//@Configuration("cacheConfiguration")
//@EnableCaching
//@AutoConfigureAfter(value = { MetricsConfiguration.class })
public class CacheConfiguration implements DisposableBean {

//  private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);
//
//  @Inject
//  private MetricRegistry metricRegistry;
//
  @Override
  public void destroy() throws Exception {
//    log.info("Remove Cache Manager metrics");
//    metricRegistry.getNames().forEach(metricRegistry::remove);
  }

//  @Bean
//  public EhCacheManagerFactoryBean cacheManagerFactory() {
//    log.debug("Starting Ehcache");
//    EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
//    factoryBean.setCacheManagerName("mica");
//
//    return factoryBean;
//  }

//  @Bean
//  public CacheManager springCacheManager() {
//    log.debug("Starting Spring Cache");
//    org.ehcache.CacheManager cacheManager = new MicaEhCacheManagerFactory().create();
//
//    return cacheManager;
//  }
}
