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

import javax.validation.constraints.NotNull;

import org.obiba.magma.Variable;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.obiba.mica.domain.Dataset} management service.
 */

public abstract class DatasetService {

  private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

  /**
   * Get the variables of the {@link org.obiba.mica.domain.Dataset} identified by its id.
   *
   * @param id
   * @return
   * @throws NoSuchDatasetException
   */
  public abstract Iterable<Variable> getVariables(@NotNull String id) throws NoSuchDatasetException;

  /**
   * Get the {@link org.obiba.opal.web.model.Magma.TableDto} of the {@link org.obiba.mica.domain.Dataset} identified by its id.
   *
   * @param id
   * @return
   */
  public abstract RestValueTable getTable(@NotNull String id);

  /**
   * Get the {@link org.obiba.magma.VariableValueSource} (proxy to the {@link org.obiba.magma.Variable} of
   * the {@link org.obiba.mica.domain.Dataset} identified by its id.
   *
   * @param id
   * @param variableName
   * @return
   * @throws NoSuchDatasetException
   */
  public RestValueTable.RestVariableValueSource getVariableValueSource(@NotNull String id, String variableName)
      throws NoSuchDatasetException {
    return (RestValueTable.RestVariableValueSource) getTable(id).getVariableValueSource(variableName);
  }

  public Magma.TableDto getTableDto(@NotNull String id) {
    return getTable(id).getTableDto();
  }

  public Magma.VariableDto getVariable(@NotNull String id, String variableName) {
    return getVariableValueSource(id, variableName).getVariableDto();
  }

  public Math.SummaryStatisticsDto getVariableSummary(@NotNull String id, String variableName) {
    return getVariableValueSource(id, variableName).getSummary();
  }

  public Search.QueryResultDto getVariableFacet(@NotNull String id, String variableName) {
    return getVariableValueSource(id, variableName).getFacet();
  }

  public Search.QueryResultDto getFacets(String id, Search.QueryTermsDto query) {
    return getTable(id).getFacets(query);
  }

  /**
   * Callback that can be used to make any operations on a {@link org.obiba.opal.rest.client.magma.RestDatasource}
   *
   * @param <T>
   */
  public interface DatasourceCallback<T> {
    T doWithDatasource(RestDatasource datasource);
  }

  /**
   * Execute the callback on the given datasource.
   *
   * @param datasource
   * @param callback
   * @param <T>
   * @return
   */
  protected <T> T execute(RestDatasource datasource, DatasourceCallback<T> callback) {
    return callback.doWithDatasource(datasource);
  }

}
