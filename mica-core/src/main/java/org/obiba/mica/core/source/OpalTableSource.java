/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.source;

import com.google.common.base.Strings;
import org.obiba.magma.ValueTable;
import org.obiba.mica.core.source.support.OpalDtos;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.spi.tables.AbstractStudyTableSource;
import org.obiba.mica.spi.tables.IVariable;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;

import javax.validation.constraints.NotNull;

/**
 * Connector to an Opal server, to retrieve value table and summary statistics.
 */
public class OpalTableSource extends AbstractStudyTableSource {

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
  public boolean providesContingency() {
    return true;
  }

  @Override
  public Mica.DatasetVariableContingencyDto getContingency(IVariable variable, IVariable crossVariable) {
    Search.QueryResultDto results = getRestValueTable().getContingency(variable.getName(), crossVariable.getName());
    return OpalDtos.asDto(variable, crossVariable, getContext().getPrivacyThreshold(), results);
  }

  @Override
  public boolean providesVariableSummary() {
    return true;
  }

  @Override
  public Mica.DatasetVariableAggregationDto getVariableSummary(String variableName) {
    RestValueTable.RestVariableValueSource variableValueSource = (RestValueTable.RestVariableValueSource) getRestValueTable().getVariableValueSource(variableName);
    Math.SummaryStatisticsDto results = variableValueSource.getSummary();
    return OpalDtos.asDto(results);
  }

  @Override
  public String getURN() {
    return String.format("urn:opal:%s.%s", project, table);
  }

  public static boolean isFor(String source) {
    return !Strings.isNullOrEmpty(source) && source.startsWith("urn:opal:");
  }

  public static OpalTableSource newSource(String project, String table) {
    OpalTableSource source = new OpalTableSource();
    source.setProject(project);
    source.setTable(table);
    return source;
  }

  public static OpalTableSource fromURN(String source) {
    if (Strings.isNullOrEmpty(source) || !source.startsWith("urn:opal:"))
      throw new IllegalArgumentException("Not a valid Opal table source URN: " + source);

    String fullName = toTableName(source);
    int sep = fullName.indexOf(".");
    String project = fullName.substring(0, sep);
    String table = fullName.substring(sep + 1);
    return OpalTableSource.newSource(project, table);
  }

  public static String toTableName(String source) {
    return source.replace("urn:opal:", "");
  }

  public void setOpalService(OpalService opalService) {
    this.opalService = opalService;
    this.opalUrl = getContext().getStudy().getOpal();
  }

  private RestDatasource getDatasource() {
    return opalService.getDatasource(opalUrl, project);
  }

  private RestValueTable getRestValueTable() {
    return (RestValueTable) getDatasource().getValueTable(table);
  }
}
