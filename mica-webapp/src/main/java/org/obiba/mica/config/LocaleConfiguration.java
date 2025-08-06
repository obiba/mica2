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

import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.obiba.mica.config.locale.AngularCookieLocaleResolver;
import org.obiba.mica.config.locale.ExtendedResourceBundleMessageSource;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class LocaleConfiguration implements WebMvcConfigurer, EnvironmentAware {

  private static final Logger logger = LoggerFactory.getLogger(LocaleConfiguration.class);
  private final MicaConfigService micaConfigService;
  private Environment environment;
  private ExtendedResourceBundleMessageSource messageSource;
  private Locale validatedLocale;

  @Inject
  public LocaleConfiguration(MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
    validateAndSetDefaultLocale();
  }

  @Bean(name = "localeResolver")
  public LocaleResolver localeResolver() {
    AngularCookieLocaleResolver cookieLocaleResolver = new AngularCookieLocaleResolver(micaConfigService, validatedLocale);
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

  private void validateAndSetDefaultLocale() {
    String rawDefaultLocale = environment.getProperty("locale.default", "en");
    List<Locale> supported = micaConfigService.getConfig().getLocales();
    validatedLocale = supported.stream()
      .filter(l -> l.equals(Locale.forLanguageTag(rawDefaultLocale)))
      .findFirst()
      .orElseGet(() -> supported.isEmpty() ? Locale.ENGLISH : supported.getFirst());

    if (environment instanceof ConfigurableEnvironment cfgEnv) {
      cfgEnv.getPropertySources().addFirst(
        new MapPropertySource("validatedLocale",
          Map.of("locale.validatedLocale", validatedLocale.toLanguageTag()))
      );
    } else {
      logger.error("Environment is not configurable, could not set validated locale property. Using ENGLISH as default.");
      validatedLocale = Locale.ENGLISH;
    }
  }
}

