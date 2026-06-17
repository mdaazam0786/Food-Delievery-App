package com.foodzie.restaurant_service.util;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

public class ExcelParsingUtil {

    /**
     * Parse restaurants from Excel file.
     * Expected columns: name, description, addressText, latitude, longitude, gstNo, fssaiNo, ownerEmail, imageUrl
     */
    public static List<Map<String, String>> parseRestaurantExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> restaurants = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row to map column indices
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = getColumnMap(headerRow);
            
            // Parse data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> restaurantData = new HashMap<>();
                restaurantData.put("rowNumber", String.valueOf(i + 1));
                restaurantData.put("name", getCellValue(row, columnMap, "name"));
                restaurantData.put("description", getCellValue(row, columnMap, "description"));
                restaurantData.put("addressText", getCellValue(row, columnMap, "addressText"));
                restaurantData.put("latitude", getCellValue(row, columnMap, "latitude"));
                restaurantData.put("longitude", getCellValue(row, columnMap, "longitude"));
                restaurantData.put("gstNo", getCellValue(row, columnMap, "gstNo"));
                restaurantData.put("fssaiNo", getCellValue(row, columnMap, "fssaiNo"));
                restaurantData.put("ownerEmail", getCellValue(row, columnMap, "owneremail"));
                restaurantData.put("imageUrl", getCellValue(row, columnMap, "imageUrl"));
                
                restaurants.add(restaurantData);
            }
        }
        
        return restaurants;
    }

    /**
     * Parse menu items from Excel file.
     * Expected columns: name, description, price, category, isVeg, imageUrl
     */
    public static List<Map<String, String>> parseMenuExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> menuItems = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row to map column indices
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = getColumnMap(headerRow);
            
            // Parse data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> itemData = new HashMap<>();
                itemData.put("rowNumber", String.valueOf(i + 1));
                itemData.put("name", getCellValue(row, columnMap, "name"));
                itemData.put("description", getCellValue(row, columnMap, "description"));
                itemData.put("price", getCellValue(row, columnMap, "price"));
                itemData.put("category", getCellValue(row, columnMap, "category"));
                itemData.put("isVeg", getCellValue(row, columnMap, "isVeg"));
                itemData.put("imageUrl", getCellValue(row, columnMap, "imageurl"));
                
                menuItems.add(itemData);
            }
        }
        
        return menuItems;
    }

    private static Map<String, Integer> getColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = headerRow.getCell(i).getStringCellValue().toLowerCase().trim();
            columnMap.put(header, i);
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
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
