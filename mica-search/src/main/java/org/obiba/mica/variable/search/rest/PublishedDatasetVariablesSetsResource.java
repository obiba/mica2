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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.SetOperation;
import org.obiba.mica.dataset.service.VariableSetOperationService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetsResource;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
@Path("/variables/sets")
@Scope("request")
public class PublishedDatasetVariablesSetsResource extends AbstractPublishedDocumentsSetsResource<VariableSetService> {

  private final VariableSetService variableSetService;

  private final VariableSetOperationService variableSetOperationService;

  @Inject
  public PublishedDatasetVariablesSetsResource(
    VariableSetService variableSetService,
    VariableSetOperationService variableSetOperationService,
    MicaConfigService micaConfigService,
    SubjectAclService subjectAclService,
    Dtos dtos) {
    super(micaConfigService, subjectAclService, dtos);
    this.variableSetService = variableSetService;
    this.variableSetOperationService = variableSetOperationService;
  }

  @Override
  protected VariableSetService getDocumentSetService() {
    return variableSetService;
  }

  @Override
  protected boolean isCartEnabled(MicaConfig config) {
    return config.isCartEnabled();
  }

  @GET
  public List<Mica.DocumentSetDto> list(@Context HttpServletRequest request, @QueryParam("id") List<String> ids) {
    return listDocumentsSets(ids, getAnonymousUserId(request));
  }

  @POST
  @RequiresAuthentication
  public Response createEmpty(@Context UriInfo uriInfo, @QueryParam("name") String name) {
    Mica.DocumentSetDto created = createEmptyDocumentSet(name);
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build()).entity(created).build();
  }

  @GET
  @Path("_cart")
  public Mica.DocumentSetDto getOrCreateCart(@Context HttpServletRequest request) {
    return getOrCreateDocumentSetCart(request);
  }

  @POST
  @Path("_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(@Context UriInfo uriInfo, @QueryParam("name") String name, String body) {
    Mica.DocumentSetDto created = importDocuments(name, body);
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build())
      .entity(created).build();
  }

  @POST
  @Path("operations")
  @RequiresAuthentication
  public Response compose(@Context UriInfo uriInfo, @QueryParam("s1") String set1, @QueryParam("s2") String set2, @QueryParam("s3") String set3) {
    if (!subjectAclService.hasMicaRole()) throw new AuthorizationException();
    List<DocumentSet> sets = Lists.newArrayList();
    sets.add(variableSetService.get(set1));
    sets.add(variableSetService.get(set2));
    if (!Strings.isNullOrEmpty(set3)) sets.add(variableSetService.get(set3));
    SetOperation setOperation = variableSetOperationService.create(sets);
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "sets", "operation", setOperation.getId()).build()).build();
  }

  @GET
  @Path("operation/{id}")
  @RequiresAuthentication
  public Mica.SetOperationDto compose(@Context UriInfo uriInfo, @PathParam("id") String operationId) {
    if (!subjectAclService.hasMicaRole()) throw new AuthorizationException();
    return dtos.asDto(variableSetOperationService.get(operationId));
  }

}
