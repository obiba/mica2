package org.obiba.mica.micaConfig.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.rest.client.magma.OpalJavaClient;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class OpalServiceHelper {
  private static final Logger log = LoggerFactory.getLogger(OpalServiceHelper.class);

  @Cacheable(value="opal-taxonomies", key="#opalJavaClient.newUri().build()") //opal root url as key
  public Map<String, Taxonomy> getTaxonomies(OpalJavaClient opalJavaClient) {
    log.info("Fetching opal taxonomies");
    URI uri = opalJavaClient.newUri().segment("system", "conf", "taxonomies").build();
    List<Opal.TaxonomyDto> taxonomies = opalJavaClient.getResources(Opal.TaxonomyDto.class, uri, Opal.TaxonomyDto.newBuilder());

    return taxonomies.stream().collect(Collectors.toConcurrentMap(Opal.TaxonomyDto::getName, Dtos::fromDto));
  }
}
