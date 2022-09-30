package cn.veasion.project.excel;

import cn.veasion.project.eval.EvalAnalysisUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ExcelUtils
 *
 * @author luozhuowei
 * @date 2021/9/21
 */
public class ExcelExportUtils {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Sheet createSheet(Workbook workbook, MikuMikuExcel mikuMikuExcel) {
        return createSheet(workbook, mikuMikuExcel, null);
    }

    public static Sheet createSheet(Workbook workbook, MikuMikuExcel mikuMikuExcel, Object object) {
        return createSheet(workbook, mikuMikuExcel, object, null);
    }

    public static Sheet createSheet(Workbook workbook, MikuMikuExcel mikuMikuExcel, Object object, String sheetName) {
        return createSheet(workbook, mikuMikuExcel, object, sheetName, 0, 0);
    }

    public static Sheet createSheet(Workbook workbook, MikuMikuExcel mikuMikuExcel, Object object, String sheetName, int startRowIndex, int startColIndex) {
        Sheet sheet;
        if (sheetName != null) {
            sheet = workbook.createSheet(sheetName);
        } else {
            sheet = workbook.createSheet();
        }
        mikuMikuExcel.writeTo(workbook, sheet, getDefaultCellStyle(workbook), object, startRowIndex, startColIndex);
        return sheet;
    }

    public static void appendToRight(Workbook workbook, Sheet sheet, MikuMikuExcel mikuMikuExcel, Object object, int gapCol, int startRowIndex) {
        int maxCellNum = 0;
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            if (sheet.getRow(i) == null) {
                continue;
            }
            int number = sheet.getRow(i).getLastCellNum();
            if (number > maxCellNum) {
                maxCellNum = number;
            }
        }
        int startColIndex = maxCellNum + gapCol;
        mikuMikuExcel.writeTo(workbook, sheet, getDefaultCellStyle(workbook), object, startRowIndex, startColIndex);
    }

    public static void appendToDown(Workbook workbook, Sheet sheet, MikuMikuExcel mikuMikuExcel, Object object, int gapRow, int startColIndex) {
        int startRowIndex = sheet.getLastRowNum() + 1 + gapRow;
        mikuMikuExcel.writeTo(workbook, sheet, getDefaultCellStyle(workbook), object, startRowIndex, startColIndex);
    }

    public static void append(Workbook workbook, Sheet sheet, MikuMikuExcel mikuMikuExcel, Object object, int startRowIndex, int startColIndex) {
        mikuMikuExcel.writeTo(workbook, sheet, getDefaultCellStyle(workbook), object, startRowIndex, startColIndex);
    }

    public static Sheet createSheet(Workbook workbook, ExcelConfig excelConfig, List<?> list) {
        return createSheet(workbook, null, excelConfig, list);
    }

    public static Sheet createSheet(Workbook workbook, String sheetName, ExcelConfig excelConfig, List<?> list) {
        Sheet sheet;
        if (sheetName != null) {
            sheet = workbook.createSheet(sheetName);
        } else {
            sheet = workbook.createSheet();
        }
        if (excelConfig.getHeadCellStyle() == null) {
            excelConfig.setHeadCellStyle(getDefaultCellStyle(workbook));
        }
        if (excelConfig.getDefaultCellStyle() == null) {
            excelConfig.setDefaultCellStyle(getDefaultCellStyle(workbook));
        }
        createTable(sheet, 0, 0, excelConfig, list);
        return sheet;
    }

    public static int createTable(Sheet sheet, int rowIndex, int colIndex, ExcelConfig excelConfig, List<?> list) {
        boolean hiddenHead = excelConfig.isHiddenHead();
        LinkedHashMap<String, ExcelConfig.FieldConfig> fieldMap = excelConfig.getFieldMap();
        String[] fields = fieldMap.keySet().toArray(new String[]{});
        List<String> names = fieldMap.values().stream().map(ExcelConfig.FieldConfig::getName).collect(Collectors.toList());
        Cell cell;
        if (!hiddenHead) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < names.size(); i++) {
                cell = row.createCell(colIndex + i);
                cell.setCellValue(names.get(i));
                cell.setCellStyle(excelConfig.getHeadCellStyle());
            }
        }
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            Row cellRow = sheet.createRow(rowIndex++);
            for (int j = 0; j < fields.length; j++) {
                cell = cellRow.createCell(colIndex + j);
                Object value = readField(obj, fields[j]);
                ExcelConfig.FieldConfig fieldConfig = fieldMap.get(fields[j]);
                CellStyle cellStyle = fieldConfig.getCellStyle() != null ? fieldConfig.getCellStyle() : excelConfig.getDefaultCellStyle();
                if (cellStyle != null) {
                    cell.setCellStyle(cellStyle);
                }
                if (fieldConfig.getCallback() != null) {
                    value = fieldConfig.getCallback().callback(fields[j], value, cell);
                }
                if (value == null) {
                    cell.setCellValue("");
                    continue;
                }
                if (value instanceof Date) {
                    Date date = (Date) value;
                    String dateFormat = fieldConfig.getDateFormat();
                    if (dateFormat != null) {
                        cell.setCellValue(new SimpleDateFormat(dateFormat).format(date));
                    } else {
                        cell.setCellValue(date);
                    }
                } else if (value instanceof Number) {
                    DecimalFormat decimalFormat = excelConfig.getDecimalFormat();
                    if (decimalFormat != null) {
                        cell.setCellValue(decimalFormat.format(value));
                    } else {
                        cell.setCellValue(((Number) value).doubleValue());
                    }
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
        }
        return rowIndex;
    }

    public static void export(HttpServletResponse response, String fileName, String[] headers) throws IOException {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (String header : headers) {
            map.put(header, header);
        }
        export(response, fileName, map, null);
    }

    public static void export(HttpServletResponse response, String fileName, LinkedHashMap<String, String> fieldMap, List<?> list) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        createTable(workbook, fieldMap, list);
        toResponse(response, workbook, fileName);
    }

    public static void createTable(Workbook workbook, LinkedHashMap<String, String> fieldMap, List<?> list) {
        Sheet sheet = workbook.createSheet();
        createTable(sheet, getDefaultCellStyle(workbook), fieldMap, list);
    }

    public static void createTable(Sheet sheet, CellStyle cellStyle, LinkedHashMap<String, String> fieldMap, List<?> list) {
        int rowIndex = 0;
        String[] fields = fieldMap.keySet().toArray(new String[]{});
        List<String> names = new ArrayList<>(fieldMap.values());
        Cell cell;
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < names.size(); i++) {
            cell = row.createCell(i);
            cell.setCellValue(names.get(i));
            cell.setCellStyle(cellStyle);
        }
        if (list == null || list.isEmpty()) {
            return;
        }
        JSONArray jsonArray = (JSONArray) JSON.toJSON(list);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Row cellRow = sheet.createRow(rowIndex++);
            for (int j = 0; j < fields.length; j++) {
                cell = cellRow.createCell(j);
                Object value = obj.get(fields[j]);
                cell.setCellStyle(cellStyle);
                if (value == null) {
                    cell.setCellValue("");
                    continue;
                }
                if (value instanceof Date) {
                    cell.setCellValue(new SimpleDateFormat(DATE_FORMAT).format((Date) value));
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
        }
    }

    public static void selectOptions(Sheet sheet, int row, int col, String... types) {
        XSSFDataValidationHelper dvHelper2 = new XSSFDataValidationHelper((XSSFSheet) sheet);
        XSSFDataValidationConstraint dvConstraint2 = (XSSFDataValidationConstraint) dvHelper2.createExplicitListConstraint(types);
        CellRangeAddressList regions2 = new CellRangeAddressList(row, row + 5000, col, col);
        XSSFDataValidation dataValidation2 = (XSSFDataValidation) dvHelper2.createValidation(dvConstraint2, regions2);
        sheet.addValidationData(dataValidation2);
    }

    public static CellStyle getDefaultCellStyle(Workbook workbook) {
        return getDefaultCellStyle(workbook, false);
    }

    public static CellStyle getDefaultCellStyle(Workbook workbook, boolean border) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        if (border) {
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
        }
        return cellStyle;
    }

    static Object readField(Object obj, String field, Object defaultValue) {
        try {
            Object value = readField(obj, field);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static Object readField(Object obj, String field) {
        if (obj instanceof Map && ((Map<?, ?>) obj).containsKey(field)) {
            return ((Map<?, ?>) obj).get(field);
        }
        return EvalAnalysisUtils.parse(field, obj);
    }

    public static void toResponse(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        setResponseHeader(response, fileName);
        workbook.write(response.getOutputStream());
    }

    public static void setResponseHeader(HttpServletResponse response, String fileName) {
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.addHeader("Pargam", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
    }

}
