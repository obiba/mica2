/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.common.Strings;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class AggregationMetaDataHelper {
  private static final String ID_VOCABULARY_NAME = "id";

  private static final Logger log = LoggerFactory.getLogger(AggregationMetaDataHelper.class);


  @Inject
  PublishedStudyService publishedStudyService;

  @Cacheable(value="aggregations-metadata", key = "'study'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getStudies() {
    List<Study> studies = publishedStudyService.findAll();
    return studies.stream().collect(Collectors
      .toMap(s -> s.getId(), m -> new AggregationMetaDataProvider.LocalizedMetaData(m.getAcronym(), m.getName())));
  }

  public void addTermsToIdVocabulary(Taxonomy taxonomy, String idVocabularyName) {
    String vocabularyName = Strings.isNullOrEmpty(idVocabularyName) ? ID_VOCABULARY_NAME : idVocabularyName;
    Optional<Vocabulary> idVocabulary = taxonomy.getVocabularies().stream()
      .filter(v -> v.getName().equals(vocabularyName)).findFirst();

    if (idVocabulary.isPresent()) {
      addIdTerms(idVocabulary.get());
    }
  }

  private void addIdTerms(Vocabulary idVocabulary) {
    Map<String, AggregationMetaDataProvider.LocalizedMetaData> studies = getStudies();
    studies.keySet().forEach(key ->
      idVocabulary.addTerm(createTermFromMetaData(key, studies.get(key)))
    );
  }

  private Term createTermFromMetaData(String id, AggregationMetaDataProvider.LocalizedMetaData metaData) {
    Term term = new Term(id);
    term.setTitle(metaData.getTitle());
    term.setDescription(metaData.getDescription());

    return term;
  }

}
