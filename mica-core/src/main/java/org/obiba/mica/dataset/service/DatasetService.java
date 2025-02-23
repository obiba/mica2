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

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.SecurityUtils;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.StudyTableSourceServiceRegistry;
import org.obiba.mica.core.support.DatasetInferredAttributesCollector;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.domain.SummaryStatisticsAccessPolicy;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.spi.tables.StudyTableSource;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * {@link org.obiba.mica.dataset.domain.Dataset} management service.
 */

public abstract class DatasetService<T extends Dataset, T1 extends EntityState> extends AbstractGitPersistableService<T1, T> {

  private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

  @Inject
  private StudyTableSourceServiceRegistry studyTableSourceServiceRegistry;

  /**
   * Get all {@link org.obiba.mica.dataset.domain.DatasetVariable}s from a {@link org.obiba.mica.dataset.domain.Dataset}.
   *
   * @param dataset
   * @return
   */
  public abstract Iterable<DatasetVariable> getDatasetVariables(T dataset, @Nullable DatasetInferredAttributesCollector collector) throws NoSuchValueTableException;

  /**
   * Get the {@link org.obiba.mica.dataset.domain.DatasetVariable} from a {@link org.obiba.mica.dataset.domain.Dataset}.
   *
   * @param dataset
   * @param name
   * @return
   */
  public abstract DatasetVariable getDatasetVariable(T dataset, String name)
      throws NoSuchValueTableException, NoSuchVariableException;

  protected abstract ValueTable getValueTable(@NotNull T dataset) throws NoSuchValueTableException;

  protected abstract StudyService getStudyService();

  protected abstract NetworkService getNetworkService();

  protected abstract OpalService getOpalService();

  protected abstract EventBus getEventBus();

  protected abstract MicaConfig getMicaConfig();

  /**
   * Apply summary statistics visibility policy.
   *
   * @param summary
   * @return
   */
  public Mica.DatasetVariableAggregationDto getFilteredVariableSummary(Mica.DatasetVariableAggregationDto summary) {
    SummaryStatisticsAccessPolicy policy = getMicaConfig().getSummaryStatisticsAccessPolicy();
    if (summary == null || policy.equals(SummaryStatisticsAccessPolicy.OPEN_ALL)) return summary;
    if (!SecurityUtils.getSubject().isAuthenticated()) {
      if (policy.equals(SummaryStatisticsAccessPolicy.OPEN_SUMMARY)) return summary;
      // strip out detailed stats because user is not authenticated
      summary = summary.toBuilder().clearFrequencies().build();
      summary = summary.toBuilder().clearStatistics().build();
    }
    return summary;
  }

  /**
   * Apply summary statistics visibility policy.
   *
   * @param summary
   * @return
   */
  public Mica.DatasetVariableAggregationsDto getFilteredVariableSummary(Mica.DatasetVariableAggregationsDto summary) {
    SummaryStatisticsAccessPolicy policy = getMicaConfig().getSummaryStatisticsAccessPolicy();
    if (summary == null || policy.equals(SummaryStatisticsAccessPolicy.OPEN_ALL)) return summary;
    if (!SecurityUtils.getSubject().isAuthenticated()) {
      if (policy.equals(SummaryStatisticsAccessPolicy.OPEN_SUMMARY)) return summary;
      // strip out detailed stats because user is not authenticated
      summary = summary.toBuilder().clearFrequencies().build();
      summary = summary.toBuilder().clearStatistics().build();
    }
    return summary;
  }

  /**
   * Find all dataset identifiers.
   *
   * @return
   */
  @NotNull
  public abstract List<String> findAllIds();

  /**
   * Find a dataset by its identifier.
   *
   * @param id
   * @return
   * @throws NoSuchDatasetException
   */
  @NotNull
  public abstract T findById(@NotNull String id) throws NoSuchDatasetException;

  @Nullable
  protected String getNextId(@Nullable LocalizedString suggested) {
    if(suggested == null) return null;
    String prefix = suggested.asUrlSafeString().toLowerCase();
    if(Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;
    try {
      findById(next);
      for(int i = 1; i <= 1000; i++) {
        next = prefix + "-" + i;
        findById(next);
      }
      return null;
    } catch(NoSuchDatasetException e) {
      return next;
    }
  }

  protected String generateDatasetId(@NotNull T dataset) {
    ensureAcronym(dataset);
    return getNextId(dataset.getAcronym());
  }

  private void ensureAcronym(@NotNull T dataset) {
    if (dataset.getAcronym() == null || dataset.getAcronym().isEmpty()) {
      dataset.setAcronym(dataset.getName().asAcronym());
    }
  }

  /**
   * Get the variables of the {@link org.obiba.mica.dataset.domain.Dataset} identified by its id.
   *
   * @param dataset
   * @return
   * @throws NoSuchDatasetException
   */
  protected Iterable<Variable> getVariables(@NotNull T dataset)
      throws NoSuchDatasetException, NoSuchValueTableException {
    return getValueTable(dataset).getVariables();
  }

  protected StudyTableSource getStudyTableSource(@NotNull T dataset, @NotNull BaseStudyTable studyTable) {
    return studyTableSourceServiceRegistry.makeStudyTableSource(dataset, getStudyService().findDraft(studyTable.getStudyId()), studyTable.getSource());
  }

  protected Iterable<DatasetVariable> wrappedGetDatasetVariables(T dataset) {
    try {
      return getDatasetVariables(dataset, new DatasetInferredAttributesCollector(null));
    } catch (NoSuchValueTableException e) {
      throw e;
    } catch (MagmaRuntimeException e) {
      throw new DatasourceNotAvailableException(e);
    }
  }

}
