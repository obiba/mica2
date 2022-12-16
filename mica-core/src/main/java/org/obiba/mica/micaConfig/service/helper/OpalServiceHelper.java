/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import com.google.common.base.Strings;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.rest.client.magma.OpalJavaClient;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.model.Search;
import org.obiba.opal.web.taxonomy.Dtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpalServiceHelper {

  private static final Logger log = LoggerFactory.getLogger(OpalServiceHelper.class);

  public Map<String, Taxonomy> getTaxonomies(OpalJavaClient opalJavaClient) {
    log.info("Fetching opal taxonomies");
    URI uri = opalJavaClient.newUri().segment("system", "conf", "taxonomies").build();
    List<Opal.TaxonomyDto> taxonomies = opalJavaClient
      .getResources(Opal.TaxonomyDto.class, uri, Opal.TaxonomyDto.newBuilder());
    return taxonomies.stream()
      .map(Dtos::fromDto).collect(Collectors.toConcurrentMap(TaxonomyEntity::getName, taxonomy -> taxonomy));
  }

  public Search.EntitiesResultDto getEntitiesCount(OpalJavaClient opalJavaClient, String query, String entityType) {
    log.info("Fetching opal entities count");
    log.debug("  Entities query: {}", query);
    URI uri = opalJavaClient.newUri().segment("datasources", "entities", "_count")
      .query("query", query)
      .query("type", Strings.isNullOrEmpty(entityType) ? "Participant" : entityType).build();
    Search.EntitiesResultDto result = opalJavaClient.getResource(Search.EntitiesResultDto.class, uri, Search.EntitiesResultDto.newBuilder());
    return result;
  }
}
