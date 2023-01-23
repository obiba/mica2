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

import javax.inject.Inject;

import org.obiba.mica.config.locale.AngularCookieLocaleResolver;
import org.obiba.mica.config.locale.ExtendedResourceBundleMessageSource;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.google.common.eventbus.Subscribe;

@Configuration
public class LocaleConfiguration extends WebMvcConfigurerAdapter implements EnvironmentAware {

  private Environment environment;

  private final MicaConfigService micaConfigService;

  private ExtendedResourceBundleMessageSource messageSource;

  @Inject
  public LocaleConfiguration(MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Bean(name = "localeResolver")
  public LocaleResolver localeResolver() {
    AngularCookieLocaleResolver cookieLocaleResolver = new AngularCookieLocaleResolver(micaConfigService);
    cookieLocaleResolver.setCookieName("NG_TRANSLATE_LANG_KEY");
    return cookieLocaleResolver;
  }

  @Bean
  public MessageSource messageSource() {
    int cacheSeconds = environment.getProperty("spring.messageSource.cacheSeconds", Integer.class, 60);
    messageSource = new ExtendedResourceBundleMessageSource(micaConfigService, cacheSeconds);
    messageSource.setBasenames("classpath:/translations/messages", "classpath:/i18n/messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setCacheSeconds(cacheSeconds);
    return messageSource;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    localeChangeInterceptor.setParamName("language");

    registry.addInterceptor(localeChangeInterceptor);
  }

  @Async
  @Subscribe
  public void micaConfigUpdated(MicaConfigUpdatedEvent event) {
    if (messageSource != null)
      messageSource.evict();
  }
}

