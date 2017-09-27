/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import com.google.common.collect.Lists;
import org.obiba.plugins.spi.ServicePluginLoader;

import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

/**
 * {@link SearchEngineService} loader.
 */
public class SearchEngineServiceLoader extends ServicePluginLoader<SearchEngineService> {

  private static SearchEngineServiceLoader loader;

  private ServiceLoader<SearchEngineService> serviceLoader = ServiceLoader.load(SearchEngineService.class);

  public static synchronized SearchEngineServiceLoader get() {
    if (loader == null) loader = new SearchEngineServiceLoader();
    return loader;
  }

  public static Collection<SearchEngineService> get(URLClassLoader classLoader) {
    List<SearchEngineService> services = Lists.newArrayList(ServiceLoader.load(SearchEngineService.class, classLoader).iterator());
    return services;
  }

  @Override
  protected ServiceLoader<SearchEngineService> getServiceLoader() {
    return serviceLoader;
  }
}
