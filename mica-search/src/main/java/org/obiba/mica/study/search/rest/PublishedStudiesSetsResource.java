package org.obiba.mica.study.search.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetsResource;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.StudySetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
@Path("/studies/sets")
@Scope("request")
public class PublishedStudiesSetsResource extends AbstractPublishedDocumentsSetsResource<StudySetService> {

  private final StudySetService studySetService;

  @Inject
  public PublishedStudiesSetsResource(StudySetService studySetService,
                                      MicaConfigService micaConfigService,
                                      SubjectAclService subjectAclService,
                                      Dtos dtos) {
    super(micaConfigService, subjectAclService, dtos);
    this.studySetService = studySetService;
  }

  @Override
  protected StudySetService getDocumentSetService() {
    return studySetService;
  }

  @Override
  protected boolean isCartEnabled(MicaConfig config) {
    return config.isStudiesCartEnabled();
  }

  @GET
  public List<Mica.DocumentSetDto> list(@Context HttpServletRequest request, @QueryParam("id") List<String> ids) {
    return listDocumentsSets(ids, getAnonymousUserId(request));
  }

  @POST
  @RequiresAuthentication
  public Response createEmpty(@Context UriInfo uriInfo, @QueryParam("name") String name) {
    Mica.DocumentSetDto created = createEmptyDocumentSet(name);
    return Response.created(uriInfo.getBaseUriBuilder().segment("studies", "set", created.getId()).build()).entity(created).build();
  }

  @GET
  @Path("_cart")
  public Mica.DocumentSetDto getOrCreateCart(@Context HttpServletRequest request) {
    return getOrCreateDocumentSetCart(request);
  }

  @POST
  @Path("_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importStudies(@Context UriInfo uriInfo, @QueryParam("name") String name, String body) {
    Mica.DocumentSetDto created = importDocuments(name, body);
    return Response.created(uriInfo.getBaseUriBuilder().segment("studies", "set", created.getId()).build())
      .entity(created).build();
  }

}
