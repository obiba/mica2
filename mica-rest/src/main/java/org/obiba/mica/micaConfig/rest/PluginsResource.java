/*
 * Copyright (c) 2023 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;


import com.google.common.base.Strings;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.MicaPlugins;
import org.obiba.mica.web.model.PluginDtos;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.stream.Collectors;

@Component
@Path("/config/plugins")
@RequiresAuthentication
@RequiresRoles(Roles.MICA_ADMIN)
public class PluginsResource {

  @Inject
  private PluginsService pluginsService;

  @GET
  public MicaPlugins.PluginPackagesDto getInstalledPlugins(@QueryParam("type") String type) {
    return PluginDtos.asDto(pluginsService.getUpdateSite(), pluginsService.getLastUpdate(), pluginsService.restartRequired(),
      pluginsService.getInstalledPlugins().stream()
        .filter(p -> Strings.isNullOrEmpty(type) || (type.equals(p.getType())))
        .collect(Collectors.toList()), pluginsService.getUninstalledPluginNames());
  }

  @GET
  @Path("/_updates")
  public MicaPlugins.PluginPackagesDto getUpdatablePlugins(@QueryParam("type") String type) {
    return PluginDtos.asDto(pluginsService.getUpdateSite(), pluginsService.getLastUpdate(), pluginsService.restartRequired(),
      pluginsService.getUpdatablePlugins().stream()
        .filter(p -> Strings.isNullOrEmpty(type) || (type.equals(p.getType())))
        .collect(Collectors.toList()));
  }

  @GET
  @Path("/_available")
  public MicaPlugins.PluginPackagesDto getAvailablePlugins(@QueryParam("type") String type) {
    return PluginDtos.asDto(pluginsService.getUpdateSite(), pluginsService.getLastUpdate(), pluginsService.restartRequired(),
      pluginsService.getAvailablePlugins().stream()
        .filter(p -> Strings.isNullOrEmpty(type) || (type.equals(p.getType())))
        .collect(Collectors.toList()));
  }

  @POST
  public Response installPlugin(@QueryParam("name") String name, @QueryParam("version") String version) {
    if (!Strings.isNullOrEmpty(name)) {
      pluginsService.installPlugin(name, version);
      return Response.ok().build();
    }
    return Response.status(Response.Status.BAD_REQUEST).build();
  }
}
