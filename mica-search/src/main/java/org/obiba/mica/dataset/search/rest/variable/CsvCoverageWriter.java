/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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

  private List<String> distinctFields;

  public ByteArrayOutputStream write(CoverageByBucket coverageByBucket) throws IOException {

    this.coverageByBucket = coverageByBucket;

    distinctFields = coverageByBucket.getBucketRows().stream().map(b -> b.field).distinct()
      .collect(Collectors.toList());

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

  private boolean hasStartEndHeaders() {
    return distinctFields.contains("dceId") || distinctFields.contains("studyId");
  }

  private boolean hasSingletonField() {
    return distinctFields.size() == 1;
  }

  private boolean isDCEField() {
    return distinctFields.contains("dceId");
  }

  private void writeHeaders(CSVWriter writer) {
    writeTaxonomyHeaders(writer);
    writeVocabularyHeaders(writer);
    writeTermHeaders(writer);
  }

  private void writeTaxonomyHeaders(CSVWriter writer) {
    List<String> headers = getPreliminaryHeaders();
    coverageByBucket.getTaxonomyHeaders().forEach(taxonomyHeader -> {
      headers.add(taxonomyHeader.taxonomy.getTitles(0).getValue());
      for(int i = 1; i < taxonomyHeader.termsCount; i++) {
        headers.add("");
      }
    });
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeVocabularyHeaders(CSVWriter writer) {
    List<String> headers = getPreliminaryHeaders();
    coverageByBucket.getVocabularyHeaders().forEach(vocabularyHeader -> {
      headers.add(vocabularyHeader.vocabulary.getTitles(0).getValue());
      for(int i = 1; i < vocabularyHeader.termsCount; i++) {
        headers.add("");
      }
    });
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private List<String> getPreliminaryHeaders() {
    List<String> headers = Lists.newArrayList("");
    if(!hasSingletonField()) {
      headers.add("");
    } else if (isDCEField()) {
      headers.add("");
      headers.add("");
    }
    if(hasStartEndHeaders()) {
      headers.add("");
      headers.add("");
    }
    return headers;
  }

  private void writeTermHeaders(CSVWriter writer) {
    List<String> headers = Lists.newArrayList();
    if(hasSingletonField()) {
      if (isDCEField()) {
        headers.add("Study");
        headers.add("Population");
      }
      headers.add(coverageByBucket.getBucketRows().get(0).fieldTitle);
    } else {
      headers.add("ID");
      headers.add("Type");
    }
    if(hasStartEndHeaders()) {
      headers.add("Start");
      headers.add("End");
    }
    coverageByBucket.getTermHeaders().forEach(termHeader -> headers.add(termHeader.term.getTitles(0).getValue()));
    writer.writeNext(headers.toArray(new String[headers.size()]));
  }

  private void writeBucketRows(CSVWriter writer) {
    coverageByBucket.getBucketRows().forEach(bucketRow -> {
      List<String> row = Lists.newArrayList();

      if(hasSingletonField() && isDCEField()) {
        String[] title = bucketRow.title.split(":");
        row.add(title[0]);
        row.add(title[1]);
        row.add(title.length > 2 ? title[2] : "");
      } else {
        row.add(bucketRow.title);
      }

      if (!hasSingletonField()) {
        row.add(bucketRow.fieldTitle);
      }
      if(hasStartEndHeaders()) {
        row.add(bucketRow.start);
        row.add(bucketRow.end);
      }
      row.addAll(bucketRow.hits.stream().map(Object::toString).collect(Collectors.toList()));
      writer.writeNext(row.toArray(new String[row.size()]));
    });
  }

}
