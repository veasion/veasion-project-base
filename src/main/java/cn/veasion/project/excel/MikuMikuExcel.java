package cn.veasion.project.excel;

import cn.veasion.project.eval.EvalAnalysisUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * MikuMikuExcel
 *
 * @author zhangxiaoye
 * @author luozhuowei
 */
public abstract class MikuMikuExcel {

    protected static Logger logger = LoggerFactory.getLogger(MikuMikuExcel.class);

    int startRowIndex;
    int startColIndex;
    Integer rowNum;
    Integer colNum;
    CellStyle cellStyle;
    boolean merge = true;
    BiConsumer<Workbook, Cell> afterCell;
    String dateFormat = "yyyy-MM-dd HH:mm:ss";

    public MikuMikuExcel setCellStyle(CellStyle cellStyle) {
        this.cellStyle = cellStyle;
        return this;
    }

    public MikuMikuExcel setAfterCell(BiConsumer<Workbook, Cell> afterCell) {
        this.afterCell = afterCell;
        return this;
    }

    public MikuMikuExcel setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    protected void travel(Consumer<MikuMikuExcelCell> visitor) {
    }

    protected void solve(Integer rowNum, Integer colNum) {
    }

    protected void solveStart() {
    }

    public boolean isMerge() {
        return merge;
    }

    public int getStartRowIndex() {
        return startRowIndex;
    }

    public int getStartColIndex() {
        return startColIndex;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public Integer getColNum() {
        return colNum;
    }

    public CellStyle getCellStyle() {
        return cellStyle;
    }

    public void writeTo(Workbook workbook, Sheet sheet, CellStyle defaultCellStyle) {
        writeTo(workbook, sheet, defaultCellStyle, null);
    }

    public void writeTo(Workbook workbook, Sheet sheet, CellStyle defaultCellStyle, Object object) {
        writeTo(workbook, sheet, defaultCellStyle, object, 0, 0);
    }

    @SuppressWarnings("unchecked")
    public void writeTo(Workbook workbook, Sheet sheet, CellStyle defaultCellStyle, Object object, int startRowIndex, int startColIndex) {
        MikuMikuExcel excel = this;

        excel.travel(ve -> {
            if (ve.getRowNum() == null || ve.getColNum() == null) {
                throw new RuntimeException(String.format("存在未确定行数或者列数的单元格: %s", ve.getValue()));
            }
        });
        excel.solveStart();

        for (int rowIndex = 0; rowIndex < excel.getRowNum(); rowIndex++) {
            Row row;
            if (sheet.getRow(rowIndex + startRowIndex) != null) {
                row = sheet.getRow(rowIndex + startRowIndex);
            } else {
                row = sheet.createRow(rowIndex + startRowIndex);
            }
            excel.travel(mikuMikuExcelCell -> {
                if (mikuMikuExcelCell.getStartRowIndex() + startRowIndex == row.getRowNum()) {
                    Cell cell = row.createCell(mikuMikuExcelCell.getStartColIndex() + startColIndex);
                    CellStyle cellStyle = mikuMikuExcelCell.getCellStyle() != null ? mikuMikuExcelCell.getCellStyle() : defaultCellStyle;
                    if (cellStyle != null) {
                        cell.setCellStyle(cellStyle);
                    }
                    Object value = mikuMikuExcelCell.getValue();
                    if (value instanceof String && object != null && ((String) value).contains("${")) {
                        value = EvalAnalysisUtils.eval(value.toString(), object);
                        if (value instanceof BiFunction) {
                            value = ((BiFunction<Workbook, Cell, String>) value).apply(workbook, cell);
                        }
                    }
                    if (value instanceof Date) {
                        cell.setCellValue(new SimpleDateFormat(dateFormat).format((Date) value));
                    } else {
                        cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                    if (mikuMikuExcelCell.afterCell != null) {
                        mikuMikuExcelCell.afterCell.accept(workbook, cell);
                    }
                }
            });
        }

        excel.travel(mikuMikuExcelCell -> {
            CellStyle cellStyle = mikuMikuExcelCell.getCellStyle() != null ? mikuMikuExcelCell.getCellStyle() : defaultCellStyle;
            if (mikuMikuExcelCell.getRowNum() > 1 || mikuMikuExcelCell.getColNum() > 1) {
                int rowNumber = mikuMikuExcelCell.getStartRowIndex() + mikuMikuExcelCell.getRowNum() + startRowIndex;
                int colNumber = mikuMikuExcelCell.getStartColIndex() + mikuMikuExcelCell.getColNum() + startColIndex;
                CellRangeAddress range = new CellRangeAddress(mikuMikuExcelCell.getStartRowIndex() + startRowIndex, rowNumber - 1, mikuMikuExcelCell.getStartColIndex() + startColIndex, colNumber - 1);
                if (mikuMikuExcelCell.isMerge()) {
                    sheet.addMergedRegion(range);
                    if (cellStyle != null) {
                        RegionUtil.setBorderBottom(cellStyle.getBorderBottom(), range, sheet);
                        RegionUtil.setBorderLeft(cellStyle.getBorderLeft(), range, sheet);
                        RegionUtil.setBorderRight(cellStyle.getBorderRight(), range, sheet);
                        RegionUtil.setBorderTop(cellStyle.getBorderTop(), range, sheet);
                    }
                } else if (cellStyle != null) {
                    for (int row = mikuMikuExcelCell.getStartRowIndex() + startRowIndex; row < rowNumber; row++) {
                        for (int col = mikuMikuExcelCell.getStartColIndex() + startColIndex; col < colNumber; col++) {
                            Row sheetRow = sheet.getRow(row);
                            if (sheetRow == null) {
                                sheetRow = sheet.createRow(row);
                            }
                            Cell cell = sheetRow.getCell(col);
                            if (cell == null) {
                                cell = sheetRow.createCell(col);
                                cell.setCellValue("");
                            }
                            cell.setCellStyle(cellStyle);
                        }
                    }
                }
            }
        });
    }
}
