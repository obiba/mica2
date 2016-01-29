package org.obiba.mica.micaConfig.service;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.domain.AggregationInfo;
import org.obiba.mica.micaConfig.domain.AggregationsConfig;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Service;

/**
 * A service to merge {@link Taxonomy}s from Opal with
 * aggregations configuration from Mica.
 */
@Service
public class AggregationsService {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private OpalService opalService;

  @NotNull
  public AggregationsConfig getAggregationsConfig() {
    // make a copy of the default aggregations
    AggregationsConfig aggregationsConfig = new AggregationsConfig(micaConfigService.getAggregationsConfig());

    // contribute with vocabularies of each taxonomy
    getOpalTaxonomies().forEach(taxo -> taxo.getVocabularies().forEach(voc -> {
      if(voc.hasTerms()) {
        AggregationInfo info = new AggregationInfo();
        info.setId("attributes-" + AttributeKey.getMapKey(voc.getName(), taxo.getName()) + "-und");
        LocalizedString title = new LocalizedString();
        voc.getTitle().forEach(title::put);
        info.setTitle(title);
        aggregationsConfig.addVariableAggregation(info);
      }
    }));

    return aggregationsConfig;
  }

  @NotNull
  private List<Taxonomy> getOpalTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }
}
