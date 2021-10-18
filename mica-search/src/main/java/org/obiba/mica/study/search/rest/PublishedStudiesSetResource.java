package org.obiba.mica.study.search.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetResource;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.JoinQueryReportGenerator;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.service.StudySetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Path("/studies/set/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedStudiesSetResource extends AbstractPublishedDocumentsSetResource<StudySetService> {

  private final StudySetService studySetService;

  public PublishedStudiesSetResource(StudySetService studySetService,
                                     JoinQueryExecutor joinQueryExecutor,
                                     MicaConfigService micaConfigService,
                                     SubjectAclService subjectAclService,
                                     Searcher searcher,
                                     Dtos dtos,
                                     JoinQueryReportGenerator joinQueryReportGenerator) {
    super(joinQueryExecutor, micaConfigService, subjectAclService, searcher, dtos, joinQueryReportGenerator);
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
  public Mica.StudiesDto getStudies(@PathParam("id") String id, @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue("10") int limit) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    studySetService.touch(documentSet);
    return Mica.StudiesDto.newBuilder()
      .setTotal(documentSet.getIdentifiers().size())
      .setFrom(from)
      .setLimit(limit)
      .addAllStudies(studySetService.getPublishedStudies(documentSet, from, limit).stream()
        .map(dtos::asDto).collect(Collectors.toList())).build();
  }

  @POST
  @Path("/documents/_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importStudies(@PathParam("id") String id, String body) {
    return Response.ok().entity(importDocuments(id, body)).build();
  }

  @POST
  @Path("/documents/_rql")
  public Response importQueryStudies(@PathParam("id") String id, @FormParam("query") String query) throws IOException {
    return Response.ok().entity(importQueryDocuments(id, query)).build();
  }

  @GET
  @Path("/documents/_export")
  @Produces(MediaType.TEXT_PLAIN)
  public Response exportStudies(@PathParam("id") String id) {
    return Response.ok(exportDocuments(id))
      .header("Content-Disposition", String.format("attachment; filename=\"%s-studies.txt\"", id)).build();
  }

  @GET
  @Path("/documents/_report")
  @Produces("text/csv")
  public Response reportStudies(@PathParam("id") String id, @QueryParam("locale") @DefaultValue("en") String locale) {
    String query = String.format("study(in(Mica_study.sets,%s),limit(0,10000),fields(*)),locale(%s)", id, locale);
    StreamingOutput stream = reportDocuments(id, QueryType.STUDY, query);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.csv\"").build();
  }

  @POST
  @Path("/documents/_delete")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response deleteStudies(@PathParam("id") String id, String body) {
    return Response.ok().entity(deleteDocuments(id, body)).build();
  }

  @DELETE
  @Path("/documents")
  public Response deleteStudies(@PathParam("id") String id) {
    deleteDocuments(id);
    return Response.ok().build();
  }

  @GET
  @Path("/document/{documentId}/_exists")
  public Response hasStudy(@PathParam("id") String id, @PathParam("documentId") String documentId) {
    return hasDocument(id, documentId) ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

}
