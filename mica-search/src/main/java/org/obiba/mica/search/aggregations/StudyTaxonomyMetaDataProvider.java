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

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

@Component
public class StudyTaxonomyMetaDataProvider implements AggregationMetaDataProvider {

  private static final Logger log = LoggerFactory.getLogger(StudyTaxonomyMetaDataProvider.class);

  @Inject
  private MicaConfigService micaConfigService;

  Map<String, Map<String, LocalizedMetaData>> cache;

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    if(!cache.containsKey(aggregation)) {
      cache.put(aggregation, getAllLocalizedMetadata(aggregation));
    }

    Map<String, LocalizedMetaData> aggs = cache.get(aggregation);

    if(aggs == null) return null;

    LocalizedMetaData md = aggs.get(termKey);

    if(md == null) return null;

    return MetaData.newBuilder().title(md.getTitle().get(locale)).description(md.getDescription().get(locale)).build();
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    if(!cache.containsKey(aggregation)) {
      cache.put(aggregation, getAllLocalizedMetadata(aggregation));
    }

    return cache.get(aggregation) != null;
  }

  @Override
  public void refresh() {
    cache = Maps.newHashMap();
  }

  private Map<String, LocalizedMetaData> getAllLocalizedMetadata(String aggregation) {
    Map<String, LocalizedMetaData> r = null;
    Taxonomy studyTaxonomy = micaConfigService.getStudyTaxonomy();
    if(studyTaxonomy.hasVocabulary(aggregation)) {
      log.debug("Found in taxonomy {} a vocabulary with name: {}", studyTaxonomy.getName(), aggregation);
      Vocabulary v = studyTaxonomy.getVocabulary(aggregation);
      if(v.hasTerms()) r = v.getTerms().stream().collect(Collectors.toMap(TaxonomyEntity::getName, t -> {
        LocalizedString title = new LocalizedString();
        title.putAll(t.getTitle());
        LocalizedString description = new LocalizedString();
        description.putAll(t.getDescription());
        return new LocalizedMetaData(title, description);
      }));
    } else {
      log.warn("Could not find in taxonomy {} any vocabulary with name: {}", studyTaxonomy.getName(), aggregation);
    }

    return r;
  }
}
