/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import com.google.common.base.Strings;
import org.obiba.magma.ValueTable;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.spi.dataset.StudyTableSource;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;

import javax.validation.constraints.NotNull;

/**
 * Connector to an Opal server, to retrieve value table and summary statistics.
 */
public class OpalTableSource implements StudyTableSource {

  private OpalService opalService;

  private String opalUrl;

  @NotNull
  private String project;

  @NotNull
  private String table;

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  @Override
  public ValueTable getValueTable() {
    return getDatasource().getValueTable(table);
  }

  @Override
  public Search.QueryResultDto getFacets(Search.QueryTermsDto query) {
    return getRestValueTable().getFacets(query);
  }

  @Override
  public Math.SummaryStatisticsDto getVariableSummary(String variableName) {
    return ((RestValueTable.RestVariableValueSource)getRestValueTable().getVariableValueSource(variableName)).getSummary();
  }

  @Override
  public String getURN() {
    return String.format("urn:opal:%s.%s", project, table);
  }

  public static boolean isFor(String sourceURN) {
    return !Strings.isNullOrEmpty(sourceURN) && sourceURN.startsWith("urn:opal:");
  }

  public static OpalTableSource newSource(String project, String table) {
    OpalTableSource source = new OpalTableSource();
    source.setProject(project);
    source.setTable(table);
    return source;
  }

  public static OpalTableSource fromURN(String sourceURN) {
    if (Strings.isNullOrEmpty(sourceURN) || !sourceURN.startsWith("urn:opal:"))
      throw new IllegalArgumentException("Not a valid Opal table source URN: " + sourceURN);

    String fullName = toTableName(sourceURN);
    int sep = fullName.indexOf(".");
    String project = fullName.substring(0, sep);
    String table = fullName.substring(sep + 1);
    return OpalTableSource.newSource(project, table);
  }

  public static String toTableName(String sourceURN) {
    return sourceURN.replace("urn:opal:", "");
  }

  public void init(OpalService opalService, String opalUrl) {
    this.opalService = opalService;
    this.opalUrl = opalUrl;
  }

  private RestDatasource getDatasource() {
    return opalService.getDatasource(opalUrl, project);
  }

  private RestValueTable getRestValueTable() {
    return (RestValueTable) getDatasource().getValueTable(table);
  }
}
