/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import java.util.List;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Registers the {@link RequiresAnyRole} check on every resource method of an annotated class, so a new
 * method cannot be added without it. Only resource methods get the filter: the OPTIONS responses Jersey
 * generates (CORS preflight) are not affected.
 */
@Provider
public class RequiresAnyRoleFeature implements DynamicFeature {

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    RequiresAnyRole annotation = findAnnotation(resourceInfo.getResourceClass());
    if (annotation != null) {
      // after the obiba-jersey-shiro authentication filter (AUTHORIZATION): unauthenticated requests still get 401
      context.register(new RequiresAnyRoleFilter(List.of(annotation.value())), Priorities.AUTHORIZATION + 1);
    }
  }

  /** the class itself or, when Spring proxies it with CGLIB, its superclass */
  private static RequiresAnyRole findAnnotation(Class<?> resourceClass) {
    if (resourceClass == null) return null;
    RequiresAnyRole annotation = resourceClass.getAnnotation(RequiresAnyRole.class);
    if (annotation == null && resourceClass.getSuperclass() != null) {
      annotation = resourceClass.getSuperclass().getAnnotation(RequiresAnyRole.class);
    }
    return annotation;
  }

  static class RequiresAnyRoleFilter implements ContainerRequestFilter {

    private final List<String> roles;

    RequiresAnyRoleFilter(List<String> roles) {
      this.roles = roles;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
      Subject subject = SecurityUtils.getSubject();
      if (roles.stream().noneMatch(subject::hasRole)) throw new ForbiddenException();
    }
  }
}
