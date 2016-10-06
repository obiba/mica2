package org.obiba.mica.search.csvexport;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.queries.rql.JoinRQLQueryWrapper;
import org.obiba.mica.search.queries.rql.RQLQueryFactory;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.ConstrainedTo;
import java.io.ByteArrayOutputStream;
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

  public OutputStream generateCsv(JoinQueryExecutor.QueryType exportType, String rqlQuery, List<String> columnsToHide) throws IOException {
    JoinRQLQueryWrapper joinQueryWrapper = rqlQueryFactory.makeJoinQuery(rqlQuery);
    MicaSearch.JoinQueryResultDto queryResult = joinQueryExecutor.query(exportType, joinQueryWrapper);

    CsvReportGenerator csvReportGenerator = csvReportGeneratorFactory.get(exportType, queryResult, columnsToHide, joinQueryWrapper.getLocale());

    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    csvReportGenerator.write(byteOutputStream);

    return byteOutputStream;
  }
}
