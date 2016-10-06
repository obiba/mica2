package org.obiba.mica.search.csvexport;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.generators.DatasetsCsvReportGenerator;
import org.obiba.mica.search.csvexport.generators.NetworkCsvReportGenerator;
import org.obiba.mica.search.csvexport.generators.StudiesCsvReportGenerator;
import org.obiba.mica.search.csvexport.generators.VariablesCsvReportGenerator;
import org.obiba.mica.search.csvexport.translator.EmptyTranslator;
import org.obiba.mica.search.csvexport.translator.JsonTranslator;
import org.obiba.mica.search.csvexport.translator.Translator;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Component
public class CsvReportGeneratorFactory {

  @Inject
  private MicaConfigService micaConfigService;

  public CsvReportGenerator get(JoinQueryExecutor.QueryType type, MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, String locale) {

    Translator translator = getTranslatorFor(locale);

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

  private Translator getTranslatorFor(String locale) {
    try {
      return new JsonTranslator(micaConfigService.getTranslations(locale, false));
    } catch (IOException e) {
      return new EmptyTranslator();
    }
  }
}
