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

import com.google.common.base.Strings;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.shiro.realm.ObibaRealm;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.NoSuchElementException;

@Component
@Path("/user/{username}")
@RequiresAuthentication
public class UserProfileResource {

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @GET
  public Mica.UserProfileDto getProfile(@PathParam("username") String username, @QueryParam("group") String group) {
    // can query itself or must have permission
    if (!subjectAclService.isCurrentUser(username))
      subjectAclService.checkPermission("/user", "VIEW");

    ObibaRealm.Subject subject = Strings.isNullOrEmpty(group) ?
      userProfileService.getProfile(username) : userProfileService.getProfileByGroup(username, group);

    if (subject == null)
      throw new NoSuchElementException("User '" + username + "' was not found" + (Strings.isNullOrEmpty(group) ? "" : " in group '" + group + "'"));

    return dtos.asDto(subject);
  }

  @GET
  @Path("/application/{application}")
  @RequiresRoles(Roles.MICA_DAO)
  @Deprecated
  public Mica.UserProfileDto getProfileByApplication(@PathParam("username") String username, @PathParam("application") String application,
                                                     @QueryParam("group") String group) {
    return getProfile(username, group);
  }
}
