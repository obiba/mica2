/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessEntityWithParent;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.config.MongoDbConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The status of the child entities of the data access requests are fetched in one query for all the listed requests.
 */
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = DataAccessEntityWithParentRepositoryTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DataAccessEntityWithParentRepositoryTest {

  // random database name, so that no existing database is touched
  private static final String DATABASE_NAME = "mica-test-" + UUID.randomUUID();

  @Inject
  private DataAccessAmendmentRepository amendmentRepository;

  @Inject
  private DataAccessFeasibilityRepository feasibilityRepository;

  @Inject
  private DataAccessAgreementRepository agreementRepository;

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

  @AfterEach
  public void dropDatabase() {
    mongoTemplate.getDb().drop();
  }

  @Test
  public void test_amendments_findStatusByParentIdIn_selects_the_given_parents_only() {
    amendmentRepository.insert((DataAccessAmendment) child(DataAccessAmendment.newBuilder().parentId("dar1"), "dar1-A1", DataAccessEntityStatus.OPENED));
    amendmentRepository.insert((DataAccessAmendment) child(DataAccessAmendment.newBuilder().parentId("dar1"), "dar1-A2", DataAccessEntityStatus.APPROVED));
    amendmentRepository.insert((DataAccessAmendment) child(DataAccessAmendment.newBuilder().parentId("dar2"), "dar2-A1", DataAccessEntityStatus.SUBMITTED));
    amendmentRepository.insert((DataAccessAmendment) child(DataAccessAmendment.newBuilder().parentId("dar3"), "dar3-A1", DataAccessEntityStatus.OPENED));

    List<DataAccessAmendment> found = amendmentRepository.findStatusByParentIdIn(List.of("dar1", "dar2"));

    assertThat(found).extracting(DataAccessEntityWithParent::getParentId, DataAccessEntityWithParent::getStatus)
      .containsExactlyInAnyOrder(
        tuple("dar1", DataAccessEntityStatus.OPENED),
        tuple("dar1", DataAccessEntityStatus.APPROVED),
        tuple("dar2", DataAccessEntityStatus.SUBMITTED));
    // only the parent and the status are fetched
    assertThat(found).extracting(DataAccessEntityWithParent::getContent).containsOnlyNulls();
  }

  @Test
  public void test_feasibilities_findStatusByParentIdIn_selects_the_given_parents_only() {
    feasibilityRepository.insert((DataAccessFeasibility) child(DataAccessFeasibility.newBuilder().parentId("dar1"), "dar1-F1", DataAccessEntityStatus.REVIEWED));
    feasibilityRepository.insert((DataAccessFeasibility) child(DataAccessFeasibility.newBuilder().parentId("dar2"), "dar2-F1", DataAccessEntityStatus.OPENED));

    assertThat(feasibilityRepository.findStatusByParentIdIn(List.of("dar1")))
      .extracting(DataAccessEntityWithParent::getParentId, DataAccessEntityWithParent::getStatus)
      .containsExactly(tuple("dar1", DataAccessEntityStatus.REVIEWED));
  }

  @Test
  public void test_agreements_findStatusByParentIdIn_selects_the_given_parents_only() {
    agreementRepository.insert((DataAccessAgreement) child(DataAccessAgreement.newBuilder().parentId("dar1"), "dar1-bob", DataAccessEntityStatus.OPENED));
    agreementRepository.insert((DataAccessAgreement) child(DataAccessAgreement.newBuilder().parentId("dar2"), "dar2-bob", DataAccessEntityStatus.APPROVED));

    assertThat(agreementRepository.findStatusByParentIdIn(List.of("dar2")))
      .extracting(DataAccessEntityWithParent::getParentId, DataAccessEntityWithParent::getStatus)
      .containsExactly(tuple("dar2", DataAccessEntityStatus.APPROVED));
  }

  @Test
  public void test_findStatusByParentIdIn_no_parents() {
    amendmentRepository.insert((DataAccessAmendment) child(DataAccessAmendment.newBuilder().parentId("dar1"), "dar1-A1", DataAccessEntityStatus.OPENED));

    assertThat(amendmentRepository.findStatusByParentIdIn(List.of())).isEmpty();
  }

  private static DataAccessEntityWithParent child(DataAccessEntity.Builder builder, String id,
    DataAccessEntityStatus status) {
    DataAccessEntityWithParent entity = (DataAccessEntityWithParent) builder.applicant("applicant").status(status.name())
      .content("{\"projectTitle\":\"" + id + "\"}").build();
    entity.setId(id);
    return entity;
  }

  @Configuration
  @EnableMongoRepositories(basePackageClasses = DataAccessAmendmentRepository.class,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
      classes = {DataAccessAmendmentRepository.class, DataAccessFeasibilityRepository.class, DataAccessAgreementRepository.class}))
  static class Config extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
      return DATABASE_NAME;
    }

    @Override
    @Bean
    public MongoCustomConversions customConversions() {
      return new MongoCustomConversions(
        Lists.newArrayList(new MongoDbConfiguration.LocalizedStringWriteConverter(),
          new MongoDbConfiguration.LocalizedStringReadConverter()));
    }
  }
}
