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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class DebugMethodImpl {

  private static final Logger logger = LoggerFactory.getLogger(DebugMethodImpl.class);

  @Around("execution(@org.obiba.mica.core.DebugMethod * *(..)) && @annotation(debugMethodAnnotation)")
  public Object logDuration(ProceedingJoinPoint joinPoint, DebugMethod debugMethodAnnotation) throws Throwable {

    logger.debug(String.format("Method called [%s] with params [%s]", joinPoint.getSignature(), Arrays.toString(joinPoint.getArgs())));

    return joinPoint.proceed();
  }
}
