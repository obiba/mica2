/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Strings;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public abstract class ConfigurationTaxonomyMetaDataProvider implements AggregationMetaDataProvider {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationTaxonomyMetaDataProvider.class);

  @Inject
  protected TaxonomiesService taxonomiesService;

  Map<String, Map<String, LocalizedMetaData>> cache;

  protected abstract Taxonomy getTaxonomy();

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    if(!containsAggregation(aggregation)) return null;

    Map<String, LocalizedMetaData> aggs = cache.get(aggregation);
    if(aggs == null) return null;

    LocalizedMetaData md = aggs.get(termKey);
    if(md == null) return null;

    return MetaData.newBuilder()
        .title(md.getTitle().get(locale))
        .description(md.getDescription().get(locale))
        .className(md.getClassName())
        .start(md.getStart())
        .end(md.getEnd())
        .sortField(md.getSortField())
        .build();
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
    Taxonomy taxonomy = getTaxonomy();
    Vocabulary v = taxonomyVocabularyWithAlias(taxonomy, aggregation);

    if(v == null && taxonomy.hasVocabulary(aggregation)) {
      log.debug("Found in taxonomy {} a vocabulary with name: {}", taxonomy.getName(), aggregation);
      v = taxonomy.getVocabulary(aggregation);
    }

    return populateMetadataMap(v);
  }

  private Map<String, LocalizedMetaData> populateMetadataMap(Vocabulary vocabulary) {
    Map<String, LocalizedMetaData> metaData = Maps.newHashMap();

    if (vocabulary != null && vocabulary.hasTerms()) {
      vocabulary.getTerms().forEach(t -> {
        LocalizedString title = new LocalizedString();
        title.putAll(t.getTitle());
        LocalizedString description = new LocalizedString();
        description.putAll(t.getDescription());
        String className = t.getAttributeValue("className");
        if (Strings.isNullOrEmpty(className)) {
          className = t.getClass().getSimpleName();
        }
        LocalizedMetaData md = new LocalizedMetaData(title, description, className, t.getAttributeValue("start"),
          t.getAttributeValue("end"), t.getAttributeValue("sortField"));
        metaData.put(t.getName(), md);
        metaData.put(t.getName().toLowerCase(), md);
      });
    }

    return metaData;
  }

  private Vocabulary taxonomyVocabularyWithAlias(Taxonomy taxonomy, String aggregation) {
    if (taxonomy.getVocabularies() == null) {
      return null;
    } else {
      List<Vocabulary> filtered = taxonomy.getVocabularies().stream()
        .filter(v -> aggregation != null && aggregation.equals(v.getAttributeValue("alias")))
        .collect(Collectors.toList());

      log.debug("Found in taxonomy {} {} vocabulary with alias: {}", taxonomy.getName(), filtered, aggregation);

      return filtered.isEmpty() ? null : filtered.get(0);
    }
  }
}
