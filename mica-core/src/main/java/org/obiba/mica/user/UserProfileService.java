/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.user;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.obiba.mica.core.service.AgateRestService;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.shiro.realm.ObibaRealm;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserProfileService extends AgateRestService {

  private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

  private static final DateTimeFormatter ISO_8601 = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  private static final String DEFAULT_CONTACT_NOTIFICATION_SUBJECT = "[${organization}] Contact";

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private MailService mailService;

  private Cache<String, Subject> subjectCache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .build();

  public synchronized Subject getProfile(@NotNull String username) {
    Assert.notNull(username, "Username cannot be null");
    Subject subject = getProfileInternal(getProfileServiceUrl(username));

    if (subject == null) {
      // return dummy Subject in case communication with Agate failed
      subject = new Subject();
      subject.setUsername(username);
    }

    return subject;
  }

  public synchronized Subject getProfile(@NotNull String username, boolean cached) {
    if (!cached) return getProfile(username);

    ObibaRealm.Subject profile = subjectCache.getIfPresent(username);
    if (profile == null) {
      profile = getProfile(username);
      subjectCache.put(username, profile);
    }
    return profile;
  }

  public synchronized Map<String, Object> getProfileMap(@NotNull String username, boolean cached) {
    return asMap(getProfile(username, cached));
  }

  public synchronized Subject getProfileByGroup(@NotNull String username, @Nullable String group) {
    Assert.notNull(username, "Username cannot be null");
    return getProfileInternal(getProfileServiceUrlByGroup(username, group));
  }

  public synchronized List<Subject> getProfilesByGroup(@Nullable String group) {
    List<Subject> subjects;
    try {
      String profileServiceUrl = getProfileServiceUrlByGroup(null, group);
      subjects = Arrays.asList(executeQuery(profileServiceUrl, Subject[].class));
      subjects.forEach(s -> subjectCache.put(s.getUsername(), s));
    } catch (RestClientException e) {
      log.error("Agate connection failure: {}", e.getMessage());
      subjects = Lists.newArrayList();
    }

    return subjects;
  }

  public String getUserProfileTranslations(String locale) {

    String serviceUrl = UriComponentsBuilder
      .fromHttpUrl(agateServerConfigService.getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path("/users/i18n/" + locale + ".json")
      .build().toUriString();

    return executeQuery(serviceUrl, String.class);
  }

  public boolean currentUserIs(@NotNull String role) {
    org.apache.shiro.subject.Subject subject = SecurityUtils.getSubject();
    if (subject == null || subject.getPrincipal() == null) {
      return false;
    }

    String username = subject.getPrincipal().toString();

    if (username.equals("administrator")) {
      return true;
    }

    ObibaRealm.Subject profile = getProfile(username);
    return profile != null && profile.getGroups() != null && profile.getGroups().stream().filter(g -> g.equals(role)).count() > 0;
  }

  public void createUser(Map<String, String[]> params) {
    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
    headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

    StringBuffer query = new StringBuffer();
    for (String group : micaConfigService.getConfig().getSignupGroups()) {
      if (query.length() > 0) query.append("&");
      query.append("group=").append(group);
    }
    for (String field : params.keySet()) {
      for (String value : params.get(field)) {
        if (!Strings.isNullOrEmpty(value)) {
          query.append("&");
          try {
            if ("g-recaptcha-response".equals(field))
              query.append("reCaptchaResponse");
            else
              query.append(field);
            query.append("=").append(URLEncoder.encode(value, "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            // ignore
          }
        }
      }
    }

    HttpEntity<String> entity = new HttpEntity<>(query.toString(), headers);
    try {
      ResponseEntity<String> response = template.exchange(getUsersJoinUrl(), HttpMethod.POST, entity, String.class);
      log.info(response.getHeaders().getLocation().toString());
    } catch (HttpClientErrorException e) {
      String message = e.getResponseBodyAsString();
      log.error("Client error on user creation: {}", message);
      throw e;
    } catch (HttpServerErrorException e) {
      String message = e.getResponseBodyAsString();
      log.error("Server error on user creation: {}", message);
      throw e;
    }
  }

  public void resetPassword(String username) {
    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

    String query = "username=";
    try {
      query = query + URLEncoder.encode(username, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // ignore
    }
    HttpEntity<String> entity = new HttpEntity<>(query, headers);
    ResponseEntity<String> response = template.exchange(getUsersForgotPasswordUrl(), HttpMethod.POST, entity, String.class);
  }

  public Map<String, Object> asMap(@NotNull Subject profile) {
    Map<String, Object> params = Maps.newHashMap();
    String fullName = profile.getUsername();
    Map<String, Object> attributes = Maps.newHashMap();
    if (profile.getAttributes() != null) {
      profile.getAttributes().forEach(attr -> {
        String key = attr.get("key");
        Object value;
        if ("lastLogin".equals(key) || "createdDate".equals(key)) {
          try {
            attributes.put(key, ISO_8601.parseDateTime(attr.get("value")));
          } catch (Exception e) {
            // ignore
          }
        }
        else
          attributes.put(key, attr.get("value"));
      });
      fullName = attributes.getOrDefault("firstName", "") + (attributes.containsKey("firstName") ? " " : "") + attributes.getOrDefault("lastName", profile.getUsername());
    }
    params.put("username", profile.getUsername());
    params.put("fullName", fullName);
    params.put("groups", profile.getGroups());
    params.put("attributes", attributes);
    return params;
  }

  public void sendContactEmail(String name, String email, String subject, String message, String reCaptcha) {
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isContactNotificationsEnabled()) return;

    Map<String, String> ctx = Maps.newHashMap();
    ctx.put("organization", config.getName());
    ctx.put("publicUrl", micaConfigService.getPublicUrl());
    ctx.put("contactName", name);
    ctx.put("contactEmail", email);
    ctx.put("contactSubject", subject);
    ctx.put("contactMessage", message);
    ctx.put("reCaptcha", reCaptcha);

    List<String> contactGroups = config.getContactGroups();
    String[] groups = new String[contactGroups.size()];
    contactGroups.toArray(groups);
    mailService.sendEmailToGroups(mailService.getSubject(config.getContactNotificationsSubject(), ctx, DEFAULT_CONTACT_NOTIFICATION_SUBJECT),
      "contactUs", ctx, groups);
  }

  //
  // Private methods
  //

  private synchronized Subject getProfileInternal(String serviceUrl) {
    try {
      return executeQuery(serviceUrl, Subject.class);
    } catch (RestClientException e) {
      log.error("Agate connection failure: {}", e.getMessage());
    }

    return null;
  }

  private <T> T executeQuery(String serviceUrl, Class<T> returnType) {

    RestTemplate template = newRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
    HttpEntity<String> entity = new HttpEntity<>(null, headers);
    ResponseEntity<T> response = template.exchange(serviceUrl, HttpMethod.GET, entity, returnType);

    return response.getBody();
  }

  private String getUsersJoinUrl() {
    return UriComponentsBuilder
      .fromHttpUrl(agateServerConfigService.getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path("/users/_join")
      .build().toUriString();
  }

  private String getUsersForgotPasswordUrl() {
    return UriComponentsBuilder
      .fromHttpUrl(agateServerConfigService.getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path("/users/_forgot_password")
      .build().toUriString();
  }

  private String getProfileServiceUrl(String username) {
    return UriComponentsBuilder
      .fromHttpUrl(agateServerConfigService.getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path(String.format("/tickets/subject/%s", username))
      .build().toUriString();
  }

  private String getProfileServiceUrlByGroup(String username, String group) {
    UriComponentsBuilder urlBuilder =
      UriComponentsBuilder.fromHttpUrl(agateServerConfigService.getAgateUrl()).path(DEFAULT_REST_PREFIX);

    if (Strings.isNullOrEmpty(username)) {
      urlBuilder.path(String.format("/application/%s/users", getApplicationName()));
    } else {
      urlBuilder.path(String.format("/application/%s/user/%s", getApplicationName(), username));
    }

    if (!Strings.isNullOrEmpty(group)) {
      urlBuilder.queryParam("group", group);
    }

    return urlBuilder.build().toUriString();
  }
}
