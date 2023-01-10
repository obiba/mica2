/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.reports;

import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.Translator;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.reports.generators.DatasetDtosCsvReportGenerator;
import org.obiba.mica.search.reports.generators.NetworkDtosCsvReportGenerator;
import org.obiba.mica.search.reports.generators.StudySummaryDtosCsvReportGenerator;
import org.obiba.mica.search.reports.generators.DatasetVariableDtosCsvReportGenerator;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class DtosCsvReportGeneratorFactory {

  @Inject
  private MicaConfigService micaConfigService;

  public ReportGenerator get(QueryType type, boolean forHarmonization, MicaSearch.JoinQueryResultDto queryResult, List<String> columnsToHide, String locale) {
    Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(locale, false));

    switch (type) {
      case STUDY:
        return new StudySummaryDtosCsvReportGenerator(queryResult, columnsToHide, locale, translator);
      case NETWORK:
        return new NetworkDtosCsvReportGenerator(forHarmonization, queryResult, columnsToHide, locale, translator);
      case DATASET:
        return new DatasetDtosCsvReportGenerator(forHarmonization, queryResult, columnsToHide, locale, translator);
      case VARIABLE:
        return new DatasetVariableDtosCsvReportGenerator(forHarmonization, queryResult, columnsToHide, locale, translator);
      default:
        throw new IllegalStateException("No CsvReportGenerator available for type " + type);
    }
  }
}
