/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.rest.rql.RQLCriteriaOpalConverter;
import org.obiba.mica.dataset.rest.rql.RQLCriterionOpalConverter;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.web.model.DocumentDigestDtos;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.web.model.Search;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("/datasets/entities")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetsEntitiesResource {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private OpalService opalService;

  @Inject
  private DocumentDigestDtos documentDigestDtos;

  @GET
  @Path("_count")
  @Timed
  public MicaSearch.EntitiesCountDto countEntities(@QueryParam("query") String query, @QueryParam("type") @DefaultValue("Participant") String entityType) {
    RQLCriteriaOpalConverter converter = applicationContext.getBean(RQLCriteriaOpalConverter.class);
    converter.parse(query);
    Map<String, List<RQLCriterionOpalConverter>> opalConverters = converter.getCriterionConverters().stream()
        .collect(Collectors.groupingBy(c -> c.getVariableReferences().getOpal()));
    List<OpalEntitiesQuery> opalEntitiesQueries = opalConverters.keySet().stream()
        .map(opalUrl -> new OpalEntitiesQuery(entityType, opalUrl, opalConverters.get(opalUrl)))
        .collect(Collectors.toList());

    MicaSearch.EntitiesCountDto.Builder builder = MicaSearch.EntitiesCountDto.newBuilder()
        .setEntityType(entityType).setQuery(query);

    int total = -1;
    for (OpalEntitiesQuery opalEntitiesQuery : opalEntitiesQueries) {
      total = total<0 ? opalEntitiesQuery.getTotal() : 0;
      builder.addAllCounts(opalEntitiesQuery.getDatasetEntitiesCounts());
    }
    builder.setTotal(total);

    return builder.build();
  }

  private class OpalEntitiesQuery {
    private final String entityType;
    private final String opalUrl;
    private final List<RQLCriterionOpalConverter> opalConverters;
    private final Search.EntitiesResultDto opalResults;

    private OpalEntitiesQuery(String entityType, String opalUrl, List<RQLCriterionOpalConverter> opalConverters) {
      this.entityType = entityType;
      this.opalUrl = opalUrl;
      this.opalConverters = opalConverters;
      this.opalResults = opalService.getEntitiesCount(opalUrl, getOpalQuery(), entityType);
    }

    private String getOpalQuery() {
      return opalConverters.stream().map(RQLCriterionOpalConverter::getOpalQuery).collect(Collectors.joining(","));
    }

    private String getMicaQuery() {
      return opalConverters.stream().map(RQLCriterionOpalConverter::getMicaQuery).collect(Collectors.joining(","));
    }

    private int getTotal() {
      return opalResults.getTotalHits();
    }

    public String getEntityType() {
      return entityType;
    }

    public List<MicaSearch.DatasetEntitiesCountDto> getDatasetEntitiesCounts() {
      List<MicaSearch.DatasetEntitiesCountDto> counts;
      if (opalResults.getPartialResultsCount()>0)
        counts = opalResults.getPartialResultsList().stream().map(this::createDatasetEntitiesCount).collect(Collectors.toList());
      else
        counts = Lists.newArrayList(createDatasetEntitiesCount(opalResults));
      return counts;
    }

    private MicaSearch.DatasetEntitiesCountDto createDatasetEntitiesCount(Search.EntitiesResultDto opalResult) {
      MicaSearch.DatasetEntitiesCountDto.Builder builder = MicaSearch.DatasetEntitiesCountDto.newBuilder()
          .setCount(opalResult.getTotalHits());
      RQLCriterionOpalConverter converter = findConverter(opalResult.getQuery());
      builder.setQuery(converter.getMicaQuery())
          .setVariable(documentDigestDtos.asDto(converter.getVariableReferences().getVariable()))
          .setDataset(documentDigestDtos.asDto(converter.getVariableReferences().getDataset()))
          .setStudy(documentDigestDtos.asDto(converter.getVariableReferences().getStudy()));
      return builder.build();
    }

    private RQLCriterionOpalConverter findConverter(String opalQuery) {
      return opalConverters.stream().filter(c -> c.getOpalQuery().equals(opalQuery)).findFirst().get();
    }

  }
}
