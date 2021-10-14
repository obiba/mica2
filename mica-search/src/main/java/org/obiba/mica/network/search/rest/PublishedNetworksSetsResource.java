package org.obiba.mica.network.search.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkSetService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetsResource;
import org.obiba.mica.security.service.SubjectAclService;
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

@Component
@Path("/networks/sets")
@Scope("request")
@RequiresAuthentication
public class PublishedNetworksSetsResource extends AbstractPublishedDocumentsSetsResource<NetworkSetService> {

  private final NetworkSetService networkSetService;

  @Inject
  public PublishedNetworksSetsResource(NetworkSetService networkSetService,
                                       MicaConfigService micaConfigService,
                                       SubjectAclService subjectAclService,
                                       Dtos dtos) {
    super(micaConfigService, subjectAclService, dtos);
    this.networkSetService = networkSetService;
  }

  @Override
  protected NetworkSetService getDocumentSetService() {
    return networkSetService;
  }

  @GET
  public List<Mica.DocumentSetDto> list(@QueryParam("id") List<String> ids) {
    return listDocumentsSets(ids);
  }

  @POST
  public Response createEmpty(@Context UriInfo uriInfo, @QueryParam("name") String name) {
    Mica.DocumentSetDto created = createEmptyDocumentSet(name);
    return Response.created(uriInfo.getBaseUriBuilder().segment("networks", "set", created.getId()).build()).entity(created).build();
  }

  @GET
  @Path("_cart")
  public Mica.DocumentSetDto getOrCreateCart() {
    return getOrCreateDocumentSetCart();
  }

  @POST
  @Path("_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importNetworks(@Context UriInfo uriInfo, @QueryParam("name") String name, String body) {
    Mica.DocumentSetDto created = importDocuments(name, body);
    return Response.created(uriInfo.getBaseUriBuilder().segment("networks", "set", created.getId()).build())
      .entity(created).build();
  }

}
