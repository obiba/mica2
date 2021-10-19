/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.util.locale.LanguageTag;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractDocumentService<T> implements DocumentService<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentService.class);

  protected static final int MAX_SIZE = 10000;

  protected static final int MAX_PACKET_SIZE = 100;

  @Inject
  protected Searcher searcher;

  @Inject
  protected Indexer indexer;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  protected SubjectAclService subjectAclService;

  @Override
  public List<T> findAll() {
    log.debug("findAll {}", getClass());
    return executeRqlQuery(String.format("generic(limit(0,%s))", MAX_SIZE));
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                           @Nullable String queryString) {
    return find(from, limit, sort, order, studyId, queryString, null);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                           @Nullable String queryString, @Nullable List<String> fields) {
    return find(from, limit, sort, order, studyId, queryString, fields, null, null);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                           @Nullable String queryString, @Nullable List<String> fields, @Nullable List<String> excludedFields,
                           Searcher.IdFilter idFilter) {
    if (!indexExists()) return new Documents<>(0, from, limit);

    Searcher.TermFilter studyIdFilter = getStudyIdFilter(studyId);

    if (idFilter == null) {
      idFilter = getAccessibleIdFilter();
    }

    Searcher.DocumentResults results = searcher.getDocuments(getIndexName(), getType(), from, limit, sort, order, queryString, studyIdFilter, idFilter, fields, excludedFields);

    Documents<T> documents = new Documents<>(Long.valueOf(results.getTotal()).intValue(), from, limit);
    results.getDocuments().forEach(res -> {
      try {
        documents.add(processHit(res));
      } catch (IOException e) {
        log.error("Failed processing found hits.", e);
      }
    });
    return documents;
  }

  @Override
  public List<String> getSuggestionFields() {
    return Lists.newArrayList("name.%s.analyzed", "acronym.%s.analyzed");
  }

  @Override
  public List<String> suggest(int limit, String locale, String queryString) {
    return suggest(limit, locale, queryString, null, this::filterSuggestions);
  }

  @Override
  public List<String> suggest(int limit,
                              String locale,
                              String queryString,
                              List<String> suggestedFields,
                              Function<Set<String>, Set<String>> filterFunction) {

    Set<String> suggestions = Sets.newLinkedHashSet();

    if (suggestedFields == null || suggestedFields.isEmpty()) {
      suggestedFields = getSuggestionFields();
    }

    // query default fields separately otherwise we do not know which field has matched and suggestion might not be correct
    suggestedFields.forEach(df -> suggestions.addAll(searcher.suggest(getIndexName(), getType(), limit, locale, queryString, df)));

    if (filterFunction == null) {
      return new ArrayList<>(new HashSet<>(suggestions));
    }

    return Lists.newArrayList(filterFunction.apply(suggestions));
  }

  @Override
  public long getCount() {
    return getCountByRql("");
  }

  /**
   * Removes all characters that will break RQL query parsing
   *
   * @param suggestions
   * @return sanitized list of suggestions
   */
  protected Set<String> filterSuggestions(Set<String> suggestions) {
    return suggestions.stream().map(s -> s
      .replace("'s", "") // english thing that confuses RQL
      .replace("l'", "") // french thing that confuses RQL
      .replaceAll("['\",:;?!\\(\\)]", "") // chars that might confuse RQL
      .replace(" - ", " ") // isolated hyphen
      .replaceAll("(\\.\\w+)", "") // remove chars after "dot"
      .trim().replaceAll(" +", " ")) // duplicated spaces
      .collect(Collectors.toSet());
  }

  protected long getCountByRql(String rql) {
    try {
      return searcher.count(getIndexName(), getType(), rql).getTotal();
    } catch (RuntimeException e) {
      return 0;
    }
  }

  /**
   * Turns a search result input stream into document's pojo.
   */
  protected abstract T processHit(Searcher.DocumentResult res) throws IOException;

  /**
   * Get the index where the search must take place.
   */
  protected abstract String getIndexName();

  /**
   * Get the document type name.
   */
  protected abstract String getType();

  /**
   * If access check apply, get the corresponding filter.
   */
  @Nullable
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return null;
  }

  protected String getStudyIdField() {
    return "studyIds";
  }

  protected List<T> executeRqlQuery(String rql) {
    return executeQueryInternal(rql);
  }

  protected boolean isOpenAccess() {
    return micaConfigService.getConfig().isOpenAccess();
  }

  //
  // Private methods
  //

  private boolean indexExists() {
    return indexer.hasIndex(getIndexName());
  }

  private List<T> executeQueryInternal(String rql) {
    Searcher.IdFilter accessibleIdFilter = getAccessibleIdFilter();
    try {
      Searcher.DocumentResults documentResults = searcher.find(getIndexName(), getType(), rql, accessibleIdFilter);
      return processHits(documentResults);
    } catch (Exception e) {
      log.error("Query execution error [{}]", e.getMessage());
      if (log.isDebugEnabled())
        log.error("Query execution error", e);
      return Lists.newArrayList();
    }
  }

  private List<T> processHits(Searcher.DocumentResults documentResults) {

    return documentResults.getDocuments().stream()
      .map(documentResult -> {
        try {
          return processHit(documentResult);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
  }

  private Searcher.TermFilter getStudyIdFilter(@Nullable final String studyId) {
    if (studyId == null) return null;
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

  protected List<String> getLocalizedFields(String... fieldNames) {
    List<String> fields = Lists.newArrayList();
    Stream.concat(micaConfigService.getConfig().getLocalesAsString().stream(), Stream.of(LanguageTag.UNDETERMINED))
      .forEach(locale -> Arrays.stream(fieldNames)
        .forEach(f -> fields.add(f + "." + locale + ".analyzed")));
    return fields;
  }
}
