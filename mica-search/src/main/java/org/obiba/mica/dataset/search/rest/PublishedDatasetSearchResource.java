/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.json.JSONException;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.web.model.MicaSearch;

import com.codahale.metrics.annotation.Timed;

@Path("/datasets/_search")
@RequiresAuthentication
public class PublishedDatasetSearchResource {
  @Inject
  JoinQueryExecutor joinQueryExecutor;

  @GET
  @Timed
  public MicaSearch.JoinQueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("size") @DefaultValue("10") int size)
      throws JSONException, IOException {
    return joinQueryExecutor.query(from, size);
  }

  @POST
  @Timed
  public MicaSearch.JoinQueryResultDto list(MicaSearch.JoinQueryDto joinQueryDto) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.DATASET, joinQueryDto);
  }

}
