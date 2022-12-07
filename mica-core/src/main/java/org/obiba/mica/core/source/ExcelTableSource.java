/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.source;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.obiba.magma.ValueTable;
import org.obiba.magma.datasource.excel.ExcelDatasource;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.spi.source.AbstractStudyTableSource;
import org.obiba.mica.spi.source.IVariable;
import org.obiba.mica.spi.source.StudyTableFileSource;
import org.obiba.mica.web.model.Mica;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

public class ExcelTableSource extends AbstractStudyTableSource implements StudyTableFileSource {

  @NotNull
  private String path;

  private String table;

  private boolean initialized;

  private ExcelDatasource excelDatasource;

  public static boolean isFor(String source) {
    if (Strings.isNullOrEmpty(source) || !source.startsWith("urn:file:"))
      return false;
    List<String> tokens = Splitter.on(":").splitToList(source);
    return tokens.size() > 2 && tokens.get(2).toLowerCase().endsWith(".xlsx");
  }

  public static ExcelTableSource fromURN(String source) {
    if (Strings.isNullOrEmpty(source) || !source.startsWith("urn:file:"))
      throw new IllegalArgumentException("Not a valid Excel table source URN: " + source);

    String fullName = source.replace("urn:file:", "");
    int sep = fullName.lastIndexOf(":");
    String file = sep > 0 ? fullName.substring(0, sep) : fullName;
    String table = sep > 0 ? fullName.substring(sep + 1) : null;
    return ExcelTableSource.newSource(file, table);
  }

  private static ExcelTableSource newSource(String path, String table) {
    ExcelTableSource source = new ExcelTableSource();
    source.path = path;
    source.table = table;
    return source;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getTable() {
    return table;
  }

  @Override
  public ValueTable getValueTable() {
    ensureInitialized();
    return Strings.isNullOrEmpty(table) ?
      excelDatasource.getValueTables().stream().findFirst().get() :
      excelDatasource.getValueTable(table);
  }

  @Override
  public String getURN() {
    return Strings.isNullOrEmpty(table) ? String.format("urn:file:%s", path) : String.format("urn:file:%s:%s", path, table);
  }

  @Override
  public void initialise(InputStream in) {
    excelDatasource = new ExcelDatasource(path, in);
  }

  private void ensureInitialized() {
    if (!initialized) {
      Initialisables.initialise(excelDatasource);
      initialized = true;
    }
  }
}
