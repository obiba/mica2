/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.magma.DatasourceFactory;
import org.obiba.magma.Variable;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.domain.HarmonizedDataset;
import org.obiba.mica.repository.HarmonizedDatasetRepository;
import org.obiba.mica.service.DatasetService;
import org.obiba.mica.service.NoSuchDatasetException;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;

@Service
@Validated
public class HarmonizedDatasetService extends DatasetService implements EnvironmentAware {

  @Inject
  private HarmonizedDatasetRepository harmonizedDatasetRepository;

  private RelaxedPropertyResolver opalPropertyResolver;

  private Map<String, RestDatasource> datasourceMap = new HashMap<>();

  @Override
  public void setEnvironment(Environment environment) {
    opalPropertyResolver = new RelaxedPropertyResolver(environment, "opal.");
  }

  public void save(@NotNull HarmonizedDataset dataset) {
    harmonizedDatasetRepository.save(dataset);
  }

  /**
   * Get the {@link org.obiba.mica.domain.HarmonizedDataset} from its id.
   *
   * @param id
   * @return
   * @throws org.obiba.mica.service.NoSuchDatasetException
   */
  @NotNull
  public HarmonizedDataset findById(@NotNull String id) throws NoSuchDatasetException {
    HarmonizedDataset dataset = harmonizedDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link org.obiba.mica.domain.HarmonizedDataset}s.
   *
   * @return
   */
  public List<HarmonizedDataset> findAll() {
    return harmonizedDatasetRepository.findAll();
  }

  /**
   * Get all {@link org.obiba.mica.domain.HarmonizedDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizedDataset> findAll(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAll();
    return harmonizedDatasetRepository.findByStudyTablesStudyId(studyId);
  }

  @Override
  public RestValueTable getTable(@NotNull String id) throws NoSuchDatasetException {
    HarmonizedDataset dataset = findById(id);
    return execute(dataset.getProject(), datasource -> (RestValueTable) datasource.getValueTable(dataset.getTable()));
  }

  /**
   * Get the variables of a {@link org.obiba.mica.domain.HarmonizedDataset} from its id.
   *
   * @param id
   * @return
   * @throws org.obiba.mica.service.NoSuchDatasetException
   */
  @Override
  public Iterable<Variable> getVariables(@NotNull String id) throws NoSuchDatasetException {
    HarmonizedDataset dataset = findById(id);
    return execute(dataset.getProject(), datasource -> datasource.getValueTable(dataset.getTable()).getVariables());
  }

  /**
   * On study deletion, go through all datasets related to this study and remove the dependency.
   *
   * @param event
   */
  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {

  }

  //
  // Private methods
  //

  /**
   * Build or reuse the {@link org.obiba.opal.rest.client.magma.RestDatasource} and execute the callback with it.
   *
   * @param callback
   * @param <T>
   * @return
   */
  private <T> T execute(String project, DatasourceCallback<T> callback) {
    return execute(getDatasource(project), callback);
  }

  /**
   * Get a {@link org.obiba.opal.rest.client.magma.RestDatasource} on the default Opal project.
   *
   * @param project
   * @return
   */
  private RestDatasource getDatasource(String project) {
    if(!datasourceMap.containsKey(project)) {
      DatasourceFactory factory = new RestDatasourceFactory(project, getOpalUrl(), getOpalUsername(), getOpalPassword(),
          project);
      RestDatasource datasource = (RestDatasource) factory.create();
      Initialisables.initialise(datasource);
      datasourceMap.put(project, datasource);
    }
    return datasourceMap.get(project);
  }

  private String getOpalUrl() {
    return opalPropertyResolver.getProperty("url");
  }

  private String getOpalUsername() {
    return opalPropertyResolver.getProperty("username");
  }

  private String getOpalPassword() {
    return opalPropertyResolver.getProperty("password");
  }

}
