package org.obiba.mica.config;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Configuration
public class EventBusConfiguration {

  @Inject
  private EventBus eventBus;

  @Bean
  public EventBus eventBus() {
    return new EventBus();
  }

  @Bean
  public EventBusPostProcessor eventBusPostProcessor() {
    return new EventBusPostProcessor();
  }

  private class EventBusPostProcessor implements BeanPostProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      // for each method in the bean
      for(Method method : bean.getClass().getMethods()) {
        if(method.isAnnotationPresent(Subscribe.class)) {
          // register it with the event bus
          eventBus.register(bean);
          log.trace("Bean {} () containing method {} was subscribed to {}", beanName, bean.getClass().getName(),
              method.getName(), EventBus.class.getCanonicalName());
          // we only need to register once
          return bean;
        }
      }

      return bean;
    }

  }

}
