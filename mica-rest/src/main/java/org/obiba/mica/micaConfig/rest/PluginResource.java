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


import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.MicaPlugins;
import org.obiba.mica.web.model.PluginDtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Scope("request")
@Path("/config/plugin/{name}")
@RequiresAuthentication
@RequiresRoles(Roles.MICA_ADMIN)
public class PluginResource {

  @Inject
  private PluginsService pluginsService;

  @GET
  public MicaPlugins.PluginDto get(@PathParam("name") String name) {
    return PluginDtos.asDto(pluginsService.getInstalledPlugin(name));
  }

  @DELETE
  public Response uninstall(@PathParam("name") String name) {
    pluginsService.prepareUninstallPlugin(name);
    return Response.noContent().build();
  }

  @PUT
  public Response cancelUninstallation(@PathParam("name") String name) {
    pluginsService.cancelUninstallPlugin(name);
    return Response.noContent().build();
  }

  @PUT
  @Path("/cfg")
  @Consumes("text/plain")
  public Response saveConfig(@PathParam("name") String name, String properties) {
    pluginsService.setInstalledPluginSiteProperties(name, properties);
    return Response.ok().build();
  }

  @GET
  @Path("/service")
  public MicaPlugins.ServicePluginDto getServiceStatus(@PathParam("name") String name) {
    return MicaPlugins.ServicePluginDto.newBuilder()
      .setName(name)
      .setStatus(pluginsService.isInstalledPluginRunning(name) ? MicaPlugins.ServicePluginStatus.RUNNING : MicaPlugins.ServicePluginStatus.STOPPED)
      .build();
  }

  @PUT
  @Path("/service")
  public Response startService(@PathParam("name") String name) {
    pluginsService.startInstalledPlugin(name);
    return Response.ok().build();
  }

  @DELETE
  @Path("/service")
  public Response stopService(@PathParam("name") String name) {
    pluginsService.stopInstalledPlugin(name);
    return Response.ok().build();
  }
}
