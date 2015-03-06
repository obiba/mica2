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

import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.config.StudiesConfiguration;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;


@Component
public class StudyTaxonomyMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  private StudiesConfiguration studiesConfiguration;

  @Override
  public MetaData getTitle(String aggregation, String termKey, String locale) {
    Optional<Term> term = studiesConfiguration.getVocabularies().stream() //
      .filter(v -> v.getName().equals(aggregation)) //
      .map(Vocabulary::getTerms) //
      .flatMap((v) -> v.stream()) //
      .filter(t -> t.getName().equals(termKey)) //
      .findFirst(); //

    MetaData metaData = null;

    if (term.isPresent()) {
      Term t = term.get();
      metaData = MetaData.newBuilder().title(t.getTitle().get(locale)).description(t.getDescription().get(locale))
        .build();
    }

    return metaData;
  }

  @Override
  public void refresh() {
  }
}
