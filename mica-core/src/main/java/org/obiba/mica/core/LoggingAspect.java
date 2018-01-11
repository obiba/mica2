/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core;

import java.util.Arrays;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.obiba.mica.config.Profiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Aspect for logging.
 */
@Aspect
public class LoggingAspect {

  @SuppressWarnings("NonConstantLogger")
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  private Environment env;

  private static final String WITHIN_EXPR = "within(org.obiba.mica.core.repository..*)";// || within(org.obiba.mica.core.service..*)";

  @AfterThrowing(pointcut = WITHIN_EXPR, throwing = "e")
  public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
    if(env.acceptsProfiles(Profiles.DEV)) {
      logger.error("Exception in {}.{}() with cause = {}", joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(), e.getCause(), e);
    } else {
      logger.error("Exception in {}.{}() with cause = {}", joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(), e.getCause());
    }
  }

  @Around(WITHIN_EXPR)
  public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
    logger.debug("Enter: {}.{}() with argument[s] = {}", joinPoint.getSignature().getDeclaringTypeName(),
        joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
    try {
      Object result = joinPoint.proceed();
      logger.debug("Exit: {}.{}() with result = {}", joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(), result);
      return result;
    } catch(IllegalArgumentException e) {
      logger.error("Illegal argument: {} in {}.{}()", Arrays.toString(joinPoint.getArgs()),
          joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
      throw e;
    }
  }
}
