/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import com.google.common.eventbus.EventBus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.repository.DocumentSetRepository;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * Deleting a dataset removes its variables from the variable sets, except from the locked ones.
 */
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = VariableSetServiceDatasetDeletedTest.Config.class)
public class VariableSetServiceDatasetDeletedTest {

  private VariableSetService variableSetService;

  @Inject
  private DocumentSetRepository documentSetRepository;

  @Inject
  private MongoTemplate mongoTemplate;

  @BeforeAll
  public static void init() {
    assumeTrue(isMongoAvailable(), "MongoDB is not available on localhost:27017");
  }

  private static boolean isMongoAvailable() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", 27017), 1000);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @BeforeEach
  public void setUp() {
    mongoTemplate.getDb().drop();
    variableSetService = new VariableSetService(null, null);
    ReflectionTestUtils.setField(variableSetService, "documentSetRepository", documentSetRepository);
    ReflectionTestUtils.setField(variableSetService, "eventBus", mock(EventBus.class));
  }

  /**
   * The dataset ID is matched literally: '.' is not a regex character, and "ds.1" is not a prefix of "ds.10".
   */
  @Test
  public void test_dataset_deleted_removes_its_variables_from_unlocked_sets() {
    DocumentSet set = createSet(DatasetVariable.MAPPING_NAME, false,
      "ds.1:v1:Collected", "ds.1:v2:Collected", "dsX1:v1:Collected", "ds.10:v1:Collected");
    DocumentSet locked = createSet(DatasetVariable.MAPPING_NAME, true, "ds.1:v1:Collected");
    DocumentSet other = createSet("Study", false, "ds.1:v1:Collected");
    StudyDataset dataset = new StudyDataset();
    dataset.setId("ds.1");

    variableSetService.datasetDeleted(new DatasetDeletedEvent(dataset));

    assertThat(identifiers(set)).containsExactly("dsX1:v1:Collected", "ds.10:v1:Collected");
    assertThat(identifiers(locked)).containsExactly("ds.1:v1:Collected");
    assertThat(identifiers(other)).containsExactly("ds.1:v1:Collected");
  }

  private DocumentSet createSet(String type, boolean locked, String... identifiers) {
    DocumentSet set = new DocumentSet();
    set.setType(type);
    set.setUsername("user");
    set.setLocked(locked);
    set.setIdentifiers(List.of(identifiers));
    return documentSetRepository.insert(set);
  }

  private List<String> identifiers(DocumentSet set) {
    return List.copyOf(documentSetRepository.findById(set.getId()).orElseThrow().getIdentifiers());
  }

  @Configuration
  @EnableMongoRepositories(basePackageClasses = DocumentSetRepository.class,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DocumentSetRepository.class))
  static class Config extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
      return "mica-test";
    }
  }
}
