package org.obiba.mica.person.search.rest;

import java.io.IOException;
import java.util.List;

import com.codahale.metrics.annotation.Timed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.obiba.mica.person.search.EsPersonService;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/persons/_search")
@Scope("request")
@Component
public class PublishedPersonsSearchResource extends AbstractPersonsSearchResource {

  @Inject
  private EsPersonService esPersonService;

  @Override
  protected boolean isDraft() {
    return false;
  }

  /**
   * The members of the given published studies, as listed on the study pages: found in the database, sorted by last
   * name.
   */
  @GET
  @Path("/_members")
  @Timed
  public Mica.PersonsDto members(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("order") @DefaultValue("asc") String order,
    @QueryParam("studyId") List<String> studyIds) {
    return asDto(esPersonService.findPublishedStudyMembers(from, limit, order, studyIds), from, limit);
  }

  @GET
  @Path("/_members/_download")
  @Timed
  @Produces("text/csv")
  public Response downloadMembersCSV(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("order") @DefaultValue("asc") String order,
    @QueryParam("studyId") List<String> studyIds) throws IOException {
    return asCSV(members(from, limit, order, studyIds));
  }
}
