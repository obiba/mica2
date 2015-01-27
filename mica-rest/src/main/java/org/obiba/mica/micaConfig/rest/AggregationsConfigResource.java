package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.domain.AggregationsConfig;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;

import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;


@Path("config/aggregations")
public class AggregationsConfigResource {

  @Inject
  MicaConfigService micaConfigService;

  @Inject
  Dtos dtos;

  @GET
  public Mica.AggregationsConfigDto getAggregationsConfig() {
    AggregationsConfig aggregations = micaConfigService.getAggregationsConfig();

    return  dtos.asDto(aggregations);
  }

  @POST
  public void saveAggregationsConfig(Mica.AggregationsConfigDto aggDto) {
    MicaConfig config = micaConfigService.getConfig();
    config.setAggregations(dtos.fromDto(aggDto));

    micaConfigService.save(config);
  }
}
