/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.csvexport;

import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.queries.rql.JoinRQLQueryWrapper;
import org.obiba.mica.search.queries.rql.RQLQueryFactory;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Component
@Scope("request")
public class GenericReportGenerator {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private RQLQueryFactory rqlQueryFactory;

  @Inject
  private CsvReportGeneratorFactory csvReportGeneratorFactory;

  public void generateCsv(JoinQueryExecutor.QueryType exportType, String rqlQuery, List<String> columnsToHide, OutputStream outputStream) throws IOException {
    JoinRQLQueryWrapper joinQueryWrapper = rqlQueryFactory.makeJoinQuery(rqlQuery);
    MicaSearch.JoinQueryResultDto queryResult = joinQueryExecutor.query(exportType, joinQueryWrapper);
    CsvReportGenerator csvReportGenerator = csvReportGeneratorFactory.get(exportType, queryResult, columnsToHide, joinQueryWrapper.getLocale());
    csvReportGenerator.write(outputStream);
  }
}
