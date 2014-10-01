/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import org.obiba.mica.search.queries.*;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.obiba.mica.web.model.MicaSearch.JoinQueryDto;
import static org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;

@Component
public class JoinQueryExecutor {

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  public enum QueryType {
    VARIABLE,
    DATASET,
    STUDY,
    NETWORK
  }

  @Inject
  private VariableQuery variableQuery;

  @Inject
  private DatasetQuery datasetQuery;

  @Inject
  private StudyQuery studyQuery;

  @Inject
  private NetworkQuery networkQuery;

  public MicaSearch.JoinQueryResultDto query(int from, int size) throws IOException {
    variableQuery.query(from, size);
    datasetQuery.query(from, size);
    studyQuery.query(from, size);
    networkQuery.query(from, size);

    return JoinQueryResultDto.newBuilder().setVariableResultDto(variableQuery.getResultQuery())
        .setDatasetResultDto(datasetQuery.getResultQuery()).setStudyResultDto(studyQuery.getResultQuery())
        .setNetworkResultDto(networkQuery.getResultQuery()).build();
  }

  public JoinQueryResultDto query(QueryType type, JoinQueryDto joinQueryDto) throws IOException {
    variableQuery.initialize(joinQueryDto.hasVariableQueryDto() ? joinQueryDto.getVariableQueryDto() : null);
    datasetQuery.initialize(joinQueryDto.hasDatasetQueryDto() ? joinQueryDto.getDatasetQueryDto() : null);
    studyQuery.initialize(joinQueryDto.hasStudyQueryDto() ? joinQueryDto.getStudyQueryDto() : null);
    networkQuery.initialize(joinQueryDto.hasNetworkQueryDto() ? joinQueryDto.getNetworkQueryDto() : null);

    switch(type) {
      case VARIABLE:
        execute(variableQuery, studyQuery, datasetQuery, networkQuery);
        break;
      case DATASET:
        execute(datasetQuery, variableQuery, studyQuery, networkQuery);
        getDatasetCountStats();
        break;
      case STUDY:
        execute(studyQuery, variableQuery, datasetQuery, networkQuery);
        getStudyCountStats();
        break;
      case NETWORK:
        execute(networkQuery, variableQuery, datasetQuery, studyQuery);
        break;
    }

    JoinQueryResultDto.Builder builder = JoinQueryResultDto.newBuilder();
    if (variableQuery.getResultQuery() != null) builder.setVariableResultDto(variableQuery.getResultQuery());
    if (datasetQuery.getResultQuery() != null) builder.setDatasetResultDto(datasetQuery.getResultQuery());
    if (studyQuery.getResultQuery() != null) builder.setStudyResultDto(studyQuery.getResultQuery());
    if (networkQuery.getResultQuery() != null) builder.setNetworkResultDto(networkQuery.getResultQuery());
    return builder.build();
  }

  private void getDatasetCountStats() {
//    MicaSearch.QueryResultDto resultDto = datasetQuery.getResultQuery();
//    if (resultDto == null || !resultDto.hasExtension(MicaSearch.DatasetResultDto.result)) return;
//
//    Map<String, Integer> varCounts = variableQuery.getStudyCounts();
//    Map<String, Integer> dsCounts = studyQuery.getStudyCounts();
//    Map<String, Integer> netCounts = networkQuery.getStudyCounts();
//    log.info(">>> Counts var: {}, ds: {}, net: {}", varCounts, dsCounts, netCounts);
//    MicaSearch.DatasetResultDto datasets = resultDto.getExtension(MicaSearch.DatasetResultDto.result);
////    datasets.getDatasetsList().forEach(ds -> ds.get);
  }

  private void getStudyCountStats() {
    MicaSearch.QueryResultDto resultDto = studyQuery.getResultQuery();
    if (resultDto == null || !resultDto.hasExtension(MicaSearch.StudyResultDto.result)) return;

    Map<String, Integer> varCounts = variableQuery.getStudyCounts();
    Map<String, Integer> dsCounts = datasetQuery.getStudyCounts();
    Map<String, Integer> hdsCounts = datasetQuery.getHarmonizationStudyCounts();
    Map<String, Integer> netCounts = networkQuery.getStudyCounts();
    log.info(">>> Counts var: {}, ds: {}, net: {}", varCounts, dsCounts, netCounts);
    MicaSearch.StudyResultDto studyResultDto = resultDto.getExtension(MicaSearch.StudyResultDto.result);
    studyResultDto.getSummariesList().forEach(summary -> {
      int vars= 0;
      int dats = 0;
      int hdats = 0;
      int nets = 0;
      Integer c;

      String id = summary.getId();
      c = varCounts.get(id);
      vars += c == null ? 0 : c;
      c = dsCounts.get(id);
      dats += c == null ? 0 : c;
      c = hdsCounts.get(id);
      hdats += c == null ? 0 : c;
      c = netCounts.get(id);
      nets += c == null ? 0 : c;
      log.info(">>>> {} : vars: {} dats: {} hdats: {} nets:{}", id, vars, dats, hdats, nets);
    });


  }

  private void execute(AbstractDocumentQuery docQuery, AbstractDocumentQuery... subQueries) throws IOException {
    List<AbstractDocumentQuery> queries = Arrays.asList(subQueries).stream().filter(q -> q.hasQueryFilters())
        .collect(Collectors.toList());

    List<String> studyIds = null;
    List<String> joinedStudyIds = null;
    if (queries.size() > 0) studyIds = queryStudyIds(queries);
    if(studyIds == null || studyIds.size() > 0) joinedStudyIds = docQuery.query(studyIds);
    if(joinedStudyIds != null && joinedStudyIds.size() > 0)
      queryAggragations(docQuery.hasQueryFilters() ? joinedStudyIds : studyIds, subQueries);
  }

  private void queryAggragations(List<String> studyIds, AbstractDocumentQuery... queries) throws IOException {
    for(AbstractDocumentQuery query : queries) query.queryAggrations(studyIds);
  }

  private List<String> queryStudyIds(List<AbstractDocumentQuery> queries) throws IOException {
    List<String> studyIds = queries.get(0).queryStudyIds();
    queries.subList(1, queries.size()).forEach(query -> {
      if(studyIds.size() > 0) {
        try {
          studyIds.retainAll(query.queryStudyIds());
        } catch(IOException e) {
          log.error("Failed to query study IDs '{}'", e);
        }
        if(studyIds.size() == 0) return;
      }
    });

    return studyIds;
  }

}
