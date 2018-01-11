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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.web.model.Mica;

import static org.obiba.mica.dataset.search.rest.harmonization.ContingencyUtils.getTermsHeaders;
import static org.obiba.mica.dataset.search.rest.harmonization.ContingencyUtils.getValuesHeaders;

public class ExcelContingencyWriter {
  private static List<String> CONTINUOUS_VALUES = Lists.newArrayList("Min", "Max", "Mean", "Standard Deviation", "N");

  private DatasetVariable crossVariable;

  private DatasetVariable variable;

  private XSSFCellStyle headerStyle;

  private XSSFCellStyle tableStyle;

  private XSSFCellStyle titleStyle;

  public ExcelContingencyWriter(DatasetVariable variable, DatasetVariable crossVariable) {
    this.variable = variable;
    this.crossVariable = crossVariable;
  }

  public ByteArrayOutputStream write(Mica.DatasetVariableContingenciesDto dto) throws IOException {
    XSSFWorkbook workbook = createWorkbook();
    writeBody(workbook, dto);
    ByteArrayOutputStream ba = new ByteArrayOutputStream();
    workbook.write(ba);

    return ba;
  }

  public ByteArrayOutputStream write(Mica.DatasetVariableContingencyDto dto) throws IOException {
    XSSFWorkbook workbook = createWorkbook();
    writeBody(workbook, dto);
    ByteArrayOutputStream ba = new ByteArrayOutputStream();
    workbook.write(ba);

    return ba;
  }

  private XSSFWorkbook createWorkbook() {
    XSSFWorkbook workbook = new XSSFWorkbook();

    XSSFFont fontTitle = workbook.createFont();
    fontTitle.setBold(true);
    titleStyle = workbook.createCellStyle();
    titleStyle.setFont(fontTitle);
    titleStyle.setAlignment(CellStyle.ALIGN_CENTER);

    XSSFFont font = workbook.createFont();
    font.setBold(true);
    headerStyle = workbook.createCellStyle();
    headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(200, 200, 200)));
    headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    headerStyle.setFont(font);
    headerStyle.setBorderBottom(CellStyle.BORDER_THIN);
    headerStyle.setBorderTop(CellStyle.BORDER_THIN);
    headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
    headerStyle.setBorderRight(CellStyle.BORDER_THIN);
    headerStyle.setAlignment(CellStyle.ALIGN_CENTER);

    tableStyle = workbook.createCellStyle();
    tableStyle.setBorderBottom(CellStyle.BORDER_THIN);
    tableStyle.setBorderTop(CellStyle.BORDER_THIN);
    tableStyle.setBorderLeft(CellStyle.BORDER_THIN);
    tableStyle.setBorderRight(CellStyle.BORDER_THIN);

    return workbook;
  }

  private void writeBody(XSSFWorkbook workbook, Mica.DatasetVariableContingenciesDto dto) {
    List<String> terms = getTermsHeaders(variable, dto);
    List<String> values = getValuesHeaders(crossVariable, dto);

    for(Mica.DatasetVariableContingencyDto c : dto.getContingenciesList()) {
      addOpalTableSheet(workbook, c, terms, values);
    }

    XSSFSheet sheet = workbook.createSheet("All");
    Mica.DatasetVariableContingencyDto c = dto.getAll();
    writeTable(sheet, c, "All", terms, values);
  }

  private void writeBody(XSSFWorkbook workbook, Mica.DatasetVariableContingencyDto dto) {
    List<String> terms = getTermsHeaders(variable, dto);
    List<String> values = getValuesHeaders(crossVariable, dto);

    addOpalTableSheet(workbook, dto, terms, values);
  }

  private void addOpalTableSheet(XSSFWorkbook workbook, Mica.DatasetVariableContingencyDto dto, List<String> terms, List<String> values) {
    String tableName;

    if(dto.hasStudyTable()) {
      tableName = String.format("%s %s", dto.getStudyTable().getTable(), dto.getStudyTable().getDceId());
    } else {
      tableName = String.format("%s %s", dto.getHarmonizationStudyTable().getTable(), dto.getHarmonizationStudyTable().getPopulationId());
    }

    XSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(tableName, '-'));
    writeTable(sheet, dto, tableName, terms, values);
  }

  private void writeTable(XSSFSheet sheet, Mica.DatasetVariableContingencyDto c, String title, List<String> terms,
    List<String> values) {
    if("CONTINUOUS".equals(crossVariable.getNature())) {
      writeTable(sheet, ContingencyUtils.getContinuousRows(c, terms), title, terms, CONTINUOUS_VALUES, true);
    } else if("CATEGORICAL".equals(crossVariable.getNature())) {
      writeTable(sheet, ContingencyUtils.getCategoricalRows(c, values, terms), title, terms, values, false);
    }
  }

  private <T extends Number> void writeTable(XSSFSheet sheet, List<List<T>> tmp, String title, List<String> headers,
    List<String> values, boolean isContinuous) {
    writeTableHeaders(sheet, title, headers);

    ArrayList<String> rowHeaders = Lists.newArrayList(values);
    if(!isContinuous) rowHeaders.add("Total");

    int counter = 0;
    int rownum = 4;

    for(String k : rowHeaders) {
      int cellnum = 0;
      XSSFRow row = sheet.createRow(rownum++);
      XSSFCell cell = row.createCell(cellnum++);
      cell.setCellValue(k);
      cell.setCellStyle(headerStyle);

      for(T obj : tmp.get(counter)) {
        cell = row.createCell(cellnum++);
        cell.setCellStyle(tableStyle);

        if(obj instanceof Integer) cell.setCellValue((Integer) obj);
        else if(obj instanceof Float) cell.setCellValue((Float) obj);
        else cell.setCellValue(String.valueOf(obj));
      }

      counter++;
    }

    sheet.autoSizeColumn(0);
  }

  private void writeTableHeaders(XSSFSheet sheet, String title, List<String> headers) {
    int colNum = headers.size() + 1;

    IntStream.rangeClosed(0, 3).forEach(i -> {
      Row rowTemp = sheet.createRow(i);
      IntStream.rangeClosed(0, colNum).forEach(j -> rowTemp.createCell(j));
    });

    Row row = sheet.getRow(0);
    Cell titleLabel = row.getCell(0);
    titleLabel.setCellValue(title);
    titleLabel.setCellStyle(titleStyle);
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colNum));

    row = sheet.getRow(2);
    Cell crossVarLabel = row.getCell(0);
    crossVarLabel.setCellValue(this.crossVariable.getName());
    sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 0));

    Cell varLabel = row.getCell(1);
    varLabel.setCellValue(this.variable.getName());
    sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, colNum - 1));

    Cell totalLabel = row.getCell(colNum);
    totalLabel.setCellValue("Total");
    sheet.addMergedRegion(new CellRangeAddress(2, 3, colNum, colNum));

    int cellnum = 1;
    row = sheet.getRow(3);

    for(String h : headers) {
      Cell cell = row.getCell(cellnum++);
      cell.setCellValue(h);
    }

    addMergedStyles(sheet, new CellRangeAddress(2, 3, 0, colNum));
  }

  private void addMergedStyles(final XSSFSheet sheet, final CellRangeAddress r) {
    IntStream.rangeClosed(r.getFirstRow(), r.getLastRow()).forEach(i -> {
      final Row temp = sheet.getRow(i);
      IntStream.rangeClosed(r.getFirstColumn(), r.getLastColumn())
        .forEach(j -> temp.getCell(j).setCellStyle(headerStyle));
    });
  }
}
