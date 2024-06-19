/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search.rest;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Path("/draft/persons/external/_search")
@RequiresAuthentication
@Scope("request")
@Component
public class DraftPersonsExternalEditorSearchResource extends AbstractPersonsSearchResource {

  @GET
  @Timed
  public Mica.PersonsDto query(@QueryParam("from") @DefaultValue("0") int from,
                               @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
                               @QueryParam("order") @DefaultValue("asc") String order, @QueryParam("query") String query,
                               @QueryParam("exclude") List<String> excludes) throws IOException {

    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN) && !SecurityUtils.getSubject().hasRole(Roles.MICA_EXTERNAL_EDITOR)) {
      throw new AuthorizationException();
    }

    return super.query(from, limit, sort, order, query, excludes);

  }

  @GET
  @Timed
  @Produces("text/csv")
  public Response queryCSV(@QueryParam("from") @DefaultValue("0") int from,
                           @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
                           @QueryParam("order") @DefaultValue("asc") String order, @QueryParam("query") String query,
                           @QueryParam("exclude") List<String> excludes) throws IOException {

    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN) && !SecurityUtils.getSubject().hasRole(Roles.MICA_EXTERNAL_EDITOR)) {
      throw new AuthorizationException();
    }

    return super.queryCSV(from, limit, sort, order, query, excludes);
  }

  @GET
  @Path("/_download")
  @Timed
  @Produces("text/csv")
  public Response downloadQueryCSV(@QueryParam("from") @DefaultValue("0") int from,
                                   @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
                                   @QueryParam("order") @DefaultValue("asc") String order, @QueryParam("query") String query,
                                   @QueryParam("exclude") List<String> excludes) throws IOException {

    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN) && !SecurityUtils.getSubject().hasRole(Roles.MICA_EXTERNAL_EDITOR)) {
      throw new AuthorizationException();
    }

    return super.downloadQueryCSV(from, limit, sort, order, query, excludes);
  }

  @Override
  protected boolean isDraft() {
    return true;
  }
}
