package org.obiba.mica.config.metrics;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * SpringBoot Actuator HealthIndicator check for JavaMail.
 */
@Component
public class EmailHealthIndicator extends HealthCheckIndicator {

  private static final Logger log = LoggerFactory.getLogger(EmailHealthIndicator.class);

  @Inject
  private JavaMailSenderImpl javaMailSender;

  @Override
  protected Result check() {
    try {
      log.debug("Initializing JavaMail health indicator");
      javaMailSender.getSession().getTransport()
        .connect(javaMailSender.getHost(), javaMailSender.getUsername(), javaMailSender.getPassword());

      return healthy();
    } catch(MessagingException e) {
      log.debug("Cannot connect to e-mail server.", e);
      return unhealthy("Cannot connect to e-mail server.", e);
    }
  }
}
