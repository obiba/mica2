/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.variable.search.rest;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/variables/sets")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesSetsResource {

  @Inject
  private VariableSetService variableSetService;

  @Inject
  private Dtos dtos;

  @GET
  public List<Mica.DocumentSetDto> list() {
    return variableSetService.getAllCurrentUser().stream().map(s -> dtos.asDto(s)).collect(Collectors.toList());
  }

  @POST
  @Path("_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(@Context UriInfo uriInfo, @QueryParam("name") String name, String body) {
    DocumentSet created = variableSetService.create(name, variableSetService.extractIdentifiers(body));
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build()).build();
  }
}
