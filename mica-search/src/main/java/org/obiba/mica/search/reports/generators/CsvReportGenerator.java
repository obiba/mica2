/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.reports.generators;

import au.com.bytecode.opencsv.CSVWriter;
import org.obiba.mica.search.reports.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Report is in CSV format.
 */
public abstract class CsvReportGenerator implements ReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(CsvReportGenerator.class);

  public void write(OutputStream outputStream, boolean omitHeader) {
    try (CSVWriter writer = new CSVWriter(new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8")))) {
      if (!omitHeader) writeHeader(writer);
      writeEachLine(writer);
      writer.flush();
      outputStream.flush();
    } catch (IOException e) {
      log.error("CSV report extraction failed", e);
      throw new UncheckedIOException(e);
    } catch (Exception e) {
      log.error("CSV report extraction failed", e);
    }
  }

  public void write(OutputStream outputStream) {
    write(outputStream, false);
  }

  protected abstract void writeHeader(CSVWriter writer);

  protected abstract void writeEachLine(CSVWriter writer);
}
