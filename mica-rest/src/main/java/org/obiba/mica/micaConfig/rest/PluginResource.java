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


import org.obiba.mica.micaConfig.service.PluginsService;
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
public class PluginResource {

  @PathParam("name")
  private String name;

  @Inject
  private PluginsService pluginsService;

  @GET
  public MicaPlugins.PluginDto get() {
    return PluginDtos.asDto(pluginsService.getInstalledPlugin(name));
  }

  @DELETE
  public Response uninstall() {
    pluginsService.prepareUninstallPlugin(name);
    return Response.noContent().build();
  }

  @PUT
  public Response cancelUninstallation() {
    pluginsService.cancelUninstallPlugin(name);
    return Response.noContent().build();
  }

  @PUT
  @Path("/cfg")
  @Consumes("text/plain")
  public Response saveConfig(String properties) {
    pluginsService.setInstalledPluginSiteProperties(name, properties);
    return Response.ok().build();
  }
}
