package com.foodzie.auth_service.util;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

public class ExcelParsingUtil {

    /**
     * Parse users from Excel file.
     * Expected columns: email, username, password, fullName, role
     */
    public static List<Map<String, String>> parseUserExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> users = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row to map column indices
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = getColumnMap(headerRow);
            
            // Parse data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> userData = new HashMap<>();
                userData.put("rowNumber", String.valueOf(i + 1));
                userData.put("email", getCellValue(row, columnMap, "email"));
                userData.put("username", getCellValue(row, columnMap, "username"));
                userData.put("password", getCellValue(row, columnMap, "password"));
                userData.put("fullName", getCellValue(row, columnMap, "fullname"));
                userData.put("role", getCellValue(row, columnMap, "role"));
                
                users.add(userData);
            }
        }
        
        return users;
    }

    private static Map<String, Integer> getColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = cell.getStringCellValue().toLowerCase().trim();
                columnMap.put(header, i);
            }
        }
        return columnMap;
    }

    private static String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
        Integer columnIndex = columnMap.get(columnName);
        if (columnIndex == null) return "";
        
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
