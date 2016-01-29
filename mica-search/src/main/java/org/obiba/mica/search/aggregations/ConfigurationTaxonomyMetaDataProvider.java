/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public abstract class ConfigurationTaxonomyMetaDataProvider implements AggregationMetaDataProvider {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationTaxonomyMetaDataProvider.class);

  @Inject
  protected MicaConfigService micaConfigService;

  Map<String, Map<String, LocalizedMetaData>> cache;

  protected abstract Taxonomy getTaxonomy();

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    if(!containsAggregation(aggregation)) return null;

    Map<String, LocalizedMetaData> aggs = cache.get(aggregation);
    if(aggs == null) return null;

    LocalizedMetaData md = aggs.get(termKey);
    if(md == null) return null;

    return MetaData.newBuilder().title(md.getTitle().get(locale)).description(md.getDescription().get(locale)).build();
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    if(!cache.containsKey(aggregation)) {
      Map<String, LocalizedMetaData> localizedMetadataMap = getAllLocalizedMetadata(aggregation);
      if(localizedMetadataMap != null && !localizedMetadataMap.isEmpty()) cache.put(aggregation, localizedMetadataMap);
    }

    return cache.get(aggregation) != null;
  }

  @Override
  public void refresh() {
    cache = Maps.newHashMap();
  }

  private Map<String, LocalizedMetaData> getAllLocalizedMetadata(String aggregation) {
    Map<String, LocalizedMetaData> r = Maps.newHashMap();
    Taxonomy taxonomy = getTaxonomy();
    if(taxonomy.hasVocabulary(aggregation)) {
      log.debug("Found in taxonomy {} a vocabulary with name: {}", taxonomy.getName(), aggregation);
      Vocabulary v = taxonomy.getVocabulary(aggregation);
      if(v.hasTerms()) {
        v.getTerms().forEach(t -> {
          LocalizedString title = new LocalizedString();
          title.putAll(t.getTitle());
          LocalizedString description = new LocalizedString();
          description.putAll(t.getDescription());
          LocalizedMetaData md = new LocalizedMetaData(title, description);
          r.put(t.getName(), md);
          r.put(t.getName().toLowerCase(), md);
        });
      }
    }
    return r;
  }
}
