/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.search.CoverageQueryExecutor;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * Search for variables in the published variable index.
 */
@Component
@Path("/variables")
@RequiresAuthentication
@Scope("request")
public class PublishedDatasetVariablesSearchResource {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private CoverageQueryExecutor coverageQueryExecutor;

  @GET
  @Timed
  public MicaSearch.JoinQueryResultDto query(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") String order, @QueryParam("query") String query,
    @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    return joinQueryExecutor.listQuery(JoinQueryExecutor.QueryType.VARIABLE,
      QueryDtoHelper.createQueryDto(from, limit, sort, order, query, locale, null, null), locale);
  }

  @POST
  @Timed
  @Path("_search")
  public MicaSearch.JoinQueryResultDto list(MicaSearch.JoinQueryDto joinQueryDto) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.VARIABLE, joinQueryDto);
  }

  /**
   * Get the frequency of each taxonomy terms, based on variables aggregation results after search query was applied.
   *
   * @param taxonomyNames Taxonomy filter
   * @param strict Return coverage matching the search criteria, if any.
   * @param joinQueryDto
   * @return
   * @throws IOException
   */
  @POST
  @Timed
  @Path("_coverage")
  @Produces("text/csv")
  public Response coverageCsv(@QueryParam("taxonomy") List<String> taxonomyNames,
    @QueryParam("strict") @DefaultValue("true") boolean strict, MicaSearch.JoinQueryDto joinQueryDto)
    throws IOException {
    // We need the aggregations internally for building the coverage result,
    // but we do not need them in the final result
    MicaSearch.JoinQueryDto joinQueryDtoWithoutFacets = MicaSearch.JoinQueryDto.newBuilder().mergeFrom(joinQueryDto)
      .setWithFacets(false).build();

    MicaSearch.TaxonomiesCoverageDto coverage = coverage(taxonomyNames, strict, joinQueryDtoWithoutFacets);
    CsvTaxonomyCoverageWriter writer = new CsvTaxonomyCoverageWriter();
    ByteArrayOutputStream values = writer.write(coverage);

    return Response.ok(values.toByteArray(), "text/csv")
      .header("Content-Disposition", "attachment; filename=\"coverage.csv\"").build();
  }

  /**
   * Get the frequency of each taxonomy terms, based on variables aggregation results after search query was applied.
   *
   * @param taxonomyNames Taxonomy filter
   * @param strict Return coverage matching the search criteria, if any.
   * @param joinQueryDto
   * @return
   * @throws IOException
   */
  @POST
  @Timed
  @Path("_coverage")
  public MicaSearch.TaxonomiesCoverageDto coverage(@QueryParam("taxonomy") List<String> taxonomyNames,
    @QueryParam("strict") @DefaultValue("true") boolean strict, MicaSearch.JoinQueryDto joinQueryDto)
    throws IOException {
    return coverageQueryExecutor.coverageQuery(taxonomyNames, strict, joinQueryDto);
  }

  /**
   * Get the frequency of each taxonomy terms, based on variables aggregation results (convenient resource for testing).
   *
   * @param taxonomyNames
   * @param withFacets
   * @param locale
   * @return
   * @throws IOException
   */
  @GET
  @Timed
  @Path("_coverage")
  public MicaSearch.TaxonomiesCoverageDto getCoverage(@QueryParam("taxonomy") List<String> taxonomyNames,
    @QueryParam("facets") @DefaultValue("false") boolean withFacets,
    @QueryParam("locale") @DefaultValue("") String locale) throws IOException {
    return coverageQueryExecutor.coverageQuery(taxonomyNames, false, withFacets, locale);
  }

}
