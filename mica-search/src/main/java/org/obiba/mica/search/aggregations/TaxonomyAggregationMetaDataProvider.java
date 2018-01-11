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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.spi.search.support.AttributeKey;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@Component
public class TaxonomyAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  OpalService opalService;

  Map<String, Map<String, LocalizedMetaData>> cache;

  @Override
  public void refresh() {
    cache = Maps.newHashMap();
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    if (!cache.containsKey(aggregation)) {
      cache.put(aggregation, getAllLocalizedMetadata(aggregation));
    }

    return cache.get(aggregation) != null;
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    if (!cache.containsKey(aggregation)) {
      cache.put(aggregation, getAllLocalizedMetadata(aggregation));
    }

    Map<String, LocalizedMetaData> aggs = cache.get(aggregation);

    if (aggs == null) return null;

    LocalizedMetaData md = aggs.get(termKey);

    if (md == null) return  null;

    return MetaData.newBuilder()
      .title(md.getTitle().get(locale))
      .description(md.getDescription().get(locale))
      .className(md.getClassName())
      .build();
  }

  private Map<String, LocalizedMetaData> getAllLocalizedMetadata(String aggregation) {
    Optional<Vocabulary> vocabulary = getVocabulary(aggregation);

    if(vocabulary.isPresent()) {
      Map<String, LocalizedMetaData> r = Maps.newHashMap();

      for(Term t:vocabulary.get().getTerms()) {
        LocalizedString title = new LocalizedString();
        title.putAll(t.getTitle());
        LocalizedString description = new LocalizedString();
        description.putAll(t.getDescription());
        String className = t.getAttributeValue("className");
        if (Strings.isNullOrEmpty(className)) {
          className = t.getClass().getSimpleName();
        }
        if(!r.containsKey(t.getName())) {
          r.put(t.getName(), new LocalizedMetaData(title, description, className));
        }
      }

      return r;
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
      .flatMap(Collection::stream) //
      .filter(vocabulary -> vocabulary.getName().equals(targetVocabulary)) //
      .findFirst();
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
