/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.variable.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EsPublishedDatasetVariableService extends AbstractIdentifiedDocumentService<DatasetVariable>
  implements PublishedDatasetVariableService {

  private static final Logger log = LoggerFactory.getLogger(EsPublishedDatasetVariableService.class);

  private static final String VARIABLE_PUBLISHED = "variable-published";

  private static final String VARIABLE_TYPE = "Variable";

  private static final String STUDY_ID_FIELD = "studyId";

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Override
  public long getCountByStudyId(String studyId) {
    Searcher.DocumentResults results = executeCountQuery(buildStudyFilteredQuery(studyId));
    return results == null ? 0 : results.getTotal();
  }

  @Override
  public long countVariables(String rql) {
    return getCountByRql(String.format("variable(%s)", rql));
  }

  @Override
  public long getHarmonizedCount() {
    try {
      return searcher.count(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE, "").getTotal();
    } catch (RuntimeException e) {
      return 0;
    }
  }

  public Map<String, Long> getCountByStudyIds(List<String> studyIds) {
    Searcher.DocumentResults results = executeCountQuery(buildStudiesFilteredQuery(studyIds));
    if (results == null) return studyIds.stream().collect(Collectors.toMap(s -> s, s -> 0L));

    Map<String, Long> aggs = results.getAggregation(STUDY_ID_FIELD);
    return studyIds.stream().collect(Collectors.toMap(s -> s, s -> aggs.getOrDefault(s, 0L)));
  }

  @Override
  public long getCountByVariableType(DatasetVariable.Type type) {
    return getCountByRql(String.format("variable(in(variableType,%s))", type));
  }

  @Override
  protected DatasetVariable processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), DatasetVariable.class);
  }

  @Override
  public List<String> getSuggestionFields() {
    return Lists.newArrayList("name.analyzed", "attributes.label.%s.analyzed");
  }

  @Override
  protected String getIndexName() {
    return VARIABLE_PUBLISHED;
  }

  @Override
  protected String getType() {
    return VARIABLE_TYPE;
  }

  private String buildStudiesFilteredQuery(List<String> ids) {
    return String.format("variable(in(%s,(%s)),aggregate(%s))", STUDY_ID_FIELD, Joiner.on(",").join(ids), STUDY_ID_FIELD);
  }

  private String buildStudyFilteredQuery(String id) {
    return String.format("variable(eq(%s,%s))", STUDY_ID_FIELD, id);
  }

  private Searcher.DocumentResults executeCountQuery(String rql) {
    try {
      Searcher.IdFilter idFilter = getAccessibleIdFilter();
      return searcher.count(getIndexName(), getType(), rql, idFilter);
    } catch (Exception e) {
      return null; //ignoring
    }
  }

  protected String getStudyIdField() {
    return "studyId";
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {

      @Override
      public String getField() {
        return "datasetId";
      }

      @Override
      public Collection<String> getValues() {
        List<String> ids = collectedDatasetService.findPublishedIds().stream()
          .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
        ids.addAll(harmonizedDatasetService.findPublishedIds().stream()
          .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList()));
        return ids;
      }
    };
  }
}
