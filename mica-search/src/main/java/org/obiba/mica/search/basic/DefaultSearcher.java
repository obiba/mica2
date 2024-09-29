/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic;

import org.jetbrains.annotations.Nullable;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.EmptyQuery;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class DefaultSearcher implements Searcher {

  private StudyRepository studyRepository;

  public void setStudyRepository(StudyRepository studyRepository) {
    this.studyRepository = studyRepository;
  }

  @Override
  public JoinQuery makeJoinQuery(String rql) {
    return new EmptyJoinQuery();
  }

  @Override
  public Query makeQuery(String rql) {
    return new EmptyQuery();
  }

  @Override
  public Query andQuery(Query... queries) {
    return new EmptyQuery();
  }

  @Override
  public DocumentResults find(String indexName, String type, String rql, IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults count(String indexName, String type, String rql, IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName) {
    return List.of();
  }

  @Override
  public InputStream getDocumentById(String indexName, String type, String id) {
    return null;
  }

  @Override
  public InputStream getDocumentByClassName(String indexName, String type, Class clazz, String id) {
    return null;
  }

  @Override
  public DocumentResults getDocumentsByClassName(String indexName, String type, Class clazz, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    if (DefaultIndexer.DRAFT_STUDY_INDEX.equals(indexName)) {
      // Calculate page number based on offset and limit
      int page = from / limit;
      Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
      // TODO ids filter
      final long total = studyRepository.count();
      final List<Study> studies = studyRepository.findAll(PageRequest.of(page, limit, sortRequest)).getContent();
      return new StudyDocumentResults(total, studies);
    }
    return new EmptyDocumentResults();
  }

  @Override
  public long countDocumentsWithField(String indexName, String type, String field) {
    return 0;
  }

  @Override
  public DocumentResults query(String indexName, String type, Query query, QueryScope scope, List<String> mandatorySourceFields, Properties aggregationProperties, @Nullable IdFilter idFilter) throws IOException {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults aggregate(String indexName, String type, Query query, Properties aggregationProperties, IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, @Nullable IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, Map<String, Properties> subAggregationProperties, @Nullable IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public Map<Object, Object> harmonizationStatusAggregation(String datasetId, int size, String aggregationFieldName, String statusFieldName) {
    return Map.of();
  }

  private static class StudyDocumentResult implements DocumentResult {
    private final Study std;

    public StudyDocumentResult(Study std) {
      this.std = std;
    }

    @Override
    public String getId() {
      return std.getId();
    }

    @Override
    public boolean hasObject() {
      return true;
    }

    @Override
    public Object getObject() {
      return std;
    }

    @Override
    public boolean hasSource() {
      return false;
    }

    @Override
    public Map<String, Object> getSource() {
      return null;
    }

    @Override
    public InputStream getSourceInputStream() {
      return null;
    }

    @Override
    public String getClassName() {
      return "";
    }
  }

  private static class StudyDocumentResults implements DocumentResults {
    private final long total;
    private final List<Study> studies;

    public StudyDocumentResults(long total, List<Study> studies) {
      this.total = total;
      this.studies = studies;
    }

    @Override
    public long getTotal() {
      return total;
    }

    @Override
    public List<DocumentResult> getDocuments() {
      return studies.stream().map(StudyDocumentResult::new).collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getAggregation(String field) {
      return Map.of();
    }

    @Override
    public List<DocumentAggregation> getAggregations() {
      return List.of();
    }
  }
}
