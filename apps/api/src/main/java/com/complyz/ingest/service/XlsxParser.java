package com.complyz.ingest.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class XlsxParser {

    public record XlsxParsedData(List<String> headers, List<List<String>> rows) {}

    public XlsxParsedData parse(byte[] fileBytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return new XlsxParsedData(List.of(), List.of());
            }

            DataFormatter formatter = new DataFormatter();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            // Locate the header row after any leading institution title rows.
            int headerRowIdx = -1;
            for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 10); r++) {
                Row row = sheet.getRow(r);
                if (row != null && countNonEmptyCells(row, formatter) >= 3) {
                    headerRowIdx = r;
                    break;
                }
            }

            if (headerRowIdx == -1) {
                return new XlsxParsedData(List.of(), List.of());
            }

            Row headerRow = sheet.getRow(headerRowIdx);
            int totalCols = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < totalCols; c++) {
                Cell cell = headerRow.getCell(c);
                String val = cell != null ? formatter.formatCellValue(cell).trim() : "COL_" + (c + 1);
                headers.add(val.isBlank() ? "COL_" + (c + 1) : val);
            }

            List<List<String>> rows = new ArrayList<>();
            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                List<String> rowData = new ArrayList<>();
                boolean hasContent = false;

                for (int c = 0; c < totalCols; c++) {
                    Cell cell = row.getCell(c);
                    String cellVal = "";
                    if (cell != null) {
                        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                            cellVal = dateFormat.format(cell.getDateCellValue());
                        } else {
                            cellVal = formatter.formatCellValue(cell).trim();
                        }
                    }
                    if (!cellVal.isBlank()) {
                        hasContent = true;
                    }
                    rowData.add(cellVal);
                }

                // Skip blank rows and summary rows without populated columns.
                if (hasContent) {
                    rows.add(rowData);
                }
            }

            return new XlsxParsedData(headers, rows);
        } catch (Exception e) {
            throw new IOException("Could not parse Excel file (.xlsx): " + e.getMessage(), e);
        }
    }

    private int countNonEmptyCells(Row row, DataFormatter formatter) {
        int count = 0;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isBlank()) {
                count++;
            }
        }
        return count;
    }
}
