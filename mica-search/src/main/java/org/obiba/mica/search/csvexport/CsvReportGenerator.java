package org.obiba.mica.search.csvexport;

import java.io.OutputStream;

public interface CsvReportGenerator {

  void write(OutputStream outputStream);

}
