/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import static java.net.URLEncoder.encode;
import static java.util.stream.Collectors.toList;

/**
 * Service for sending e-mails.
 *
 * We use the @Async annotation to send e-mails asynchronously.
 */
@Service
public class MailService extends AgateRestService {

  private static final Logger log = LoggerFactory.getLogger(MailService.class);

  private static final String DEFAULT_NOTIFICATIONS_PATH = "/notifications";

  /**
   * System default email address that sends the e-mails.
   */
  private String from;

  @Override
  public void afterPropertiesSet() throws Exception {
    from = env.getProperty("spring.mail.from");
    if(Strings.isNullOrEmpty(from)) {
      from = "mica@example.org";
    }
  }

  @Async
  public void sendEmailToUsers(String subject, String template, Map<String, String> context, String... usernames) {
    sendEmail(subject, template, context, toRecipientFormParam("username", usernames));
  }

  @Async
  public void sendEmailToGroups(String subject, String template, Map<String, String> context, String... groups) {
    sendEmail(subject, template, context, toRecipientFormParam("group", groups));
  }

  @Async
  public void sendEmailToGroupsAndUsers(String subject, String template, Map<String, String> context,
    Collection<String> groups, Collection<String> users) {
    String groupsParam = toRecipientFormParam("group", groups.stream().toArray(String[]::new));
    String usernameParam = toRecipientFormParam("username", users.stream().toArray(String[]::new));
    sendEmail(subject, template, context, Joiner.on("&")
      .join(Stream.of(groupsParam, usernameParam).filter(s -> !Strings.isNullOrEmpty(s)).collect(toList())));
  }

  @Async
  public void sendEmailToUsers(String subject, String text, String... usernames) {
    sendEmail(subject, text, toRecipientFormParam("username", usernames));
  }

  @Async
  public void sendEmailToGroups(String subject, String text, String... groups) {
    sendEmail(subject, text, toRecipientFormParam("group", groups));
  }

  public String getSubject(String subjectFormat, Map<String, String> ctx, String defaultSubject) {
    StrSubstitutor sub = new StrSubstitutor(ctx, "${", "}");

    String temp = Optional.ofNullable(subjectFormat) //
      .filter(s -> !s.isEmpty()) //
      .orElse(defaultSubject);

    return sub.replace(temp);
  }

  //
  // Private methods
  //

  private String toRecipientFormParam(String type, String... recipients) {
    StringBuilder recipient = new StringBuilder();
    if(recipients != null) {
      Stream.of(recipients).forEach(rec -> {
        try {
          if(recipient.length() > 0) recipient.append("&");
          recipient.append(type).append("=").append(encode(rec, "UTF-8"));
        } catch(UnsupportedEncodingException ignored) {
        }
      });
    }
    return recipient.toString();
  }

  private synchronized void sendEmail(String subject, String templateName, Map<String, String> context,
    String recipient) {
    try {
      RestTemplate template = newRestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      StringBuilder form = new StringBuilder(Strings.isNullOrEmpty(recipient) ? "" : recipient + "&");
      form.append("subject=").append(encode(subject, "UTF-8")).append("&template=")
        .append(encode(templateName, "UTF-8"));
      context.forEach((k, v) -> {
        try {
          form.append("&").append(k).append("=").append(encode(v, "UTF-8"));
        } catch(UnsupportedEncodingException ignored) {
        }
      });
      log.info("Sending email with parameters: {}", form);
      HttpEntity<String> entity = new HttpEntity<>(form.toString(), headers);

      ResponseEntity<String> response = template.exchange(getNotificationsUrl(), HttpMethod.POST, entity, String.class);

      if(response.getStatusCode().is2xxSuccessful()) {
        log.info("Email sent via Agate");
      } else {
        log.error("Agate email service failure with status: {}", response.getStatusCode());
      }
    } catch(Exception e) {
      log.error("Agate email service connection failure: {}", e.getMessage());
    }
  }

  private synchronized void sendEmail(String subject, String text, String recipient) {
    try {
      RestTemplate template = newRestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.set(APPLICATION_AUTH_HEADER, getApplicationAuth());
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      String form = (Strings.isNullOrEmpty(recipient) ? "" : recipient + "&") + "subject=" +
        encode(subject, "UTF-8") + "&body=" + encode(text, "UTF-8");
      HttpEntity<String> entity = new HttpEntity<>(form, headers);

      ResponseEntity<String> response = template.exchange(getNotificationsUrl(), HttpMethod.POST, entity, String.class);

      if(response.getStatusCode().is2xxSuccessful()) {
        log.info("Email sent via Agate");
      } else {
        log.error("Sending email via Agate failed with status: {}", response.getStatusCode());
      }
    } catch(Exception e) {
      log.error("Agate connection failure: {}", e.getMessage());
    }
  }

  private String getNotificationsUrl() {
    return UriComponentsBuilder
      .fromHttpUrl(agateServerConfigService.getAgateUrl())
      .path(DEFAULT_REST_PREFIX)
      .path(DEFAULT_NOTIFICATIONS_PATH).build()
      .toUriString();
  }

}
