package cn.veasion.project.excel;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MikuMikuExcelCell
 *
 * @author zhangxiaoye
 * @author luozhuowei
 */
public class MikuMikuExcelCell extends MikuMikuExcel {

    Object value;

    private MikuMikuExcelCell(Integer rowNum, Integer colNum) {
        this(rowNum, colNum, null);
    }

    private MikuMikuExcelCell(Integer rowNum, Integer colNum, Object value) {
        this.rowNum = rowNum;
        this.colNum = colNum;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    protected void travel(Consumer<MikuMikuExcelCell> consumer) {
        consumer.accept(this);
    }

    @Override
    protected void solve(Integer rowNum, Integer colNum) {
        if (rowNum != null) {
            this.rowNum = rowNum;
        }
        if (colNum != null) {
            this.colNum = colNum;
        }
    }

    /**
     * 固定 1 行 1 列
     */
    public static MikuMikuExcel fixedByFunction(Function<MikuMikuExcelCell, Object> function) {
        MikuMikuExcelCell cell = new MikuMikuExcelCell(1, 1);
        cell.value = function.apply(cell);
        return cell;
    }

    /**
     * 固定 1 行 1 列
     */
    public static MikuMikuExcel fixed(Object v) {
        return new MikuMikuExcelCell(1, 1, v);
    }

    /**
     * 固定 rowNum 行 1 列
     */
    public static MikuMikuExcel fixedRows(int rowNum, Object v) {
        return new MikuMikuExcelCell(rowNum, 1, v);
    }

    /**
     * 固定 1 行 colNum 列
     */
    public static MikuMikuExcel fixedCols(int colNum, Object v) {
        return new MikuMikuExcelCell(1, colNum, v);
    }

    /**
     * 固定 rowNum 行 colNum 列
     */
    public static MikuMikuExcel fixed(int rowNum, int colNum, Object v) {
        return new MikuMikuExcelCell(rowNum, colNum, v);
    }

    /**
     * 自动扩高，固定1列
     */
    public static MikuMikuExcel dynamicRows(Object v) {
        return new MikuMikuExcelCell(null, 1, v);
    }

    /**
     * 自动扩高，固定 colNum 列
     */
    public static MikuMikuExcel dynamicRows(int colNum, Object v) {
        return new MikuMikuExcelCell(null, colNum, v);
    }

    /**
     * 自动扩高，固定 colNum 列
     */
    public static MikuMikuExcel dynamicRows(int colNum, Object v, boolean merge) {
        MikuMikuExcel excel = dynamicRows(colNum, v);
        excel.merge = merge;
        return excel;
    }

    /**
     * 自动扩宽，固定1行
     */
    public static MikuMikuExcel dynamicCols(Object v) {
        return new MikuMikuExcelCell(1, null, v);
    }

    /**
     * 自动扩宽，固定 rowNum 行
     */
    public static MikuMikuExcel dynamicCols(int rowNum, Object v) {
        return new MikuMikuExcelCell(rowNum, null, v);
    }

    /**
     * 自动扩宽，固定 rowNum 行
     */
    public static MikuMikuExcel dynamicCols(int rowNum, Object v, boolean merge) {
        MikuMikuExcel excel = dynamicCols(rowNum, v);
        excel.merge = merge;
        return excel;
    }

}
