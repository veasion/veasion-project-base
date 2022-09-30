package cn.veasion.project.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * ExcelStyleUtils
 *
 * @author luozhuowei
 * @date 2021/11/17
 */
public class ExcelStyleUtils {

    public static CellStyle getDefaultCellStyle(Workbook workbook) {
        return getDefaultCellStyle(workbook, true);
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

    public static CellStyle cloneCellStyle(Workbook workbook, Cell cell) {
        CellStyle cellStyle = workbook.createCellStyle();
        if (cell.getCellStyle() != null) {
            cellStyle.cloneStyleFrom(cell.getCellStyle());
        }
        cell.setCellStyle(cellStyle);
        return cellStyle;
    }

    public static XSSFColor getColor(int r, int g, int b) {
        return new XSSFColor(new Color(r, g, b));
    }

    public static CellStyle color(CellStyle cellStyle, int r, int g, int b, BiConsumer<XSSFCellStyle, XSSFColor> consumer) {
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        consumer.accept((XSSFCellStyle) cellStyle, getColor(r, g, b));
        return cellStyle;
    }

    public static CellStyle font(Workbook workbook, boolean border, Consumer<Font> consumer) {
        CellStyle cellStyle = getDefaultCellStyle(workbook, border);
        Font font = workbook.createFont();
        consumer.accept(font);
        cellStyle.setFont(font);
        return cellStyle;
    }

    public static CellStyle style(Workbook workbook, boolean border, Consumer<CellStyle> consumer) {
        CellStyle cellStyle = getDefaultCellStyle(workbook, border);
        consumer.accept(cellStyle);
        return cellStyle;
    }

    public static CellStyle style(Workbook workbook, boolean border, BiConsumer<CellStyle, Font> consumer) {
        CellStyle cellStyle = getDefaultCellStyle(workbook, border);
        Font font = workbook.createFont();
        consumer.accept(cellStyle, font);
        cellStyle.setFont(font);
        return cellStyle;
    }

}
