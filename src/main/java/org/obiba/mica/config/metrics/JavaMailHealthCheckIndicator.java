package org.obiba.mica.config.metrics;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * SpringBoot Actuator HealthIndicator check for JavaMail.
 */
public class JavaMailHealthCheckIndicator extends HealthCheckIndicator {

  public static final String EMAIL_HEALTH_INDICATOR = "email";

  private static final Logger log = LoggerFactory.getLogger(JavaMailHealthCheckIndicator.class);

  private JavaMailSenderImpl javaMailSender;

  public JavaMailHealthCheckIndicator() {
  }

  public void setJavaMailSender(JavaMailSenderImpl javaMailSender) {
    this.javaMailSender = javaMailSender;
  }

  @Override
  protected String getHealthCheckIndicatorName() {
    return EMAIL_HEALTH_INDICATOR;
  }

  @Override
  protected Result check() {
    log.debug("Initializing JavaMail health indicator");

    try {
      javaMailSender.getSession().getTransport()
          .connect(javaMailSender.getHost(), javaMailSender.getUsername(), javaMailSender.getPassword());

      return healthy();

    } catch(MessagingException e) {
      log.debug("Cannot connect to e-mail server.", e);
      return unhealthy("Cannot connect to e-mail server", e);
    }
  }
}
