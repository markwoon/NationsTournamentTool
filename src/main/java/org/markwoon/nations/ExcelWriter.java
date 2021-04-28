package org.markwoon.nations;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.markwoon.nations.model.Game;


/**
 * Helper class to write out Excel file.
 *
 * @author Mark Woon
 */
public class ExcelWriter implements AutoCloseable {
  private final Workbook m_workbook;
  private final CellStyle m_headerStyle;
  private final CellStyle m_finishedStyle;
  private final CellStyle m_deadStyle;
  private final Sheet m_sheet;
  private Row m_headerRow;
  private Row m_currentRow;


  public ExcelWriter(String sheetName) {
    m_workbook = new HSSFWorkbook();
    m_sheet = m_workbook.createSheet(sheetName);

    m_headerStyle = m_workbook.createCellStyle();
    m_headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    m_headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    addBorder(m_headerStyle);
    Font boldFont = m_workbook.createFont();
    boldFont.setBold(true);
    m_headerStyle.setFont(boldFont);

    m_finishedStyle = m_workbook.createCellStyle();
    addBorder(m_finishedStyle);
    Font blueFont = m_workbook.createFont();
    blueFont.setColor(IndexedColors.ROYAL_BLUE.getIndex());
    m_finishedStyle.setFont(blueFont);

    m_deadStyle = m_workbook.createCellStyle();
    addBorder(m_deadStyle);
    Font redFont = m_workbook.createFont();
    redFont.setColor(IndexedColors.RED.getIndex());
    m_deadStyle.setFont(redFont);
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(m_workbook);
  }


  private int getNextCellId(Row row) {
    return row.getFirstCellNum() == -1 ? 0 : row.getLastCellNum();
  }


  public void writeHeader(String header) {
    if (m_headerRow == null) {
      m_headerRow = m_sheet.createRow(0);
    }
    Cell cell = m_headerRow.createCell(getNextCellId(m_headerRow));
    cell.setCellStyle(m_headerStyle);
    cell.setCellValue(header);
  }


  private void setCellStyle(Cell cell, Game game) {
    if (game != null) {
      if (game.isFinished()) {
        cell.setCellStyle(m_finishedStyle);
      } else if (game.isDead()) {
        cell.setCellStyle(m_deadStyle);
      }
    }
  }

  public void writeCell(String value, Game game) {
    if (m_currentRow == null) {
      m_currentRow = m_sheet.createRow(1);
    }
    Cell cell = m_currentRow.createCell(getNextCellId(m_currentRow));
    setCellStyle(cell, game);
    cell.setCellValue(value);
  }

  public void writeCell(double value, Game game) {
    if (m_currentRow == null) {
      m_currentRow = m_sheet.createRow(1);
    }
    Cell cell = m_currentRow.createCell(getNextCellId(m_currentRow));
    setCellStyle(cell, game);
    cell.setCellValue(value);
  }


  public void newRow() {
    m_currentRow = m_sheet.createRow(m_sheet.getLastRowNum() + 1);
  }


  public void save(Path file) throws IOException {
    for (int x = 0; x < m_headerRow.getLastCellNum(); x += 1) {
      m_sheet.autoSizeColumn(x);
    }
    try (OutputStream out = Files.newOutputStream(file)) {
      m_workbook.write(out);
    }
  }

  private  void addBorder(CellStyle style) {
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
  }
}
