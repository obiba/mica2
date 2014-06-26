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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.magma.Datasource;
import org.obiba.magma.DatasourceFactory;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.Variable;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.domain.Dataset;
import org.obiba.mica.repository.DatasetRepository;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * {@link org.obiba.mica.domain.Dataset} management service.
 */
@Service
@Validated
public class DatasetService implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

  @Inject
  private DatasetRepository datasetRepository;

  private RelaxedPropertyResolver opalPropertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    opalPropertyResolver = new RelaxedPropertyResolver(environment, "opal.");
  }

  public void save(@NotNull Dataset dataset) {
    datasetRepository.save(dataset);
  }

  @NotNull
  public Dataset findById(@NotNull String id) throws NoSuchStudyException {
    Dataset dataset = datasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  public Iterable<Variable> getVariables(String name) {
    try {
      Datasource datasource = getDatasource();
      Initialisables.initialise(datasource);
      return datasource.getValueTable(name).getVariables();
    } catch(NoSuchValueTableException e) {
      throw NoSuchDatasetException.withName(name);
    } catch(Exception e) {
      log.error("Unable to connect to Opal: {}", e.getMessage(), e);
      throw NoSuchDatasetException.withName(name);
    }
  }

  private Datasource getDatasource() {
    String name = getRemoteDatasource();
    DatasourceFactory factory = new RestDatasourceFactory(name, getOpalUrl(),
        getOpalUsername(), getOpalPassword(), name);
    return factory.create();
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

  private String getRemoteDatasource() {
    return opalPropertyResolver.getProperty("project");
  }

}
