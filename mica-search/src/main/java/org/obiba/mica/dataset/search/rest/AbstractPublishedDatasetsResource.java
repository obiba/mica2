/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset}s from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetsResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDatasetsResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private Searcher searcher;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  protected Mica.DatasetsDto getDatasetDtos(Class<T> clazz, int from, int limit, @Nullable String sort,
                                            @Nullable String order, final @Nullable String studyId, @Nullable String queryString) {
    Searcher.DocumentResults results = searcher.getDocumentsByClassName(Indexer.PUBLISHED_DATASET_INDEX, Indexer.DATASET_TYPE,
        clazz, from, limit, sort, order, queryString, getStudyIdFilter(studyId), getAccessibleIdsFilter(clazz));

    Mica.DatasetsDto.Builder builder = Mica.DatasetsDto.newBuilder() //
        .setTotal(Long.valueOf(results.getTotal()).intValue()) //
        .setFrom(from) //
        .setLimit(limit);

    results.getDocuments().forEach(res -> {
      try {
        builder.addDatasets(dtos.asDto(objectMapper.readValue(res.getSourceInputStream(), clazz)));
      } catch (IOException e) {
        log.error("Failed retrieving {}", clazz.getSimpleName(), e);
      }
    });

    return builder.build();
  }

  protected abstract String getStudyIdField();

  private Searcher.TermFilter getStudyIdFilter(final String studyId) {
    return new Searcher.TermFilter() {
      @Override
      public String getField() {
        return getStudyIdField();
      }

      @Override
      public String getValue() {
        return studyId;
      }
    };
  }

  private Searcher.IdFilter getAccessibleIdsFilter(final Class<T> clazz) {
    if (micaConfigService.getConfig().isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        List<String> ids;
        if ("StudyDataset".equals(clazz.getSimpleName()))
          ids = collectedDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
              .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
        else ids = harmonizedDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
            .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList());
        return ids;
      }
    };
  }

}
