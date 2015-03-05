/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class TaxonomyAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  OpalService opalService;

  @Override
  public void refresh() {
  }

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    Optional<Vocabulary> vocabulary = getVocabulary(aggregation);

    if(vocabulary.isPresent()) {
      Optional<Term> term = getTerm(vocabulary.get(), termKey);
      if(term.isPresent()) {
        Term t = term.get();
        return MetaData.newBuilder().title(t.getTitle().get(locale)).description(t.getDescription().get(locale))
          .build();
      }
    }

    return null;
  }

  private Optional<Vocabulary> getVocabulary(String aggregation) {
    String key = aggregation.replaceAll("^attributes-", "").replaceAll("-und$", "");
    AttributeKey attrKey = AttributeKey.from(key);
    String targetTaxonomy = attrKey.hasNamespace(null) ? "Default" : attrKey.getNamespace();
    String targetVocabulary = attrKey.getName();

    return getTaxonomies().stream() //
      .filter(taxonomy -> !Strings.isNullOrEmpty(targetTaxonomy) && taxonomy.getName().equals(targetTaxonomy)) //
      .map(Taxonomy::getVocabularies) //
      .flatMap((v) -> v.stream()) //
      .filter(vocabulary -> vocabulary.getName().equals(targetVocabulary)) //
      .findFirst();
  }

  private Optional<Term> getTerm(Vocabulary vocabulary, String key) {
    if(vocabulary == null) return null;

    return vocabulary.getTerms().stream() //
      .filter(term -> term.getName().equals(key)).findFirst(); //
  }

  protected List<Taxonomy> getTaxonomies() {
    try {
      return opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }
    return Collections.emptyList();
  }

}
