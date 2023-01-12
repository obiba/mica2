/*
 * Copyright (c) 2023 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import com.google.common.base.Strings;
import org.obiba.magma.type.DateTimeType;
import org.obiba.plugins.PluginPackage;
import org.obiba.plugins.PluginResources;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PluginDtos {

  public static MicaPlugins.PluginPackagesDto asDto(String site, Date updated, boolean restart, List<PluginPackage> packages) {
    return asDto(site, updated, restart, packages, null);
  }

  public static MicaPlugins.PluginPackagesDto asDto(String site, Date updated, boolean restart, List<PluginPackage> packages, Collection<String> uninstalledNames) {
    MicaPlugins.PluginPackagesDto.Builder builder = asDto(site, updated, restart);
    builder.addAllPackages(packages.stream().map(p -> asDto(p, uninstalledNames == null ? null : uninstalledNames.contains(p.getName())))
      .collect(Collectors.toList()));
    return builder.build();
  }

  public static MicaPlugins.PluginPackagesDto.Builder asDto(String site, Date updated, boolean restart) {
    MicaPlugins.PluginPackagesDto.Builder builder = MicaPlugins.PluginPackagesDto.newBuilder()
      .setSite(site)
      .setRestart(restart);
    if (updated != null) builder.setUpdated(DateTimeType.get().valueOf(updated).toString());
    return builder;
  }

  public static MicaPlugins.PluginPackageDto asDto(PluginPackage pluginPackage, Boolean uninstalled) {
    MicaPlugins.PluginPackageDto.Builder buider = MicaPlugins.PluginPackageDto.newBuilder()
      .setName(pluginPackage.getName())
      .setType(pluginPackage.getType())
      .setTitle(pluginPackage.getTitle())
      .setDescription(pluginPackage.getDescription())
      .setAuthor(Strings.isNullOrEmpty(pluginPackage.getAuthor()) ? "-" : pluginPackage.getAuthor())
      .setMaintainer(Strings.isNullOrEmpty(pluginPackage.getMaintainer()) ? "-" : pluginPackage.getMaintainer())
      .setLicense(Strings.isNullOrEmpty(pluginPackage.getLicense()) ? "-" : pluginPackage.getLicense())
      .setVersion(pluginPackage.getVersion().toString())
      .setMicaVersion(pluginPackage.getMicaVersion().toString());
    if (!Strings.isNullOrEmpty(pluginPackage.getWebsite()))
      buider.setWebsite(pluginPackage.getWebsite());
    if (!Strings.isNullOrEmpty(pluginPackage.getFileName()))
      buider.setFile(pluginPackage.getFileName());
    if (uninstalled != null) buider.setUninstalled(uninstalled);
    return buider.build();
  }

  public static MicaPlugins.PluginDto asDto(PluginResources plugin) {
    MicaPlugins.PluginDto.Builder builder = MicaPlugins.PluginDto.newBuilder()
      .setName(plugin.getName())
      .setTitle(plugin.getTitle())
      .setDescription(plugin.getDescription())
      .setAuthor(plugin.getAuthor())
      .setMaintainer(plugin.getMaintainer())
      .setLicense(plugin.getLicense())
      .setVersion(plugin.getVersion().toString())
      .setMicaVersion(plugin.getHostVersion().toString())
      .setType(plugin.getType())
      .setSiteProperties(plugin.getSitePropertiesString());
    return builder.build();
  }

}
