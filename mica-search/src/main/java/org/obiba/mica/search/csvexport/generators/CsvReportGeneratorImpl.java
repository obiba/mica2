/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.csvexport.generators;

import au.com.bytecode.opencsv.CSVWriter;
import org.obiba.core.translator.Translator;
import org.obiba.mica.search.csvexport.CsvReportGenerator;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class CsvReportGeneratorImpl implements CsvReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(CsvReportGeneratorImpl.class);

  public void write(OutputStream outputStream) {
    try (CSVWriter writer = new CSVWriter(new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8")))) {
      writeHeader(writer);
      writeEachLine(writer);
      outputStream.flush();
    } catch (IOException e) {
      log.error("CSV report extraction failed", e);
      throw new UncheckedIOException(e);
    } catch (Exception e) {
      log.error("CSV report extraction failed", e);
    }
  }

  protected abstract void writeHeader(CSVWriter writer);

  protected abstract void writeEachLine(CSVWriter writer);
}
