/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.Optional;

import org.apache.commons.math3.util.Pair;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.AbstractShiroTest;
import org.obiba.mica.core.notification.EntityPublishedMailNotification;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.service.ProjectService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A failure while notifying an entity (un)publication must not fail it, as the state is already saved.
 */
@ExtendWith(MockitoExtension.class)
public class EntityPublishedNotificationFailureTest extends AbstractShiroTest {

  private static final String ID = "abc";

  @InjectMocks
  private ProjectService projectService;

  @Mock
  private ProjectStateRepository projectStateRepository;

  @Mock
  private GitService gitService;

  @Mock
  private EntityPublishedMailNotification entityPublishedNotification;

  private Subject subject;

  private ProjectState state;

  @BeforeEach
  public void setUp() {
    subject = mock(Subject.class);
    setSubject(subject);

    state = new ProjectState();
    state.setId(ID);
    when(projectStateRepository.findById(ID)).thenReturn(Optional.of(state));
    doThrow(new RuntimeException("agate is down")).when(entityPublishedNotification)
      .send(anyString(), anyString(), anyBoolean());
  }

  @AfterEach
  public void tearDown() {
    clearSubject();
  }

  @Test
  public void test_publish_succeeds_when_notification_fails() {
    // only the publication records the current user
    when(subject.getPrincipal()).thenReturn("editor");
    when(gitService.tag(state)).thenReturn(Pair.create("1", "commit-1"));

    ProjectState published = projectService.publishState(ID);

    assertThat(published).isSameAs(state);
    assertThat(published.getPublishedTag()).isEqualTo("1");
    verify(projectStateRepository).save(state);
    verify(entityPublishedNotification).send(ID, "project", true);
  }

  @Test
  public void test_unpublish_succeeds_when_notification_fails() {
    state.setPublishedTag("1");

    ProjectState unpublished = projectService.unPublishState(ID);

    assertThat(unpublished).isSameAs(state);
    assertThat(unpublished.getPublishedTag()).isNull();
    verify(projectStateRepository).save(state);
    verify(entityPublishedNotification).send(ID, "project", false);
  }
}
