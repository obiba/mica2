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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetResource;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.reports.ReportGenerator;
import org.obiba.mica.search.reports.generators.DatasetVariableCsvReportGenerator;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/variables/set/{id}")
@Scope("request")
public class PublishedDatasetVariablesSetResource extends AbstractPublishedDocumentsSetResource<VariableSetService> {

  private final VariableSetService variableSetService;

  @Inject
  public PublishedDatasetVariablesSetResource(
    VariableSetService variableSetService,
    JoinQueryExecutor joinQueryExecutor,
    Searcher searcher,
    Dtos dtos,
    MicaConfigService micaConfigService,
    SubjectAclService subjectAclService,
    PersonService personService) {
    super(joinQueryExecutor, micaConfigService, subjectAclService, searcher, dtos, personService);
    this.variableSetService = variableSetService;
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
  public Mica.DocumentSetDto get(@PathParam("id") String id, @Context HttpServletRequest request) {
    return getDocumentSet(id, getAnonymousUserId(request));
  }

  @DELETE
  public Response delete(@PathParam("id") String id, @Context HttpServletRequest request) {
    deleteDocumentSet(id, getAnonymousUserId(request));
    return Response.ok().build();
  }

  @GET
  @Path("/documents")
  public Mica.DatasetVariablesDto getVariables(@PathParam("id") String id, @Context HttpServletRequest request, @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit) {
    DocumentSet documentSet = getSecuredDocumentSet(id, getAnonymousUserId(request));
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
  public Response importVariables(@PathParam("id") String id, @Context HttpServletRequest request, String body) {
    return Response.ok().entity(importDocuments(id, body, getAnonymousUserId(request))).build();
  }

  @POST
  @Path("/documents/_rql")
  public Response importQueryVariables(@PathParam("id") String id, @Context HttpServletRequest request, @FormParam("query") String query) throws IOException {
    return Response.ok().entity(importQueryDocuments(id, query, getAnonymousUserId(request))).build();
  }

  @GET
  @Path("/documents/_export")
  @Produces(MediaType.TEXT_PLAIN)
  public Response exportVariables(@PathParam("id") String id, @Context HttpServletRequest request) {
    return Response.ok(exportDocuments(id, getAnonymousUserId(request)))
      .header("Content-Disposition", String.format("attachment; filename=\"%s-variables.txt\"", id)).build();
  }

  @GET
  @Path("/documents/_report")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response reportVariables(@PathParam("id") String id, @Context HttpServletRequest request, @QueryParam("locale") @DefaultValue("en") String locale) {
    DocumentSet documentSet = getSecuredDocumentSet(id, getAnonymousUserId(request));
    ReportGenerator reporter = new DatasetVariableCsvReportGenerator(variableSetService.getVariables(documentSet, false), locale);
    StreamingOutput stream = reporter::write;
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Variables.zip\"").build();
  }

  @GET
  @Path("/documents/_opal")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @RequiresAuthentication
  public Response createOpalViewsGet(@PathParam("id") String id, @QueryParam("ids") String identifiers) {
    DocumentSet set = getSecuredDocumentSet(id, null);
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
  @RequiresAuthentication
  public Response createOpalViewsPost(@PathParam("id") String id, @FormParam("ids") String identifiers) {
    return createOpalViewsGet(id, identifiers);
  }

  @POST
  @Path("/documents/_delete")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response deleteVariables(@PathParam("id") String id, @Context HttpServletRequest request, String body) {
    return Response.ok().entity(deleteDocuments(id, body, getAnonymousUserId(request))).build();
  }

  @DELETE
  @Path("/documents")
  public Response deleteVariables(@PathParam("id") String id, @Context HttpServletRequest request) {
    deleteDocuments(id, getAnonymousUserId(request));
    return Response.ok().build();
  }

  @GET
  @Path("/document/{documentId}/_exists")
  public Response hasVariable(@PathParam("id") String id, @Context HttpServletRequest request, @PathParam("documentId") String documentId) {
    return hasDocument(id, documentId, getAnonymousUserId(request)) ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

  private Mica.DocumentSetDto importQueryDocuments(String id, String query, String anonymousUserId) {
    DocumentSet set = getSecuredDocumentSet(id, anonymousUserId);
    if (Strings.isNullOrEmpty(query)) return dtos.asDto(set);
    MicaSearch.JoinQueryResultDto result = makeQuery(QueryType.VARIABLE, query);
    if (result.hasVariableResultDto() && result.getVariableResultDto().getTotalHits() > 0) {
      List<String> ids = result.getVariableResultDto().getExtension(MicaSearch.DatasetVariableResultDto.result).getSummariesList().stream()
        .map(Mica.DatasetVariableResolverDto::getId).collect(Collectors.toList());
      getDocumentSetService().addIdentifiers(id, ids);
      set = getSecuredDocumentSet(id, anonymousUserId);
    }
    return dtos.asDto(set);
  }

}
