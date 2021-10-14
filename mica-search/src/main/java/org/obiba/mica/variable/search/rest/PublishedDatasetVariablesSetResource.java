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
import com.google.common.collect.Sets;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetResource;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Path("/variables/set/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesSetResource extends AbstractPublishedDocumentsSetResource<VariableSetService> {

  private final VariableSetService variableSetService;

  @Inject
  public PublishedDatasetVariablesSetResource(
    VariableSetService variableSetService,
    JoinQueryExecutor joinQueryExecutor,
    Searcher searcher,
    Dtos dtos,
    MicaConfigService micaConfigService,
    SubjectAclService subjectAclService) {
    super(joinQueryExecutor, micaConfigService, subjectAclService, searcher, dtos);
    this.variableSetService = variableSetService;
  }

  @Override
  protected VariableSetService getDocumentSetService() {
    return variableSetService;
  }

  @GET
  public Mica.DocumentSetDto get(@PathParam("id") String id) {
    return getDocumentSet(id);
  }

  @DELETE
  public Response delete(@PathParam("id") String id) {
    deleteDocumentSet(id);
    return Response.ok().build();
  }

  @GET
  @Path("/documents")
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id, @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    variableSetService.touch(documentSet);
    return Mica.DatasetVariablesDto.newBuilder()
      .setTotal(documentSet.getIdentifiers().size())
      .setFrom(from)
      .setLimit(limit)
      .addAllVariables(variableSetService.getVariables(documentSet, from, limit).stream()
        .map(dtos::asDto).collect(Collectors.toList())).build();
  }

  @POST
  @Path("/documents/_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(@PathParam("id") String id, String body) {
    return Response.ok().entity(importDocuments(id, body)).build();
  }

  @POST
  @Path("/documents/_rql")
  public Response importQueryVariables(@PathParam("id") String id, @FormParam("query") String query) throws IOException {
    return Response.ok().entity(importQueryDocuments(id, query)).build();
  }

  @GET
  @Path("/documents/_export")
  @Produces(MediaType.TEXT_PLAIN)
  public Response exportVariables(@PathParam("id") String id) {
    return Response.ok(exportDocuments(id))
      .header("Content-Disposition", String.format("attachment; filename=\"%s-variables.txt\"", id)).build();
  }

  @GET
  @Path("/documents/_opal")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response createOpalViewsGet(@PathParam("id") String id, @QueryParam("ids") String identifiers) {
    DocumentSet set = getSecuredDocumentSet(id);
    if (!subjectAclService.isAdministrator() && !subjectAclService.isDataAccessOfficer())
      throw new AuthorizationException();
    StreamingOutput streamingOutput;

    if (!Strings.isNullOrEmpty(identifiers)) {
      streamingOutput = stream -> variableSetService.createOpalViewsZip(variableSetService.getVariables(Sets.newHashSet(identifiers.split(","))), micaConfigService.getConfig().getOpalViewsGrouping(), new BufferedOutputStream(stream));
    } else {
      streamingOutput = stream -> variableSetService.createOpalViewsZip(variableSetService.getVariables(set), micaConfigService.getConfig().getOpalViewsGrouping(), new BufferedOutputStream(stream));
    }

    return Response.ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"opal-views-" + id + ".zip\"").build();
  }

  @POST
  @Path("/documents/_opal")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response createOpalViewsPost(@PathParam("id") String id, @FormParam("ids") String identifiers) {
    return createOpalViewsGet(id, identifiers);
  }

  @POST
  @Path("/documents/_delete")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response deleteVariables(@PathParam("id") String id, String body) {
    return Response.ok().entity(deleteDocuments(id, body)).build();
  }

  @DELETE
  @Path("/documents")
  public Response deleteVariables(@PathParam("id") String id) {
    deleteDocuments(id);
    return Response.ok().build();
  }

  @GET
  @Path("/document/{documentId}/_exists")
  public Response hasVariable(@PathParam("id") String id, @PathParam("documentId") String documentId) {
    return hasDocument(id, documentId) ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

  //
  // Protected methods
  //

  protected DocumentSet getSecuredDocumentSet(String id) {
    DocumentSet documentSet = super.getSecuredDocumentSet(id);
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled() && !documentSet.hasName()) throw new AuthorizationException(); // cart
    if (config.isCartEnabled() && !config.isAnonymousCanCreateCart() && !subjectAclService.hasMicaRole() && !documentSet.hasName())
      throw new AuthorizationException(); // cart
    if (documentSet.hasName() && !subjectAclService.hasMicaRole()) throw new AuthorizationException();
    return documentSet;
  }

}
