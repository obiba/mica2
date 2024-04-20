/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config.locale;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.util.WebUtils;

/**
 * Angular cookie saved the locale with a double quote (%22en%22).
 * So the default CookieLocaleResolver#StringUtils.parseLocaleString(localePart)
 * is not able to parse the locale.
 * <p>
 * This class will check if a double quote has been added, if so it will remove it.
 */
public class AngularCookieLocaleResolver extends CookieLocaleResolver {

  private final MicaConfigService micaConfigService;

  public AngularCookieLocaleResolver(MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  @Override
  protected Locale determineDefaultLocale(HttpServletRequest request) {
    Locale defaultLocale = super.determineDefaultLocale(request);
    // validate default locale, which could come from the Accept-Language header
    List<Locale> configLocales = micaConfigService.getConfig().getLocales();
    List<Locale> languageLocales = configLocales.stream()
      .filter(locale -> locale.getLanguage().equalsIgnoreCase(defaultLocale.getLanguage()))
      .collect(Collectors.toList());
    if (languageLocales.isEmpty())
      return configLocales.stream().findFirst().orElse(Locale.ENGLISH);
    return languageLocales.get(0);
  }

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    parseLocaleCookieIfNecessary(request);
    return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
  }

  @Override
  public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
    parseLocaleCookieIfNecessary(request);
    return new TimeZoneAwareLocaleContext() {
      @Override
      public Locale getLocale() {
        return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
      }

      @Override
      public TimeZone getTimeZone() {
        return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
      }
    };
  }

  @Override
  public void addCookie(HttpServletResponse response, String cookieValue) {
    // Mandatory cookie modification for angular to support the locale switching on the server side.
    super.addCookie(response, "%22" + cookieValue + "%22");
  }

  private void parseLocaleCookieIfNecessary(HttpServletRequest request) {
    if(request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
      // Retrieve and parse cookie value.
      Cookie cookie = WebUtils.getCookie(request, getCookieName());
      Locale locale = null;
      TimeZone timeZone = null;
      if(cookie != null) {
        String value = cookie.getValue();

        // Remove the double quote
        value = StringUtils.replace(value, "%22", "");

        String localePart = value;
        String timeZonePart = null;
        int spaceIndex = localePart.indexOf(' ');
        if(spaceIndex != -1) {
          localePart = value.substring(0, spaceIndex);
          timeZonePart = value.substring(spaceIndex + 1);
        }
        locale = "-".equals(localePart) ? null : StringUtils.parseLocaleString(localePart);
        if(timeZonePart != null) {
          timeZone = StringUtils.parseTimeZoneString(timeZonePart);
        }
        if(logger.isTraceEnabled()) {
          logger.trace("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
              "'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
        }
      }
      request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, locale == null ? determineDefaultLocale(request) : locale);
      request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
          timeZone == null ? determineDefaultTimeZone(request) : timeZone);
    }
  }
}
