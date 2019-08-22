/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
@Scope("prototype")
class CoverageByBucket {

  @Inject
  private TaxonomyConfigService taxonomyConfigService;

  private List<TaxonomyHeader> taxonomyHeaders = Lists.newArrayList();

  private List<VocabularyHeader> vocabularyHeaders = Lists.newArrayList();

  private List<TermHeader> termHeaders = Lists.newArrayList();

  private List<BucketRow> bucketRows = Lists.newArrayList();

  private int termsCount = 0;

  private String locale;

  CoverageByBucket() { }

  public List<TaxonomyHeader> getTaxonomyHeaders() {
    return taxonomyHeaders;
  }

  public List<VocabularyHeader> getVocabularyHeaders() {
    return vocabularyHeaders;
  }

  public List<TermHeader> getTermHeaders() {
    return termHeaders;
  }

  public List<BucketRow> getBucketRows() {
    return bucketRows;
  }

  public String getLocale() {
    return locale;
  }

  void initialize(MicaSearch.TaxonomiesCoverageDto coverage) {
    initializeLocale(coverage);
    for(MicaSearch.TaxonomyCoverageDto taxonomyCoverage : coverage.getTaxonomiesList()) {
      int taxonomyTermsCount = 0;
      for(MicaSearch.VocabularyCoverageDto vocabularyCoverage : taxonomyCoverage.getVocabulariesList()) {
        for(MicaSearch.TermCoverageDto termCoverage : vocabularyCoverage.getTermsList()) {
          termsCount++;
          taxonomyTermsCount++;
          termHeaders.add(new TermHeader(taxonomyCoverage, vocabularyCoverage, termCoverage));
          for(MicaSearch.BucketCoverageDto bucketCoverage : termCoverage.getBucketsList()) {
            BucketRow row = findBucketRow(bucketCoverage);
            if(row == null) {
              row = new BucketRow(bucketCoverage);
              bucketRows.add(row);
            }
            row.setHits(termsCount - 1, bucketCoverage);
          }
        }
        vocabularyHeaders.add(new VocabularyHeader(taxonomyCoverage, vocabularyCoverage));
      }
      taxonomyHeaders.add(new TaxonomyHeader(taxonomyCoverage, taxonomyTermsCount));
    }

    bucketRows.forEach(bucketRow -> {
      bucketRow.fillHits(termsCount);
    });
    bucketRows = bucketRows.stream().sorted().collect(Collectors.toList());
  }

  //
  // Private methods
  //

  private void initializeLocale(MicaSearch.TaxonomiesCoverageDto coverage) {
    if(coverage.getTaxonomiesCount() > 0) {
      Mica.LocalizedStringDto localized = coverage.getTaxonomies(0).getTaxonomy().getTitles(0);
      locale = localized.getLang();
    }
  }

  @Nullable
  private BucketRow findBucketRow(MicaSearch.BucketCoverageDto bucketCoverage) {
    for(BucketRow row : bucketRows) {
      if(row.field.equals(bucketCoverage.getField()) && row.value.equals(bucketCoverage.getValue())) return row;
    }
    return null;
  }

  class TaxonomyHeader {

    final Mica.TaxonomyEntityDto taxonomy;

    final int hits;

    final int termsCount;

    TaxonomyHeader(MicaSearch.TaxonomyCoverageDto taxonomyCoverage, int termsCount) {
      taxonomy = taxonomyCoverage.getTaxonomy();
      hits = taxonomyCoverage.getHits();
      this.termsCount = termsCount;
    }
  }

  class VocabularyHeader {

    final Mica.TaxonomyEntityDto taxonomy;

    final Mica.TaxonomyEntityDto vocabulary;

    final int hits;

    final int termsCount;

    VocabularyHeader(MicaSearch.TaxonomyCoverageDto taxonomyCoverage,
      MicaSearch.VocabularyCoverageDto vocabularyCoverage) {
      taxonomy = taxonomyCoverage.getTaxonomy();
      vocabulary = vocabularyCoverage.getVocabulary();
      termsCount = vocabularyCoverage.getTermsCount();
      hits = vocabularyCoverage.getHits();
    }
  }

  class TermHeader {

    final Mica.TaxonomyEntityDto taxonomy;

    final Mica.TaxonomyEntityDto vocabulary;

    final Mica.TaxonomyEntityDto term;

    final int hits;

    TermHeader(MicaSearch.TaxonomyCoverageDto taxonomyCoverage, MicaSearch.VocabularyCoverageDto vocabularyCoverage,
      MicaSearch.TermCoverageDto termCoverage) {
      taxonomy = taxonomyCoverage.getTaxonomy();
      vocabulary = vocabularyCoverage.getVocabulary();
      term = termCoverage.getTerm();
      hits = termCoverage.getHits();
    }
  }

  class BucketRow implements Comparable<BucketRow> {

    final String field;

    final String fieldTitle;

    final String value;

    final String title;

    final String description;

    final String className;

    final String start;

    final String end;

    final String sortField;

    final List<Integer> hits = Lists.newArrayList();

    final List<Integer> counts = Lists.newArrayList();

    BucketRow(MicaSearch.BucketCoverageDto bucketCoverage) {
      Taxonomy variableTaxonomy = taxonomyConfigService.findByTarget(TaxonomyTarget.VARIABLE);

      field = bucketCoverage.getField();
      if(variableTaxonomy.hasVocabulary(field) &&
        variableTaxonomy.getVocabulary(field).getTitle().containsKey(locale)) {
        fieldTitle = variableTaxonomy.getVocabulary(field).getTitle().get(locale);
      } else {
        fieldTitle = field;
      }
      value = bucketCoverage.getValue();
      title = bucketCoverage.hasTitle() ? bucketCoverage.getTitle() : value;
      description = bucketCoverage.hasDescription() ? bucketCoverage.getDescription() : "";
      className = bucketCoverage.hasClassName() ? bucketCoverage.getClassName() : "";
      start = bucketCoverage.hasStart() ? bucketCoverage.getStart() : "";
      end = bucketCoverage.hasEnd() ? bucketCoverage.getEnd() : "";
      sortField = bucketCoverage.hasSortField() ? bucketCoverage.getSortField() : "";
    }

    public void setHits(int termPosition, MicaSearch.BucketCoverageDto bucketCoverage) {
      // ensure empty hits are filled-in
      fillHits(termPosition);
      hits.add(bucketCoverage.getHits());
      counts.add(bucketCoverage.getCount());
    }

    public void fillHits(int count) {
      for(int i = hits.size(); i < count; i++) {
        hits.add(0);
        counts.add(0);
      }
    }

    @Override
    public int compareTo(BucketRow o) {
      if(o == null) return 1;
      return field.equals(o.field) ? value.compareTo(o.value) : field.compareTo(o.field);
    }

    @Override
    public boolean equals(Object obj) {
      if(obj == null) return false;
      if(!BucketRow.class.isAssignableFrom(obj.getClass())) return false;
      BucketRow otherRow = (BucketRow) obj;
      return field.equals(otherRow.field) && value.equals(otherRow.value);
    }

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 53 * hash + field.hashCode();
      hash = 53 * hash + value.hashCode();
      return hash;
    }
  }

}
