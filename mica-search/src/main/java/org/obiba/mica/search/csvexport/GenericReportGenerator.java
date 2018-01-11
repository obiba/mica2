/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.csvexport;

import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.JoinQuery;
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
  private Searcher searcher;

  @Inject
  private CsvReportGeneratorFactory csvReportGeneratorFactory;

  public void generateCsv(QueryType exportType, String rqlQuery, List<String> columnsToHide, OutputStream outputStream) throws IOException {
    JoinQuery joinQuery = searcher.makeJoinQuery(rqlQuery);
    MicaSearch.JoinQueryResultDto queryResult = joinQueryExecutor.query(exportType, joinQuery);
    CsvReportGenerator csvReportGenerator = csvReportGeneratorFactory.get(exportType, queryResult, columnsToHide, joinQuery.getLocale());
    csvReportGenerator.write(outputStream);
  }
}
