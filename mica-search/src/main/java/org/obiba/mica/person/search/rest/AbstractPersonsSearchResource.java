/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search.rest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;

import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.person.search.EsPersonService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

public abstract class AbstractPersonsSearchResource {

  public static final String DEFAULT_SORT = "lastName";

  @Inject
  private EsPersonService esPersonService;

  @Inject
  private Dtos dtos;

  protected abstract boolean isDraft();

  @GET
  @Timed
  public Mica.PersonsDto query(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
    @QueryParam("order") @DefaultValue("desc") String order, @QueryParam("query") String query,
    @QueryParam("exclude") List<String> excludes) throws IOException {

    String ids = excludes.stream().map(s -> "id:" + s).collect(Collectors.joining(" "));

    if(!Strings.isNullOrEmpty(ids)) {
      query += String.format(" AND NOT(%s)", ids);
    }

    PublishedDocumentService.Documents<Person> contacts = esPersonService.find(from, limit, sort, order, null, query);

    Mica.PersonsDto.Builder builder = Mica.PersonsDto.newBuilder().setFrom(from).setLimit(limit)
      .setTotal(contacts.getTotal());
    builder.addAllPersons(contacts.getList().stream().map(p -> dtos.asDto(p, isDraft()))
      .filter(p -> p.getStudyMembershipsCount() > 0 || p.getNetworkMembershipsCount() > 0)
      .collect(Collectors.toList()));

    return builder.build();
  }
}
