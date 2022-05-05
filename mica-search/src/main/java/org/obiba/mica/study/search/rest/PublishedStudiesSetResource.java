package org.obiba.mica.study.search.rest;

import com.google.common.base.Strings;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.rest.AbstractPublishedDocumentsSetResource;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.reports.ReportGenerator;
import org.obiba.mica.search.reports.generators.StudyCsvReportGenerator;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.service.StudySetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
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
                                     PersonService personService) {
    super(joinQueryExecutor, micaConfigService, subjectAclService, searcher, dtos, personService);
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
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response reportStudies(@PathParam("id") String id, @QueryParam("locale") @DefaultValue("en") String locale, @QueryParam("studyType") String studyType) {
    DocumentSet documentSet = getSecuredDocumentSet(id);
    boolean forHarmonization = !Strings.isNullOrEmpty(studyType) && HarmonizationStudy.RESOURCE_PATH.equals(studyType);
    ReportGenerator reporter = new StudyCsvReportGenerator(studySetService.getPublishedStudies(documentSet, true), locale, personService, forHarmonization);
    StreamingOutput stream = reporter::write;
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.zip\"").build();
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

  private Mica.DocumentSetDto importQueryDocuments(String id, String query) {
    DocumentSet set = getSecuredDocumentSet(id);
    if (Strings.isNullOrEmpty(query)) return dtos.asDto(set);
    MicaSearch.JoinQueryResultDto result = makeQuery(QueryType.STUDY, query);
    if (result.hasStudyResultDto() && result.getStudyResultDto().getTotalHits() > 0) {
      List<String> ids = result.getStudyResultDto().getExtension(MicaSearch.StudyResultDto.result).getSummariesList().stream()
        .map(Mica.StudySummaryDto::getId).collect(Collectors.toList());
      getDocumentSetService().addIdentifiers(id, ids);
      set = getSecuredDocumentSet(id);
    }
    return dtos.asDto(set);
  }
}
