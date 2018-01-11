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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.security.Roles;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/users")
@RequiresAuthentication
public class UsersProfileResource {

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/application/{application}")
  @RequiresRoles(Roles.MICA_DAO)
  public List<Mica.UserProfileDto> getProfilesByApplication(@PathParam("application") String application,
    @QueryParam("group") String group) {
    return userProfileService.getProfilesByApplication(application, group).stream().map(dtos::asDto)
      .collect(Collectors.toList());
  }
}
