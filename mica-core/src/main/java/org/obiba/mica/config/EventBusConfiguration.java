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

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Configuration
public class EventBusConfiguration {

  // defined in AsyncConfiguration
  @Inject
  @Qualifier("applicationTaskExecutor")
  private Executor executor;

  @Bean
  public EventBus eventBus() {
    return new AsyncEventBus(executor);
  }

  @Bean
  public BeanPostProcessor eventBusPostProcessor() {
    return new EventBusSubscriberPostProcessor();
  }

  private static class EventBusSubscriberPostProcessor implements BeanPostProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private EventBus eventBus;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      // need the target class, not the Spring proxied one
      Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
      // for each method in the bean
      for(Method method : targetClass.getMethods()) {
        if(method.isAnnotationPresent(Subscribe.class)) {
          log.debug("Register bean {} ({}) containing method {} to EventBus", beanName, bean.getClass().getName(),
              method.getName());
          // register it with the event bus
          eventBus.register(bean);
          return bean; // we only need to register once
        }
      }
      return bean;
    }
  }

}
