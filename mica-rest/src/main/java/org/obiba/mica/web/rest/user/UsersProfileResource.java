/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.security.Roles;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.owasp.esapi.ESAPI;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("/users")
public class UsersProfileResource {

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/application/{application}")
  @RequiresRoles(Roles.MICA_DAO)
  @Deprecated
  public List<Mica.UserProfileDto> getProfilesByApplication(@PathParam("application") String application, @QueryParam("group") String group) {
    return getProfilesByGroup(group);
  }

  @GET
  @RequiresPermissions("/user:VIEW")
  public List<Mica.UserProfileDto> getProfilesByGroup(@QueryParam("group") String group) {
    return userProfileService.getProfilesByGroup(group).stream().map(dtos::asDto)
      .collect(Collectors.toList());
  }

  @POST
  public Response userJoin(@Context HttpServletRequest request) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> params;

    try {
      params =  mapper.readValue(request.getInputStream(),Map.class);
      userProfileService.createUser(params);
      return Response.ok().build();
    } catch (IOException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/_forgot_password")
  public Response resetPassword(@FormParam("username") String username) {
    userProfileService.resetPassword(username);
    return Response.ok().build();
  }

  @POST
  @Path("/_contact")
  public Response contact(@FormParam("name") String name, @FormParam("email") String email,
                          @FormParam("subject") String subject, @FormParam("message") String message,
                          @FormParam("g-recaptcha-response") String reCaptcha) {
    if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(email) || Strings.isNullOrEmpty(subject) || Strings.isNullOrEmpty(message) || Strings.isNullOrEmpty(reCaptcha)) {
      throw new BadRequestException();
    }
    userProfileService.sendContactEmail(ESAPI.encoder().encodeForHTML(name), ESAPI.encoder().encodeForHTML(email),
      ESAPI.encoder().encodeForHTML(subject), ESAPI.encoder().encodeForHTML(message), reCaptcha);
    return Response.ok().build();
  }
}
