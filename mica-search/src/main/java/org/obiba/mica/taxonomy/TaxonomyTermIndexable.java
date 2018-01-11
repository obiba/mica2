/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class TaxonomyTermIndexable extends TaxonomyEntityIndexable<Term> {

  private final String taxonomyName;

  private final String vocabularyName;

  private final Term term;

  public TaxonomyTermIndexable(TaxonomyTarget target, Taxonomy taxonomy, Vocabulary vocabulary, Term term) {
    super(target);
    taxonomyName = taxonomy.getName();
    vocabularyName = vocabulary.getName();
    this.term = term;
  }

  @Override
  public String getId() {
    return TaxonomyResolver.asId(taxonomyName, vocabularyName, getName());
  }

  @Override
  public String getMappingName() {
    return Indexer.TAXONOMY_TERM_TYPE;
  }

  @Override
  public String getParentId() {
    return TaxonomyResolver.asId(taxonomyName, vocabularyName);
  }

  public String getTaxonomyName() {
    return taxonomyName;
  }

  public String getVocabularyName() {
    return vocabularyName;
  }

  @Override
  protected Term getTaxonomyEntity() {
    return term;
  }
}

