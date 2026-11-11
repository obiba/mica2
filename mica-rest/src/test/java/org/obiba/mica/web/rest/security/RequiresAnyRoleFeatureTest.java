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
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.security.Roles;
import org.obiba.mica.study.rest.StudiesImportResource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The study import is restricted to the global roles by a class-level guard (#4752).
 */
@ExtendWith(MockitoExtension.class)
public class RequiresAnyRoleFeatureTest {

  @Mock
  private ResourceInfo resourceInfo;

  @Mock
  private FeatureContext featureContext;

  @Mock
  private ContainerRequestContext requestContext;

  @Mock
  private Subject subject;

  @BeforeEach
  public void setUp() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  /**
   * A mica-external-editor has ADD on every study type but none of the global roles.
   */
  @Test
  public void test_study_import_forbidden_without_global_role() {
    ContainerRequestFilter filter = registeredFilter(StudiesImportResource.class);
    withRoles(Roles.MICA_EXTERNAL_EDITOR, Roles.MICA_USER);

    assertThatThrownBy(() -> filter.filter(requestContext)).isInstanceOf(ForbiddenException.class);
  }

  /**
   * Any one of the global roles is enough (the obiba-jersey-shiro filter would require all of them).
   */
  @Test
  public void test_study_import_allowed_with_any_global_role() {
    ContainerRequestFilter filter = registeredFilter(StudiesImportResource.class);
    for (String role : List.of(Roles.MICA_ADMIN, Roles.MICA_REVIEWER, Roles.MICA_EDITOR)) {
      withRoles(role);
      assertThatCode(() -> filter.filter(requestContext)).as(role).doesNotThrowAnyException();
    }
  }

  /** Spring may hand Jersey a CGLIB subclass of the resource */
  @Test
  public void test_annotation_found_on_proxied_class() {
    registeredFilter(ProxiedResource.class);
  }

  @Test
  public void test_no_filter_without_annotation() {
    doReturn(Object.class).when(resourceInfo).getResourceClass();

    new RequiresAnyRoleFeature().configure(resourceInfo, featureContext);

    verifyNoInteractions(featureContext);
  }

  /** the filter the feature registers, after the authentication filter */
  private ContainerRequestFilter registeredFilter(Class<?> resourceClass) {
    doReturn(resourceClass).when(resourceInfo).getResourceClass();

    new RequiresAnyRoleFeature().configure(resourceInfo, featureContext);

    ArgumentCaptor<Object> filter = ArgumentCaptor.forClass(Object.class);
    verify(featureContext).register(filter.capture(), eq(Priorities.AUTHORIZATION + 1));
    return (ContainerRequestFilter) filter.getValue();
  }

  /** the subject has exactly these roles */
  private void withRoles(String... roles) {
    Set<String> granted = Set.of(roles);
    lenient().when(subject.hasRole(anyString())).thenAnswer(invocation -> granted.contains(invocation.<String>getArgument(0)));
  }

  private static class ProxiedResource extends StudiesImportResource {
  }
}
