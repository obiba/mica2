/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.obiba.magma.DatasourceFactory;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.micaConfig.MicaConfigService;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DatasourceConnectionPool implements EnvironmentAware {

  @Inject
  private MicaConfigService micaConfigService;

  private Map<String, RestDatasource> datasourceMap = new HashMap<>();

  private RelaxedPropertyResolver opalPropertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    opalPropertyResolver = new RelaxedPropertyResolver(environment, "opal.");
  }

  /**
   * Get the datasource from the provided Opal server url.
   * @param opalUrl if null, default Opal server url will be used.
   * @param project
   * @return
   */
  public synchronized RestDatasource getDatasource(@Nullable String opalUrl, String project) {
    String url = opalUrl == null ? getDefaultOpal() : opalUrl;
    url = url + "/ws/datasource/" + project;
    if(!datasourceMap.containsKey(url)) {
      DatasourceFactory factory = new RestDatasourceFactory(project, opalUrl, getOpalUsername(), getOpalPassword(),
          project);
      RestDatasource datasource = (RestDatasource) factory.create();
      Initialisables.initialise(datasource);
      datasourceMap.put(url, datasource);
    }
    return datasourceMap.get(url);
  }

  /**
   * Get a {@link org.obiba.opal.rest.client.magma.RestDatasource} from the default Opal server.
   *
   * @param project
   * @return
   */
  public RestDatasource getDatasource(String project) {
    return getDatasource(getDefaultOpal(), project);
  }

  /**
   * Get the url of the default Opal server as defined in the configuration.
   * @return
   */
  public String getDefaultOpal() {
    return micaConfigService.getConfig().getOpal();
  }

  private String getOpalUsername() {
    return opalPropertyResolver.getProperty("username");
  }

  private String getOpalPassword() {
    return opalPropertyResolver.getProperty("password");
  }
}
