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
import java.util.Collection;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/variables/set/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesSetResource {

  private final VariableSetService variableSetService;

  private final JoinQueryExecutor joinQueryExecutor;

  private final MicaConfigService micaConfigService;

  private final SubjectAclService subjectAclService;

  private final Searcher searcher;

  private final Dtos dtos;

  @Inject
  public PublishedDatasetVariablesSetResource(
    VariableSetService variableSetService,
    JoinQueryExecutor joinQueryExecutor,
    Searcher searcher,
    Dtos dtos,
    MicaConfigService micaConfigService,
    SubjectAclService subjectAclService) {
    this.variableSetService = variableSetService;
    this.joinQueryExecutor = joinQueryExecutor;
    this.micaConfigService = micaConfigService;
    this.subjectAclService = subjectAclService;
    this.searcher = searcher;
    this.dtos = dtos;
  }

  @GET
  public Mica.DocumentSetDto get(@PathParam("id") String id) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    variableSetService.touch(documentSet);
    return dtos.asDto(documentSet);
  }

  @DELETE
  public Response delete(@PathParam("id") String id) {
    variableSetService.delete(getSecuredDocumentSet(id));
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

  @GET
  @Path("/documents/_export")
  @Produces(MediaType.TEXT_PLAIN)
  public Response exportVariables(@PathParam("id") String id) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    variableSetService.touch(documentSet);
    return Response.ok(toStream(documentSet.getIdentifiers()))
      .header("Content-Disposition", String.format("attachment; filename=\"%s-variables.txt\"", id)).build();
  }

  @GET
  @Path("/opal/_export")
  @Produces(MediaType.TEXT_PLAIN)
  public Response createView(@PathParam("id") String id) {
    return Response.ok().entity(toStream(variableSetService.toOpalVariableFullNames(id)))
      .header("Content-Disposition", String.format("attachment; filename=\"%s-opal-variables.txt\"", id)).build();
  }

  @POST
  @Path("/documents/_delete")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response deleteVariables(@PathParam("id") String id, String body) {
    DocumentSet set = getSecuredDocumentSet(id);
    if (Strings.isNullOrEmpty(body)) return Response.ok().entity(dtos.asDto(set)).build();
    variableSetService.removeIdentifiers(id, variableSetService.extractIdentifiers(body));
    return Response.ok().entity(dtos.asDto(variableSetService.get(id))).build();
  }

  @POST
  @Path("/documents/_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(@PathParam("id") String id, String body) {
    DocumentSet set = getSecuredDocumentSet(id);
    if (Strings.isNullOrEmpty(body)) return Response.ok().entity(dtos.asDto(set)).build();
    variableSetService.addIdentifiers(id, variableSetService.extractIdentifiers(body));
    return Response.ok().entity(dtos.asDto(variableSetService.get(id))).build();
  }

  @POST
  @Path("/documents/_rql")
  public Response importQueryVariables(@PathParam("id") String id, @FormParam("query") String query) throws IOException {
    DocumentSet set = getSecuredDocumentSet(id);
    if (Strings.isNullOrEmpty(query)) return Response.ok().entity(dtos.asDto(set)).build();
    MicaSearch.JoinQueryResultDto result = joinQueryExecutor.query(QueryType.VARIABLE, searcher.makeJoinQuery(query));
    if (result.hasVariableResultDto() && result.getVariableResultDto().getTotalHits() > 0) {
      List<String> ids = result.getVariableResultDto().getExtension(MicaSearch.DatasetVariableResultDto.result).getSummariesList().stream()
        .map(Mica.DatasetVariableResolverDto::getId).collect(Collectors.toList());
      variableSetService.addIdentifiers(id, ids);
      set = getSecuredDocumentSet(id);
    }
    return Response.ok().entity(dtos.asDto(set)).build();
  }

  @DELETE
  @Path("/documents")
  public Response deleteVariables(@PathParam("id") String id) {
    variableSetService.setIdentifiers(id, Lists.newArrayList());
    return Response.ok().build();
  }

  @GET
  @Path("/document/{documentId}/_exists")
  public Response hasVariable(@PathParam("id") String id, @PathParam("documentId") String documentId) {
    DocumentSet set = getSecuredDocumentSet(id);
    return set.getIdentifiers().contains(documentId) ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

  private DocumentSet getSecuredDocumentSet(String id) {
    DocumentSet documentSet = variableSetService.get(id);
    if (!subjectAclService.isCurrentUser(documentSet.getUsername())) throw new AuthorizationException();

    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled() && !documentSet.hasName()) throw new AuthorizationException(); // cart
    if (config.isCartEnabled() && !config.isAnonymousCanCreateCart() && !subjectAclService.hasMicaRole() && !documentSet.hasName()) throw new AuthorizationException(); // cart
    if (documentSet.hasName() && !subjectAclService.hasMicaRole()) throw new AuthorizationException();

    return documentSet;
  }

  private StreamingOutput toStream(Collection<String> items) {
    return os -> {
      items.forEach(item -> {
        try {
          os.write((item + "\n").getBytes());
        } catch (IOException e) {
          //
        }
      });
      os.flush();
    };
  }

}
