/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.DebugMethod;
import org.obiba.mica.search.CoverageQueryExecutor;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.GenericReportGenerator;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;

/**
 * Search for variables in the published variable index.
 */
@Component
@Path("/variables")
@Scope("request")
public class PublishedDatasetVariableSetsResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedDatasetVariableSetsResource.class);

  @POST
  @Path("sets")
  @Timed
  public Response createVariableSet(@QueryParam("q") String query, @QueryParam("n") String name) {
    RQLParser parser = new RQLParser();
    ASTNode queryNode = parser.parse(query);
    log.info("queryNode={}", queryNode);

    return Response.ok().build();
  }

  //
  // Private methods
  //


}
