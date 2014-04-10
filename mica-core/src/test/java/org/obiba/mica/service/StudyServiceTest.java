package org.obiba.mica.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.domain.Study;
import org.obiba.mica.domain.StudyState;
import org.obiba.mica.repository.StudyStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.fakemongo.Fongo;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import com.mongodb.Mongo;

import static com.lordofthejars.nosqlunit.mongodb.MongoDbRule.MongoDbRuleBuilder.newMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.obiba.mica.domain.LocalizedString.en;

@RunWith(SpringJUnit4ClassRunner.class)
//@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = StudyServiceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class StudyServiceTest {

  private static final Logger log = LoggerFactory.getLogger(StudyServiceTest.class);

  private static final String DATABASE_NAME = "mica";

  @Rule
  public MongoDbRule mongoDbRule = newMongoDbRule().defaultSpringMongoDb(DATABASE_NAME);

  @Inject
  private StudyService studyService;

  @Inject
  private StudyStateRepository studyStateRepository;

  @Test
  public void test_save_new_study() throws Exception {

    Study study = new Study();
    study.setName(en("english name").forFr("nom français"));
    studyService.save(study);

    List<StudyState> studyStates = studyStateRepository.findAll();
    assertThat(studyStates).hasSize(1);

    log.debug("state: {}", studyStates.get(0));

  }

  @Configuration
  @EnableMongoRepositories("org.obiba.mica.repository")
  static class Config extends AbstractMongoConfiguration {

    @Bean
    public StudyService studyService() {
      return new StudyService();
    }

    @Bean
    public GitService gitService() throws IOException {
      File baseRepo = Files.createTempDir();
      baseRepo.deleteOnExit();
      return new GitService(baseRepo);
    }

    @Bean
    public EventBus eventBus() {
      return mock(EventBus.class);
    }

    @Override
    protected String getDatabaseName() {
      return DATABASE_NAME;
    }

    @Override
    public Mongo mongo() {
      return new Fongo("mica-test").getMongo();
    }

    @Override
    protected String getMappingBasePackage() {
      return "org.obiba.mica.domain";
    }
  }

}
