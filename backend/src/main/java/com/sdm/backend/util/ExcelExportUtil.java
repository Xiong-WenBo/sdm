package com.sdm.backend.util;

import com.sdm.backend.annotation.ExcelColumn;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

public class ExcelExportUtil {

    /**
     * 导出 Excel 文件
     *
     * @param dataList 数据列表
     * @param clazz    实体类
     * @param sheetName 工作表名称
     * @param output   输出流
     * @throws IOException IO 异常
     */
    public static <T> void exportExcel(List<T> dataList, Class<T> clazz, String sheetName, OutputStream output) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        // 创建标题样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // 创建内容样式
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        Field[] fields = clazz.getDeclaredFields();
        int colIndex = 0;
        for (Field field : fields) {
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(excelColumn.name());
                cell.setCellStyle(headerStyle);
            }
        }

        // 填充数据
        int rowIndex = 1;
        for (T data : dataList) {
            Row row = sheet.createRow(rowIndex++);
            colIndex = 0;
            for (Field field : fields) {
                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(data);
                        Cell cell = row.createCell(colIndex++);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                        cell.setCellStyle(contentStyle);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < colIndex; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(output);
        workbook.close();
    }
}
