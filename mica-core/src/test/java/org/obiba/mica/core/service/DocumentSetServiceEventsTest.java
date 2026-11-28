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

import com.google.common.eventbus.EventBus;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.event.DocumentSetCreatedEvent;
import org.obiba.mica.core.event.DocumentSetDeletedEvent;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.core.repository.DocumentSetRepository;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Only a new set posts a {@link DocumentSetCreatedEvent}; every identifiers change still posts a
 * {@link DocumentSetUpdatedEvent}, for the indexers.
 */
public class DocumentSetServiceEventsTest {

  private static final String TYPE = "Variable";

  private DocumentSetService service;

  private DocumentSetRepository repository;

  private EventBus eventBus;

  @BeforeEach
  public void setUp() {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("user");
    ThreadContext.bind(subject);

    repository = mock(DocumentSetRepository.class);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    eventBus = mock(EventBus.class);
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getConfig()).thenReturn(new MicaConfig());

    service = new DocumentSetService() {
      @Override
      public String getType() {
        return TYPE;
      }
    };
    ReflectionTestUtils.setField(service, "documentSetRepository", repository);
    ReflectionTestUtils.setField(service, "eventBus", eventBus);
    ReflectionTestUtils.setField(service, "micaConfigService", micaConfigService);
  }

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void createPostsTheUpdatedThenTheCreatedEvent() {
    DocumentSet created = service.create("my set", List.of("a", "b"));

    List<Object> events = postedEvents();
    assertEquals(2, events.size());
    DocumentSetUpdatedEvent updated = assertInstanceOf(DocumentSetUpdatedEvent.class, events.get(0));
    assertEquals(created, updated.getPersistable());
    DocumentSetCreatedEvent createdEvent = assertInstanceOf(DocumentSetCreatedEvent.class, events.get(1));
    assertEquals(created, createdEvent.getPersistable());
  }

  @Test
  public void createForAnAnonymousUserPostsOneCreatedEvent() {
    when(ThreadContext.getSubject().getPrincipal()).thenReturn(null);

    service.create("", List.of(), "anonymous");

    assertEquals(1, postedEvents().stream().filter(DocumentSetCreatedEvent.class::isInstance).count());
  }

  @Test
  public void identifiersChangesPostOnlyUpdatedEvents() {
    existingSet("a");

    service.addIdentifiers("set1", List.of("b"));
    service.removeIdentifiers("set1", List.of("a"));
    service.setIdentifiers("set1", List.of("c"));

    List<Object> events = postedEvents();
    assertEquals(3, events.size());
    events.forEach(event -> assertInstanceOf(DocumentSetUpdatedEvent.class, event));
  }

  @Test
  public void deletePostsTheDeletedEvent() {
    DocumentSet set = existingSet("a");

    service.delete(set);

    DocumentSetDeletedEvent deleted = assertInstanceOf(DocumentSetDeletedEvent.class, postedEvents().get(0));
    assertEquals(set, deleted.getPersistable());
  }

  private DocumentSet existingSet(String... identifiers) {
    DocumentSet set = new DocumentSet();
    set.setId("set1");
    set.setType(TYPE);
    set.setUsername("user");
    set.setIdentifiers(List.of(identifiers));
    when(repository.findById("set1")).thenReturn(Optional.of(set));
    return set;
  }

  private List<Object> postedEvents() {
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventBus, atLeastOnce()).post(captor.capture());
    return captor.getAllValues();
  }
}
