package cn.veasion.project.excel;

import cn.veasion.project.eval.EvalAnalysisUtils;
import org.apache.poi.ss.usermodel.CellStyle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * MikuMikuExcelTable
 *
 * @author zhangxiaoye
 * @author luozhuowei
 */
public class MikuMikuExcelTable extends MikuMikuExcel {

    private MikuMikuExcelTable() {
    }

    /**
     * 表格
     */
    public static MikuMikuExcel of(LinkedHashMap<String, String> fieldNames, List<?> list) {
        return of(fieldNames, list, null, null);
    }

    /**
     * 表格
     */
    public static MikuMikuExcel of(LinkedHashMap<String, String> fieldNames, List<?> list, Callback cellCallback, CellStyle headerStyle) {
        return of(fieldNames, list, cellCallback, 1, headerStyle);
    }

    /**
     * 表格
     */
    public static MikuMikuExcel of(LinkedHashMap<String, String> fieldNames, List<?> list, Callback cellCallback, int headerRowNum, CellStyle headerStyle) {
        List<MikuMikuExcel> lines = new ArrayList<>(list.size() + 1);
        lines.add(MikuMikuExcelLayout.cols(fieldNames.values(), v -> MikuMikuExcelCell.fixedRows(headerRowNum, v)).setCellStyle(headerStyle));
        for (int i = 0; i < list.size(); i++) {
            int lineNumber = i + 1;
            Object data = list.get(i);
            if (data == null) {
                lines.add(MikuMikuExcelLayout.cols(fieldNames.keySet(), field -> MikuMikuExcelCell.fixed(null)));
            } else {
                lines.add(MikuMikuExcelLayout.cols(fieldNames.keySet(), field -> {
                    Object value = "lineNumber".equals(field) ? ExcelExportUtils.readField(data, field, lineNumber) : ExcelExportUtils.readField(data, field);
                    if (cellCallback != null) {
                        return MikuMikuExcelCell.fixedByFunction(cell -> cellCallback.callback(field, value, cell));
                    } else {
                        return MikuMikuExcelCell.fixed(value);
                    }
                }));
            }
        }
        return MikuMikuExcelLayout.rows(lines, Function.identity());
    }

    public static MikuMikuExcel ofEval(Object object, String eval, LinkedHashMap<String, String> fieldNames) {
        return ofEval(object, eval, fieldNames, null, null);
    }

    public static MikuMikuExcel ofEval(Object object, String eval, LinkedHashMap<String, String> fieldNames, Callback cellCallback, CellStyle headerStyle) {
        return ofEval(object, eval, fieldNames, cellCallback, 1, headerStyle);
    }

    public static MikuMikuExcel ofEval(Object object, String eval, LinkedHashMap<String, String> fieldNames, Callback cellCallback, int headerRowNum, CellStyle headerStyle) {
        return of(fieldNames, (List<?>) EvalAnalysisUtils.eval(eval, object), cellCallback, headerRowNum, headerStyle);
    }

    /**
     * 空表格
     */
    public static MikuMikuExcel empty(int rows, int cols) {
        return MikuMikuExcelCell.fixed(rows, cols, null);
    }

    @FunctionalInterface
    public interface Callback {
        Object callback(String field, Object value, MikuMikuExcelCell cell);
    }

}
