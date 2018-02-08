/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.variable.search.rest;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/variables/set/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetVariablesSetResource {

  @Inject
  private DocumentSetService documentSetService;

  @Inject
  private Dtos dtos;

  @PathParam("id")
  private String id;

  @GET
  public Mica.DocumentSetDto get() {
    return dtos.asDto(documentSetService.findById(id));
  }

  @GET
  @Path("/variables/_export")
  @Produces(MediaType.TEXT_PLAIN)
  public Response exportVariables() {
    DocumentSet documentSet = documentSetService.findById(id);
    if (!validateType(documentSet)) return Response.status(Response.Status.BAD_REQUEST).build();
    StreamingOutput stream = os -> {
      documentSet.getIdentifiers().forEach(id -> {
        try {
          os.write((id + "\n").getBytes());
        } catch (IOException e) {
          // ignore
        }
      });
      os.flush();
    };
    return Response.ok(stream)
      .header("Content-Disposition", String.format("attachment; filename=\"%s-variables.txt\"", id)).build();
  }

  @POST
  @Path("/variables/_import")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response importVariables(String body) {
    if (Strings.isNullOrEmpty(body)) return Response.status(Response.Status.BAD_REQUEST).build();
    DocumentSet documentSet = documentSetService.findById(id);
    if (!validateType(documentSet)) return Response.status(Response.Status.BAD_REQUEST).build();
    // TODO verify that these are valid
    List<String> identifiers = Splitter.on("\n").splitToList(body).stream()
      .filter(id -> !Strings.isNullOrEmpty(id) && DatasetVariable.Type.Collected.equals(DatasetVariable.IdResolver.from(id).getType()))
      .collect(Collectors.toList());
    if (identifiers.isEmpty()) return Response.status(Response.Status.BAD_REQUEST).build();
    documentSet.setIdentifiers(identifiers);
    documentSetService.save(documentSet);
    return Response.ok().build();
  }

  @DELETE
  @Path("/variables")
  public Response deleteVariables() {
    DocumentSet documentSet = documentSetService.findById(id);
    if (!validateType(documentSet)) return Response.status(Response.Status.BAD_REQUEST).build();
    documentSet.setIdentifiers(Lists.newArrayList());
    documentSetService.save(documentSet);
    return Response.ok().build();
  }

  private boolean validateType(DocumentSet documentSet) {
    return DatasetVariable.MAPPING_NAME.equals(documentSet.getType());
  }
}
