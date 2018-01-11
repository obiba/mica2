/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.web.model.Mica;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

import static java.util.stream.Stream.concat;
import static org.obiba.mica.dataset.search.rest.harmonization.ContingencyUtils.getTermsHeaders;
import static org.obiba.mica.dataset.search.rest.harmonization.ContingencyUtils.getValuesHeaders;

public class CsvContingencyWriter {

  private DatasetVariable var;

  private DatasetVariable crossVar;

  public CsvContingencyWriter(DatasetVariable var, DatasetVariable crossVar) {
    this.var = var;
    this.crossVar = crossVar;
  }

  public ByteArrayOutputStream write(Mica.DatasetVariableContingenciesDto dto) throws IOException {
    ByteArrayOutputStream ba = new ByteArrayOutputStream();

    try(CSVWriter writer = new CSVWriter(new PrintWriter(ba))) {
      writeBody(writer, dto);
    }

    return ba;
  }

  public ByteArrayOutputStream write(Mica.DatasetVariableContingencyDto dto) throws IOException {
    ByteArrayOutputStream ba = new ByteArrayOutputStream();

    try(CSVWriter writer = new CSVWriter(new PrintWriter(ba))) {
      writeBody(writer, dto);
    }

    return ba;
  }

  private void writeBody(CSVWriter writer, Mica.DatasetVariableContingenciesDto dto) {
    List<String> terms = getTermsHeaders(var, dto);
    List<String> values = getValuesHeaders(crossVar, dto);

    dto.getContingenciesList().forEach(c -> {
      writeHeaders(writer, c, terms);
      writeTableBody(writer, c, values, terms);
      writer.writeNext(new String[] { });
    });

    writer.writeNext(new String[] { "All" });
    writeHeaders(writer, dto.getAll(), terms);
    writeTableBody(writer, dto.getAll(), values, terms);
  }

  private void writeBody(CSVWriter writer, Mica.DatasetVariableContingencyDto dto) {
    List<String> terms = getTermsHeaders(var, dto);
    List<String> values = getValuesHeaders(crossVar, dto);

    writeHeaders(writer, dto, terms);
    writeTableBody(writer, dto, values, terms);
  }

  private void writeHeaders(CSVWriter writer, Mica.DatasetVariableContingencyDto c, List<String> terms) {
    if(c.hasStudyTable()) writer.writeNext(new String[] { String
      .format("%s - %s - %s", c.getStudyTable().getProject(), c.getStudyTable().getTable(),
        c.getStudyTable().getDceId()) });
    else if(c.hasHarmonizationStudyTable()) writer.writeNext(new String[] { String
      .format("%s - %s - %s", c.getHarmonizationStudyTable().getProject(), c.getHarmonizationStudyTable().getTable(),
      c.getHarmonizationStudyTable().getPopulationId())});
    writer.writeNext(concat(concat(Stream.of(""), terms.stream()), Stream.of("Total")).toArray(String[]::new));
  }

  private void writeTableBody(CSVWriter writer, Mica.DatasetVariableContingencyDto dto, List<String> values, List<String> terms) {
    if("CONTINUOUS".equals(crossVar.getNature())) {
      writeContingencyContinuous(writer, dto, Lists.newArrayList(terms));
    } else if("CATEGORICAL".equals(crossVar.getNature())){
      writeContingencyCategorical(writer, dto, values, terms);
    } else {
      throw new RuntimeException("Invalid Dataset Variable nature");
    }
  }

  private void writeContingencyCategorical(CSVWriter writer, Mica.DatasetVariableContingencyDto c, List<String> values,
    List<String> terms) {
    List<List<Integer>> tmp = ContingencyUtils.getCategoricalRows(c, values, terms);

    IntStream.range(0, values.size()).forEach(i -> writer.writeNext(
      Lists.asList(values.get(i), tmp.get(i).stream().map(x -> x.toString()).toArray()).toArray(new String[0])));

    writer.writeNext(Lists.asList("Total", tmp.get(tmp.size() - 1).stream().map(x -> x.toString()).toArray()).stream()
      .toArray(String[]::new));
  }

  private void writeContingencyContinuous(CSVWriter writer, Mica.DatasetVariableContingencyDto c, List<String> terms) {
    List<List<Number>> table = ContingencyUtils.getContinuousRows(c, terms);
    List<String> values = Lists.newArrayList("Min", "Max", "Mean", "Std", "N");

    IntStream.range(0, values.size()).forEach(i -> writer.writeNext(
      Lists.asList(values.get(i), table.get(i).stream().map(x -> x.toString()).toArray()).stream().toArray(String[]::new)));
  }
}
