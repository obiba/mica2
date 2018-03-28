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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.rest.entity.StudyEntitiesCountService;
import org.obiba.mica.dataset.rest.entity.StudyEntitiesCountQuery;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Component
@Path("/datasets/entities")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetsEntitiesResource {


  @Inject
  private StudyEntitiesCountService studyEntitiesCountService;

  @Inject
  private MicaConfigService micaConfigService;

  @GET
  @Path("_count")
  @Timed
  public MicaSearch.EntitiesCountDto countEntities(@QueryParam("query") String query, @QueryParam("type") @DefaultValue("Participant") String entityType) {
    MicaSearch.EntitiesCountDto.Builder builder = MicaSearch.EntitiesCountDto.newBuilder()
        .setQuery(query);

    int total = 0;
    for (StudyEntitiesCountQuery studyEntitiesCountQuery : studyEntitiesCountService.newQueries(query, entityType)) {
      total = total + studyEntitiesCountQuery.getTotal();
      builder.addCounts(studyEntitiesCountQuery.getStudyEntitiesCount());
    }
    // sum of all the study counts because entities are study specific
    int privacyThreshold = micaConfigService.getConfig().getPrivacyThreshold();
    if (total<privacyThreshold) {
      builder.setTotal(privacyThreshold);
      builder.setBelowPrivacyThreshold(true);
    } else {
      builder.setTotal(total);
      builder.setBelowPrivacyThreshold(false);
    }

    return builder.build();
  }

}
