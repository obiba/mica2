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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class JoinQueryReportGenerator {

  private static final int MAX_DOWNLOAD_STEP = 50000;

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Searcher searcher;

  @Inject
  private DtosCsvReportGeneratorFactory dtosCsvReportGeneratorFactory;

  public void generateCsv(QueryType exportType, String rqlQuery, List<String> columnsToHide, OutputStream outputStream) throws IOException {
    final String limitRegex = "limit\\((\\d+),(\\d+)\\)";
    Pattern pattern = Pattern.compile(limitRegex);
    Matcher matcher = pattern.matcher(rqlQuery);

    if (matcher.find()) {
      int size = Integer.valueOf(matcher.group(2));

      int numberOfSteps = Double.valueOf(Math.ceil(size / (double) MAX_DOWNLOAD_STEP)).intValue();

      for (int i = 0; i < numberOfSteps; i++) {
        JoinQuery joinQuery = searcher.makeJoinQuery(rqlQuery.replace(matcher.group(), "limit(" + (MAX_DOWNLOAD_STEP * i) + "," + MAX_DOWNLOAD_STEP + ")"));
        MicaSearch.JoinQueryResultDto queryResult = joinQueryExecutor.query(exportType, joinQuery);
        CsvReportGenerator csvReportGenerator = dtosCsvReportGeneratorFactory.get(exportType, queryResult, columnsToHide, joinQuery.getLocale());
        csvReportGenerator.write(outputStream, i > 0);
      }
    } else {
      JoinQuery joinQuery = searcher.makeJoinQuery(rqlQuery);
      MicaSearch.JoinQueryResultDto queryResult = joinQueryExecutor.query(exportType, joinQuery);
      CsvReportGenerator csvReportGenerator = dtosCsvReportGeneratorFactory.get(exportType, queryResult, columnsToHide, joinQuery.getLocale());
      csvReportGenerator.write(outputStream);
    }
  }
}
