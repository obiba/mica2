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

import org.obiba.mica.web.model.MicaSearch;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

public class CsvTaxonomyCoverageWriter {

  public ByteArrayOutputStream write(MicaSearch.TaxonomiesCoverageDto coverage) throws IOException {

    ByteArrayOutputStream values = new ByteArrayOutputStream();
    CSVWriter writer = null;
    try {
      writer = new CSVWriter(new PrintWriter(values));
      List<String> bucketNames = writeHeader(writer, coverage);
      writeBody(writer, coverage, bucketNames);
    } finally {
      if(writer != null) writer.close();
    }

    return values;
  }

  private List<String> writeHeader(CSVWriter writer, MicaSearch.TaxonomiesCoverageDto coverage) {
    List<String> headers = Lists.newArrayList("Taxonomy", "Vocabulary", "Term");
    List<String> bucketNames = Lists.newArrayList();
    if(coverage.getTaxonomiesCount() > 0) {
      coverage.getTaxonomiesList().forEach(taxo -> {
        if(taxo.getBucketsCount() > 0) {
          taxo.getBucketsList().stream().filter(bucket -> !bucketNames.contains(bucket.getValue()))
            .forEach(bucket -> bucketNames.add(bucket.getValue()));
        }
      });
    }
    headers.addAll(bucketNames);

    writer.writeNext(headers.toArray(new String[headers.size()]));

    return bucketNames;
  }

  private void writeBody(CSVWriter writer, MicaSearch.TaxonomiesCoverageDto coverage, List<String> bucketNames) {
    if(coverage.getTaxonomiesCount() == 0) return;

    coverage.getTaxonomiesList().forEach(taxo -> writeBody(writer, taxo, bucketNames));
  }

  private void writeBody(CSVWriter writer, MicaSearch.TaxonomyCoverageDto taxo, List<String> bucketNames) {
    if(taxo.getHits() == 0) return;

    taxo.getVocabulariesList().forEach(voc -> writeBody(writer, taxo.getTaxonomy().getName(), voc, bucketNames));
  }

  private void writeBody(CSVWriter writer, String taxoName, MicaSearch.VocabularyCoverageDto voc,
    List<String> bucketNames) {
    if(voc.getHits() == 0) return;

    voc.getTermsList().forEach(term -> writeBody(writer, taxoName, voc.getVocabulary().getName(), term, bucketNames));
  }

  private void writeBody(CSVWriter writer, String taxoName, String vocName, MicaSearch.TermCoverageDto term,
    Iterable<String> bucketNames) {
    if(term.getHits() == 0) return;

    List<String> row = Lists.newArrayList();

    row.add(taxoName);
    row.add(vocName);
    row.add(term.getTerm().getName());

    bucketNames.forEach(b -> {
      boolean found = false;
      for(MicaSearch.BucketCoverageDto bucket : term.getBucketsList()) {
        if(bucket.getValue().equals(b)) {
          row.add(bucket.getHits() + "");
          found = true;
          break;
        }
      }
      if(!found) {
        row.add("0");
      }
    });

    writer.writeNext(row.toArray(new String[row.size()]));
  }

}
