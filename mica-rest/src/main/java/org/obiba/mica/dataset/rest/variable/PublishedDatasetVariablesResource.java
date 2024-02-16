/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.variable;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.web.model.Dtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;

@Component
@Path("/variables")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesResource {

  @Inject
  private PublishedDatasetVariableService publishedDatasetVariableService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("_suggest")
  @Timed
  public List<String> suggest(@QueryParam("locale") @DefaultValue("en") String locale,
                              @QueryParam("limit") @DefaultValue("10") int limit,
                              @QueryParam("query") String query) {
    if (Strings.isNullOrEmpty(query)) return Lists.newArrayList();
    return publishedDatasetVariableService.suggest(limit, locale, query);
  }
}
