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
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.MaximumDocumentSetCreationExceededException;
import org.obiba.mica.core.domain.SetOperation;
import org.obiba.mica.dataset.service.VariableSetOperationService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Path("/variables/sets")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesSetsResource {

  private VariableSetService variableSetService;

  private VariableSetOperationService variableSetOperationService;

  private MicaConfigService micaConfigService;

  private SubjectAclService subjectAclService;

  private Dtos dtos;

  private static final Logger log = LoggerFactory.getLogger(PublishedDatasetVariablesSetsResource.class);

  @Inject
  public PublishedDatasetVariablesSetsResource(
    VariableSetService variableSetService,
    VariableSetOperationService variableSetOperationService,
    MicaConfigService micaConfigService,
    SubjectAclService subjectAclService,
    Dtos dtos) {
    this.variableSetService = variableSetService;
    this.variableSetOperationService = variableSetOperationService;
    this.micaConfigService = micaConfigService;
    this.subjectAclService = subjectAclService;
    this.dtos = dtos;
  }

  @GET
  public List<Mica.DocumentSetDto> list(@QueryParam("id") List<String> ids) {
    if (!subjectAclService.hasMicaRole()) throw new AuthorizationException();

    if (ids.isEmpty())
      return variableSetService.getAllCurrentUser().stream().map(s -> dtos.asDto(s)).collect(Collectors.toList());
    else
      return ids.stream().map(id -> dtos.asDto(variableSetService.get(id))).collect(Collectors.toList());
  }

  @POST
  public Response createEmpty(@Context UriInfo uriInfo, @QueryParam("name") String name) {
    ensureUserIsAuthorized(name);
    if (!Strings.isNullOrEmpty(name)) checkSetsNumberLimit();

    DocumentSet created = variableSetService.create(name, Lists.newArrayList());
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build()).build();
  }

  /**
   * A cart is a set without name, associated to a user.
   *
   * @return
   */
  @GET
  @Path("_cart")
  public Mica.DocumentSetDto getOrCreateCart() {
    if (!subjectAclService.hasMicaRole()) throw new AuthorizationException();
    ensureUserIsAuthorized("");
    return dtos.asDto(variableSetService.getCartCurrentUser());
  }

  @POST
  @Path("_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(@Context UriInfo uriInfo, @QueryParam("name") String name, String body) {
    ensureUserIsAuthorized(name);
    if (!Strings.isNullOrEmpty(name)) checkSetsNumberLimit();

    DocumentSet created = variableSetService.create(name, variableSetService.extractIdentifiers(body));
    return Response.created(uriInfo.getBaseUriBuilder().segment("variables", "set", created.getId()).build())
      .entity(dtos.asDto(created)).build();
  }

  @POST
  @Path("operations")
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
  public Mica.SetOperationDto compose(@Context UriInfo uriInfo, @PathParam("id") String operationId) {
    if (!subjectAclService.hasMicaRole()) throw new AuthorizationException();
    return dtos.asDto(variableSetOperationService.get(operationId));
  }

  private void ensureUserIsAuthorized(String name) {
    MicaConfig config = micaConfigService.getConfig();
    if (!config.isCartEnabled() && Strings.isNullOrEmpty(name)) throw new AuthorizationException(); // cart
    if (config.isCartEnabled() && !config.isAnonymousCanCreateCart() && !subjectAclService.hasMicaRole() && Strings.isNullOrEmpty(name)) throw new AuthorizationException(); // cart
    if (!Strings.isNullOrEmpty(name) && !subjectAclService.hasMicaRole()) throw new AuthorizationException();
  }

  private long numberOfNamedSets() {
    return variableSetService.getAllCurrentUser().stream().filter(DocumentSet::hasName).count();
  }

  private void checkSetsNumberLimit() {
    long maxNumberOfSets = micaConfigService.getConfig().getMaxNumberOfSets();

    if (numberOfNamedSets() >= maxNumberOfSets)
      throw MaximumDocumentSetCreationExceededException.because(maxNumberOfSets, variableSetService.getType());
  }
}
