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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Semaphore;

@Configuration
@EnableAsync
@EnableScheduling
public class EsConcurrencyConfiguration implements EnvironmentAware {

  private static final int DEFAULT_MAX_CONCURRENT_MAX_JOIN_QUERIES = 4;

  private RelaxedPropertyResolver propertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    propertyResolver = new RelaxedPropertyResolver(environment, "elasticsearch.");
  }

  @Bean(name = "esJoinQueriesSemaphore")
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public Semaphore getSemaphore() {
    return new Semaphore(propertyResolver.getProperty("maxConcurrentJoinQueries", Integer.class, DEFAULT_MAX_CONCURRENT_MAX_JOIN_QUERIES));
  }
}
