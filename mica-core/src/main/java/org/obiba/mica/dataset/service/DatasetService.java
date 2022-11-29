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
import com.google.protobuf.GeneratedMessage;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import org.obiba.magma.*;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.StudyTableSourceServiceRegistry;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.spi.dataset.StudyTableSource;
import org.obiba.mica.study.service.StudyService;
import org.obiba.opal.web.model.Math;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
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
  public abstract Iterable<DatasetVariable> getDatasetVariables(T dataset) throws NoSuchValueTableException;

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

  protected StudyTableSource getStudyTableSource(@NotNull BaseStudyTable studyTable) {
    return studyTableSourceServiceRegistry.makeSource(getStudyService().findDraft(studyTable.getStudyId()), studyTable.getSourceURN());
  }

  protected Iterable<DatasetVariable> wrappedGetDatasetVariables(T dataset) {
    try {
      return getDatasetVariables(dataset);
    } catch (NoSuchValueTableException e) {
      throw new InvalidDatasetException(e);
    } catch (MagmaRuntimeException e) {
      throw new DatasourceNotAvailableException(e);
    }
  }

  /**
   * Helper class to serialize protobuf object extension.
   */
  public static class SummaryStatisticsWrapper implements Serializable {
    @IgnoreSizeOf
    private org.obiba.opal.web.model.Math.SummaryStatisticsDto summary;

    public SummaryStatisticsWrapper(Math.SummaryStatisticsDto summary) {
      this.summary = summary;
    }

    public Math.SummaryStatisticsDto getWrappedDto() {
      return summary;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      summary = (Math.SummaryStatisticsDto)in.readObject();
      GeneratedMessage ext = (GeneratedMessage)in.readObject();

      if (ext == null) return;

      Math.SummaryStatisticsDto.Builder builder = summary.toBuilder();

      if(ext instanceof Math.CategoricalSummaryDto)
        builder.setExtension(Math.CategoricalSummaryDto.categorical, (Math.CategoricalSummaryDto) ext);
      else if(ext instanceof Math.ContinuousSummaryDto)
        builder.setExtension(Math.ContinuousSummaryDto.continuous, (Math.ContinuousSummaryDto) ext);
      else if(ext instanceof Math.DefaultSummaryDto)
        builder.setExtension(Math.DefaultSummaryDto.defaultSummary, (Math.DefaultSummaryDto) ext);
      else if(ext instanceof Math.TextSummaryDto)
        builder.setExtension(Math.TextSummaryDto.textSummary, (Math.TextSummaryDto) ext);
      else if(ext instanceof Math.GeoSummaryDto)
        builder.setExtension(Math.GeoSummaryDto.geoSummary, (Math.GeoSummaryDto) ext);
      else if(ext instanceof Math.BinarySummaryDto)
        builder.setExtension(Math.BinarySummaryDto.binarySummary, (Math.BinarySummaryDto) ext);

      summary = builder.build();
    }

    private void writeObject(java.io.ObjectOutputStream stream)
      throws IOException {
      GeneratedMessage ext = null;

      Math.SummaryStatisticsDto.Builder builder = Math.SummaryStatisticsDto.newBuilder(summary);

      if(summary.hasExtension(Math.CategoricalSummaryDto.categorical)) {
        ext = summary.getExtension(Math.CategoricalSummaryDto.categorical);
        builder.clearExtension(Math.CategoricalSummaryDto.categorical);
      } else if(summary.hasExtension(Math.ContinuousSummaryDto.continuous)) {
        ext = summary.getExtension(Math.ContinuousSummaryDto.continuous);
        builder.clearExtension(Math.ContinuousSummaryDto.continuous);
      } else if(summary.hasExtension(Math.DefaultSummaryDto.defaultSummary)) {
        ext = summary.getExtension(Math.DefaultSummaryDto.defaultSummary);
        builder.clearExtension(Math.DefaultSummaryDto.defaultSummary);
      } else if(summary.hasExtension(Math.TextSummaryDto.textSummary)) {
        ext = summary.getExtension(Math.TextSummaryDto.textSummary);
        builder.clearExtension(Math.TextSummaryDto.textSummary);
      } else if(summary.hasExtension(Math.GeoSummaryDto.geoSummary)) {
        ext = summary.getExtension(Math.GeoSummaryDto.geoSummary);
        builder.clearExtension(Math.GeoSummaryDto.geoSummary);
      } else if(summary.hasExtension(Math.BinarySummaryDto.binarySummary)) {
        ext = summary.getExtension(Math.BinarySummaryDto.binarySummary);
        builder.clearExtension(Math.BinarySummaryDto.binarySummary);
      }

      stream.writeObject(builder.build());
      stream.writeObject(ext);
    }
  }
}
