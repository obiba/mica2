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

import java.util.concurrent.Executor;
import org.obiba.mica.core.MicaAsyncTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer, EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

  private static final int DEFAULT_QUEUE_CAPACITY = 10_000;

  private static final int DEFAULT_POOL_SIZE = 10;

  private RelaxedPropertyResolver propertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "async.");
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SimpleAsyncUncaughtExceptionHandler();
  }

  @Override
  @Bean
  public Executor getAsyncExecutor() {

    Integer poolSize = propertyResolver.getProperty("poolSize", Integer.class, DEFAULT_POOL_SIZE);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    executor.setQueueCapacity(propertyResolver.getProperty("queueCapacity", Integer.class, DEFAULT_QUEUE_CAPACITY));
    executor.setThreadNamePrefix("mica-executor-");
    return new MicaAsyncTaskExecutor(executor);
  }

  @Bean(name="opalExecutor")
  public Executor getOpalAsyncExecutor() {
    log.debug("Creating Async Task Executor");

    Integer poolSize = propertyResolver.getProperty("opal.poolSize", Integer.class, DEFAULT_POOL_SIZE);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    executor.setQueueCapacity(propertyResolver.getProperty("opal.queueCapacity", Integer.class, DEFAULT_QUEUE_CAPACITY));
    executor.setThreadNamePrefix("mica-opal-executor-");
    return new MicaAsyncTaskExecutor(executor);
  }
}
