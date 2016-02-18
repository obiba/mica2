/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Coverage is rendered in a CSV dataset. The CSV layout is:
 * <ul>
 * <li>
 * one header row for the vocabulary names,
 * </li>
 * <li>
 * one header row for the term names (each one is a column),
 * </li>
 * <li>
 * one data row per bucket type (study, DCE, dataset, network)
 * </li>
 * </ul>
 */
public class CsvCoverageWriter {

  private CoverageByBucket coverageByBucket;

  public ByteArrayOutputStream write(CoverageByBucket coverageByBucket) throws IOException {

    this.coverageByBucket = coverageByBucket;

    ByteArrayOutputStream values = new ByteArrayOutputStream();
    CSVWriter writer = null;
    try {
      writer = new CSVWriter(new PrintWriter(values));
      writeHeaders(writer);
      writeBucketRows(writer);
    } finally {
      if(writer != null) writer.close();
    }

    return values;
  }

  private void writeHeaders(CSVWriter writer) {
    writeTaxonomyHeaders(writer);
    writeVocabularyHeaders(writer);
    writeTermHeaders(writer);
  }

  private void writeTaxonomyHeaders(CSVWriter writer) {
    List<String> headers = Lists.newArrayList("", "");
    coverageByBucket.getTaxonomyHeaders().forEach(taxonomyHeader -> {
      headers.add(taxonomyHeader.taxonomy.getTitles(0).getValue());
      for(int i = 1; i < taxonomyHeader.termsCount; i++) {
        headers.add("");
      }
    });
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeVocabularyHeaders(CSVWriter writer) {
    List<String> headers = Lists.newArrayList("", "");
    coverageByBucket.getVocabularyHeaders().forEach(vocabularyHeader -> {
      headers.add(vocabularyHeader.vocabulary.getTitles(0).getValue());
      for(int i = 1; i < vocabularyHeader.termsCount; i++) {
        headers.add("");
      }
    });
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeTermHeaders(CSVWriter writer) {
    List<String> headers = Lists.newArrayList("ID", "Type");
    coverageByBucket.getTermHeaders().forEach(termHeader -> headers.add(termHeader.term.getTitles(0).getValue()));
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeBucketRows(CSVWriter writer) {
    coverageByBucket.getBucketRows().forEach(bucketRow -> {
      List<String> row = Lists.newArrayList();
      row.add(bucketRow.title);
      row.add(bucketRow.fieldTitle);
      row.addAll(bucketRow.hits.stream().map(Object::toString).collect(Collectors.toList()));
      writer.writeNext(row.toArray(new String[row.size()]));
    });
  }

}
