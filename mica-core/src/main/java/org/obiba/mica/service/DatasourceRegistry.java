/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.service;

import java.util.HashMap;
import java.util.Map;

import org.obiba.magma.DatasourceFactory;
import org.obiba.magma.support.Initialisables;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DatasourceRegistry implements EnvironmentAware {

  private Map<String, RestDatasource> datasourceMap = new HashMap<>();

  private RelaxedPropertyResolver opalPropertyResolver;

  @Override
  public void setEnvironment(Environment environment) {
    opalPropertyResolver = new RelaxedPropertyResolver(environment, "opal.");
  }

  public synchronized RestDatasource getDatasource(String opalUrl, String project) {
    String url = opalUrl + "/ws/datasource/" + project;
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
   * Get a {@link org.obiba.opal.rest.client.magma.RestDatasource} on the default Opal project.
   *
   * @param project
   * @return
   */
  public RestDatasource getDatasource(String project) {
    return getDatasource(getOpalUrl(), project);
  }

  private String getOpalUrl() {
    return opalPropertyResolver.getProperty("url");
  }

  private String getOpalUsername() {
    return opalPropertyResolver.getProperty("username");
  }

  private String getOpalPassword() {
    return opalPropertyResolver.getProperty("password");
  }
}
