package org.obiba.mica.search.csvexport;

import org.obiba.core.translator.EmptyTranslator;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.generators.DatasetsCsvReportGenerator;
import org.obiba.mica.search.csvexport.generators.NetworkCsvReportGenerator;
import org.obiba.mica.search.csvexport.generators.StudiesCsvReportGenerator;
import org.obiba.mica.search.csvexport.generators.VariablesCsvReportGenerator;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class CsvReportGeneratorFactory {

  @Inject
  private MicaConfigService micaConfigService;

  public CsvReportGenerator get(JoinQueryExecutor.QueryType type, MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, String locale) {

    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));

    switch (type) {
      case STUDY:
        return new StudiesCsvReportGenerator(queryResult, columnsToHide, translator);
      case NETWORK:
        return new NetworkCsvReportGenerator(queryResult, columnsToHide, translator);
      case DATASET:
        return new DatasetsCsvReportGenerator(queryResult, columnsToHide, translator);
      case VARIABLE:
        return new VariablesCsvReportGenerator(queryResult, columnsToHide, translator);
      default:
        throw new IllegalStateException("No CsvReportGenerator available for type " + type);
    }
  }
}
