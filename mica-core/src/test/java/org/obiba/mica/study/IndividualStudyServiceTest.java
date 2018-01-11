/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.obiba.core.util.FileUtil;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.config.taxonomies.DatasetTaxonomy;
import org.obiba.mica.config.taxonomies.NetworkTaxonomy;
import org.obiba.mica.config.taxonomies.StudyTaxonomy;
import org.obiba.mica.config.taxonomies.TaxonomyTaxonomy;
import org.obiba.mica.config.taxonomies.VariableTaxonomy;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.notification.EntityPublicationFlowMailNotification;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.core.service.StudyIdGeneratorService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.impl.GridFsService;
import org.obiba.mica.file.notification.FilePublicationFlowMailNotification;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.mongodb.Mongo;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.core.domain.LocalizedString.en;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { IndividualStudyServiceTest.Config.class, JsonConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class IndividualStudyServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private MongoTemplate mongoTemplate;

  @BeforeClass
  public static void init() {
    SecurityUtils.setSecurityManager(new DefaultWebSecurityManager());
  }

  @Before
  public void clearDatabase() {
    mongoTemplate.getDb().dropDatabase();
    reset(eventBus);
  }

  @Test
  public void test_create_and_load_new_study() throws Exception {

    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    individualStudyService.save(study);

    List<StudyState> studyStates = studyStateRepository.findAll();
    assertThat(studyStates).hasSize(1);

    StudyState studyState = studyStates.get(0);
    assertThat(studyState.getId()) //
        .isNotEmpty() //
        .isEqualTo(study.getId());

    verify(eventBus).post(any(DraftStudyUpdatedEvent.class));

    Study retrievedStudy = individualStudyService.findDraft(study.getId());
    assertThat(retrievedStudy).areFieldsEqualToEachOther(study);
  }

  @Test
  public void testCreateStudyWithContacts() throws Exception {
    Study study = new Study();
    study.setId("test");

    Person person = new Person();
    person.setEmail("test@test.com");
    List<Person> persons = Lists.newArrayList();
    persons.add(person);
    study.getMemberships().get(Membership.CONTACT).addAll(persons.stream().map(e -> new Membership(e, "contact")).collect(Collectors.toList()));

    individualStudyService.save(study);
    Study retrievedStudy = individualStudyService.findDraft(study.getId());

    List<Person> retrievedPersons = retrievedStudy.getMemberships().get(Membership.CONTACT).stream().map(Membership::getPerson).collect(Collectors.toList());
    assertThat(retrievedPersons).contains(person);
  }

  @Test
  public void test_update_study() throws Exception {
    Study study = new Study();
    study.setName(en("name en to update").forFr("name fr to update"));
    individualStudyService.save(study);

    study.setName(en("new name en").forFr("new name fr"));
    individualStudyService.save(study);

    List<StudyState> studyStates = studyStateRepository.findAll();
    assertThat(studyStates).hasSize(1);

    StudyState studyState = studyStates.get(0);
    assertThat(studyState.getId()) //
        .isNotEmpty() //
        .isEqualTo(study.getId());

    verify(eventBus, times(2)).post(any(DraftStudyUpdatedEvent.class));

    Study retrievedStudy = individualStudyService.findDraft(study.getId());
    assertThat(retrievedStudy).areFieldsEqualToEachOther(study);
  }

  @Test
  public void test_publish_current() throws Exception {
    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    individualStudyService.save(study);

    assertThat(individualStudyService.findAllStates()).hasSize(1);
    assertThat(individualStudyService.findPublishedStates()).isEmpty();

    individualStudyService.publish(study.getId(), true);
    List<StudyState> publishedStates = individualStudyService.findPublishedStates();
    assertThat(publishedStates).hasSize(1);
    StudyState publishedState = publishedStates.get(0);
    assertThat(publishedState.getId()).isEqualTo(study.getId());
    assertThat(publishedState.getPublishedTag()).isEqualTo("1");

    Study draft = individualStudyService.findDraft(study.getId());
    draft.setName(en("new name en").forFr("new name fr"));
    individualStudyService.save(draft);

    assertThat(individualStudyService.findDraft(study.getId())).areFieldsEqualToEachOther(draft);
  }

  @Test
  public void test_find_all_draft_studies() {
    Stream.of("cancer", "gout", "diabetes").forEach(name -> {
      Study draft = new Study();
      draft.setName(en(name +" en").forFr(name + " fr"));
      individualStudyService.save(draft);
    });

    List<Study> drafts = individualStudyService.findAllDraftStudies();
    assertThat(drafts.size()).isEqualTo(3);
    assertThat(drafts.get(2).getName().get("en")).isEqualTo("diabetes en");
  }

  @Test
  public void test_loosing_git_base_repo() throws IOException {
    Study study = new Study();
    Stream.of("a", "b", "c").forEach(name -> {
      study.setName(en(name+ " en").forFr(name + " fr"));
      individualStudyService.save(study);
      individualStudyService.publish(study.getId(), true);
    });

    FileUtil.delete(Config.BASE_REPO);
    Study draft = individualStudyService.findDraft(study.getId());
    draft.setName(en("d en").forFr("d fr"));
    individualStudyService.save(draft);
    individualStudyService.publish(draft.getId(), true);
    StudyState studyState = individualStudyService.findStateById(draft.getId());
    assertThat(studyState.isPublished()).isTrue();
    assertThat(studyState.getPublishedTag()).isEqualTo("4");
  }

  @Test
  public void test_loosing_git_clone_repo() throws IOException {
    Study study = new Study();
    Stream.of("a", "b", "c").forEach(name -> {
      study.setName(en(name+ " en").forFr(name + " fr"));
      individualStudyService.save(study);
      individualStudyService.publish(study.getId(), true);
    });

    FileUtil.delete(Config.BASE_CLONE);
    Study draft = individualStudyService.findDraft(study.getId());
    draft.setName(en("d en").forFr("d fr"));
    individualStudyService.save(draft);
    individualStudyService.publish(draft.getId(), true);
    StudyState studyState = individualStudyService.findStateById(draft.getId());
    assertThat(studyState.isPublished()).isTrue();
    assertThat(studyState.getPublishedTag()).isEqualTo("4");
  }

  @Test
  public void test_loosing_git_base_and_clone_repos() throws IOException {
    Study study = new Study();
    Stream.of("a", "b", "c").forEach(name -> {
      study.setName(en(name+ " en").forFr(name + " fr"));
      individualStudyService.save(study);
      individualStudyService.publish(study.getId(), true);
    });

    FileUtil.delete(Config.BASE_REPO);
    FileUtil.delete(Config.BASE_CLONE);

    Study draft = individualStudyService.findDraft(study.getId());
    draft.setName(en("d en").forFr("d fr"));
    individualStudyService.save(draft);
    individualStudyService.publish(draft.getId(), true);
    StudyState studyState = individualStudyService.findStateById(draft.getId());
    assertThat(studyState.isPublished()).isTrue();
    assertThat(studyState.getPublishedTag()).isEqualTo("1");
  }

  @Test
  public void test_delete_study() {
    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    individualStudyService.save(study);

    assertThat(studyStateRepository.findAll()).hasSize(1);

    individualStudyService.delete(study.getId());

    assertThat(studyRepository.findAll()).hasSize(0);
    assertThat(studyStateRepository.findAll()).hasSize(0);
  }

  @Test
  public void test_delete_study_conflict() {
    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    individualStudyService.save(study);
    Network network = new Network();
    network.setId("test");
    network.setStudyIds(new ArrayList() {{ add(study.getId()); }});
    networkRepository.save(network);

    assertThat(studyStateRepository.findAll()).hasSize(1);

    exception.expect(ConstraintException.class);

    individualStudyService.delete(study.getId());
  }

  @After
  public void cleanup() throws IOException {
    FileUtil.delete(Config.BASE_REPO);
    FileUtil.delete(Config.BASE_CLONE);
    FileUtil.delete(Config.TEMP);
  }

  @Configuration
  @EnableMongoRepositories("org.obiba.mica")
  static class Config extends AbstractMongoConfiguration {

    static final File BASE_REPO = Files.createTempDir();

    static final File BASE_CLONE = Files.createTempDir();

    static final File TEMP = Files.createTempDir();

    static {
      BASE_REPO.deleteOnExit();
      BASE_CLONE.deleteOnExit();
      TEMP.deleteOnExit();
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
      return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public IndividualStudyService studyService() {
      return new IndividualStudyService();
    }

    @Bean
    public TempFileService tempFileService() {
      TempFileService tempFileService = new TempFileService();
      tempFileService.setTmpRoot(TEMP);
      return tempFileService;
    }

    @Bean
    public GitService gitService() throws IOException {
      GitService gitService = new GitService();
      gitService.setRepositoriesRoot(BASE_REPO);
      gitService.setClonesRoot(BASE_CLONE);
      return gitService;
    }

    @Bean
    public GitCommandHandler gitCommandHandler() throws IOException {
      return new GitCommandHandler();
    }

    @Bean
    public PublishedStudyService publishedStudyService() {
      return mock(PublishedStudyService.class);
    }

    @Bean
    public FileStoreService fsService() {
      return mock(GridFsService.class);
    }

    @Bean
    public FileSystemService fileSystemService() {
      return mock(FileSystemService.class);
    }

    @Bean
    public SubjectAclService subjectAclService() {
      return mock(SubjectAclService.class);
    }

    @Bean
    public MailService mailService() {
      return mock(MailService.class);
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
      return mock(AttachmentRepository.class);
    }

    @Bean
    public AttachmentStateRepository attachmentStateRepository() {
      return mock(AttachmentStateRepository.class);
    }

    @Bean
    public GridFsOperations gridFsOperations() {
      return mock(GridFsOperations.class);
    }

    @Bean
    public MicaConfigService micaConfigService() {
      MicaConfigService micaConfigService = mock(MicaConfigService.class);
      MicaConfig micaConfig = new MicaConfig();
      micaConfig.setRoles(Lists.newArrayList(Membership.CONTACT, Membership.INVESTIGATOR));
      when(micaConfigService.getConfig()).thenReturn(micaConfig);

      return micaConfigService;
    }

    @Bean
    public NetworkTaxonomy networksConfiguration() {
      return mock(NetworkTaxonomy.class);
    }

    @Bean
    public StudyTaxonomy studiesConfiguration() {
      return mock(StudyTaxonomy.class);
    }

    @Bean
    public DatasetTaxonomy datasetsConfiguration() {
      return mock(DatasetTaxonomy.class);
    }

    @Bean
    public VariableTaxonomy variablesConfiguration() {
      return mock(VariableTaxonomy.class);
    }

    @Bean
    public TaxonomyTaxonomy taxonomyConfiguration() {
      return mock(TaxonomyTaxonomy.class);
    }

    @Bean
    public StudyIdGeneratorService studyIdGeneratorService() {
      return mock(StudyIdGeneratorService.class);
    }

    @Bean
    public EntityPublicationFlowMailNotification entityPublicationFlowNotification() {
      return mock(EntityPublicationFlowMailNotification.class);
    }

    @Bean
    public FilePublicationFlowMailNotification filePublicationFlowNotification() {
      return mock(FilePublicationFlowMailNotification.class);
    }

    @Bean
    public EventBus eventBus() {
      return mock(EventBus.class);
    }

    @Bean
    public TaxonomyConfigService taxonomyConfigService() {
      return mock(TaxonomyConfigService.class);
    }

    @Bean
    public ModelAwareTranslator modelAwareTranslator() {
      return mock(ModelAwareTranslator.class);
    }

    @Override
    protected String getDatabaseName() {
      return "mica-test";
    }

    @Override
    public Mongo mongo() throws IOException {
      return MongodForTestsFactory.with(Version.Main.PRODUCTION).newMongo();
    }

    @Override
    @Bean
    public CustomConversions customConversions() {
      return new CustomConversions(
          Lists.newArrayList(new MongoDbConfiguration.LocalizedStringWriteConverter(),
              new MongoDbConfiguration.LocalizedStringReadConverter()));
    }

    @Override
    protected String getMappingBasePackage() {
      return "org.obiba.mica";
    }

  }
}
