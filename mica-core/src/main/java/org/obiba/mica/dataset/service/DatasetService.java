/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import javax.validation.constraints.NotNull;

import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.Variable;
import org.obiba.mica.dataset.DatasourceRegistry;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.study.StudyService;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * {@link org.obiba.mica.dataset.domain.Dataset} management service.
 */

public abstract class DatasetService<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

  /**
   * Get all {@link org.obiba.mica.dataset.domain.DatasetVariable}s from a {@link org.obiba.mica.dataset.domain.Dataset}.
   * @param dataset
   * @return
   */
  public abstract Iterable<DatasetVariable> getDatasetVariables(T dataset);

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable} from a {@link org.obiba.mica.dataset.domain.Dataset}.
   * @param dataset
   * @param name
   * @return
   */
  public abstract DatasetVariable getDatasetVariable(T dataset, String name);

  /**
   * Get the {@link org.obiba.opal.web.model.Magma.TableDto} of the {@link org.obiba.mica.dataset.domain.Dataset} identified by its id.
   *
   * @param dataset
   * @return
   */
  @NotNull
  protected abstract RestValueTable getTable(@NotNull T dataset)
      throws NoSuchDatasetException, NoSuchValueTableException;

  protected abstract StudyService getStudyService();

  protected abstract DatasourceRegistry getDatasourceRegistry();

  protected abstract EventBus getEventBus();

  /**
   * Get the variables of the {@link org.obiba.mica.dataset.domain.Dataset} identified by its id.
   *
   * @param dataset
   * @return
   * @throws NoSuchDatasetException
   */
  protected Iterable<Variable> getVariables(@NotNull T dataset) throws NoSuchDatasetException {
    return getTable(dataset).getVariables();
  }

  /**
   * Get the {@link org.obiba.magma.VariableValueSource} (proxy to the {@link org.obiba.magma.Variable} of
   * the {@link org.obiba.mica.dataset.domain.Dataset} identified by its id.
   *
   * @param dataset
   * @param variableName
   * @return
   * @throws NoSuchDatasetException
   */
  protected RestValueTable.RestVariableValueSource getVariableValueSource(@NotNull T dataset, String variableName)
      throws NoSuchDatasetException {
    return (RestValueTable.RestVariableValueSource) getTable(dataset).getVariableValueSource(variableName);
  }

  public Magma.TableDto getTableDto(@NotNull T dataset) {
    return getTable(dataset).getTableDto();
  }

  public Magma.VariableDto getVariable(@NotNull T dataset, String variableName) {
    return getVariableValueSource(dataset, variableName).getVariableDto();
  }

  public Math.SummaryStatisticsDto getVariableSummary(@NotNull T dataset, String variableName) {
    return getVariableValueSource(dataset, variableName).getSummary();
  }

  public Search.QueryResultDto getVariableFacet(@NotNull T dataset, String variableName) {
    return getVariableValueSource(dataset, variableName).getFacet();
  }

  public Search.QueryResultDto getFacets(@NotNull T dataset, Search.QueryTermsDto query) {
    return getTable(dataset).getFacets(query);
  }

  /**
   * Callback that can be used to make any operations on a {@link org.obiba.opal.rest.client.magma.RestDatasource}
   *
   * @param <R>
   */
  public interface DatasourceCallback<R> {
    R doWithDatasource(RestDatasource datasource);
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll() {
    getEventBus().post(new IndexDatasetsEvent());
  }

  /**
   * Execute the callback on the given datasource.
   *
   * @param datasource
   * @param callback
   * @param <R>
   * @return
   */
  protected <R> R execute(RestDatasource datasource, DatasourceCallback<R> callback) {
    return callback.doWithDatasource(datasource);
  }

  protected RestDatasource getDatasource(@NotNull StudyTable studyTable) {
    String opalUrl = getStudyService().findDraftStudy(studyTable.getStudyId()).getOpal();
    return getDatasourceRegistry().getDatasource(opalUrl, studyTable.getProject());
  }

  protected RestDatasource getDatasource(@NotNull String project) {
    return getDatasourceRegistry().getDatasource(project);
  }

}
