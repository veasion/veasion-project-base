package cn.veasion.project.excel;

import cn.veasion.project.eval.EvalAnalysisUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MikuMikuExcelLayout
 *
 * @author zhangxiaoye
 * @author luozhuowei
 */
public class MikuMikuExcelLayout extends MikuMikuExcel {

    List<MikuMikuExcel> rows;
    List<MikuMikuExcel> cols;

    private MikuMikuExcelLayout(List<MikuMikuExcel> rows, List<MikuMikuExcel> cols) {
        this.rows = rows;
        this.cols = cols;
        if (rows != null) {
            List<Integer> allRowCols = rows.stream().map(MikuMikuExcel::getColNum).filter(Objects::nonNull).sorted().collect(Collectors.toList());
            if (!allRowCols.isEmpty()) {
                colNum = allRowCols.stream().max(Integer::compareTo).orElse(0);
                rows.forEach(mme -> mme.solve(null, colNum));
            }
            if (rows.stream().allMatch(mme -> mme.getRowNum() != null)) {
                rowNum = rows.stream().mapToInt(MikuMikuExcel::getRowNum).sum();
            }
        }
        if (cols != null) {
            List<Integer> allColRows = cols.stream().map(MikuMikuExcel::getRowNum).filter(Objects::nonNull).sorted().collect(Collectors.toList());
            if (!allColRows.isEmpty()) {
                rowNum = allColRows.stream().max(Integer::compareTo).orElse(0);
                cols.forEach(mme -> mme.solve(rowNum, null));
            }
            if (cols.stream().allMatch(mme -> mme.getColNum() != null)) {
                colNum = cols.stream().mapToInt(MikuMikuExcel::getColNum).sum();
            }
        }
    }

    @Override
    protected void travel(Consumer<MikuMikuExcelCell> consumer) {
        if (rows != null) {
            rows.forEach(o -> {
                if (o.getCellStyle() == null) {
                    o.setCellStyle(cellStyle);
                }
                o.travel(consumer);
            });
        }
        if (cols != null) {
            cols.forEach(o -> {
                if (o.getCellStyle() == null) {
                    o.setCellStyle(cellStyle);
                }
                o.travel(consumer);
            });
        }
    }

    @Override
    protected void solve(Integer rowNum, Integer colNum) {
        if (colNum != null) {
            this.colNum = colNum;
            if (rows != null) {
                rows.forEach(mme -> mme.solve(rowNum, colNum));
            }
            if (cols != null) {
                List<MikuMikuExcel> unknownCols = cols.stream().filter(mme -> mme.colNum == null).collect(Collectors.toList());
                if (!unknownCols.isEmpty()) {
                    int realColNum = colNum - cols.stream().map(MikuMikuExcel::getColNum).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
                    if (realColNum < 1) {
                        logger.info("已知列小于1无法自动计算扩列");
                    }
                    for (int i = 0; i < unknownCols.size(); i++) {
                        if (i < unknownCols.size() - 1) {
                            // 前面平均分配列
                            unknownCols.get(i).solve(rowNum, realColNum / unknownCols.size());
                        } else {
                            // 最后一个分剩下的列
                            unknownCols.get(i).solve(rowNum, realColNum / unknownCols.size() + realColNum % unknownCols.size());
                        }
                    }
                }
            }
        }
        if (rowNum != null) {
            this.rowNum = rowNum;
            if (cols != null) {
                cols.forEach(mme -> mme.solve(rowNum, colNum));
            }
            if (rows != null) {
                List<MikuMikuExcel> unknownRows = rows.stream().filter(mme -> mme.rowNum == null).collect(Collectors.toList());
                if (!unknownRows.isEmpty()) {
                    int realRowNum = rowNum - rows.stream().map(MikuMikuExcel::getRowNum).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
                    if (realRowNum < 1) {
                        logger.info("已知行小于1无法自动计算扩行");
                    }
                    for (int i = 0; i < unknownRows.size(); i++) {
                        if (i < unknownRows.size() - 1) {
                            // 前面平均分配行
                            unknownRows.get(i).solve(realRowNum / unknownRows.size(), colNum);
                        } else {
                            // 最后一个分剩下的行
                            unknownRows.get(i).solve(realRowNum / unknownRows.size() + realRowNum % unknownRows.size(), colNum);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void solveStart() {
        int lastStartRowIndex = startRowIndex;
        int lastStartColIndex = startColIndex;
        if (rows != null) {
            for (MikuMikuExcel excel : rows) {
                excel.startRowIndex = lastStartRowIndex;
                excel.startColIndex = lastStartColIndex;
                excel.solveStart();
                lastStartRowIndex += excel.getRowNum();
            }
        }
        if (cols != null) {
            for (MikuMikuExcel excel : cols) {
                excel.startRowIndex = lastStartRowIndex;
                excel.startColIndex = lastStartColIndex;
                excel.solveStart();
                lastStartColIndex += excel.getColNum();
            }
        }
    }

    /**
     * 多行
     */
    public static <T> MikuMikuExcel rows(Collection<T> data, Function<T, MikuMikuExcel> func) {
        return new MikuMikuExcelLayout(data.stream().map(func).collect(Collectors.toList()), null);
    }

    /**
     * 多行
     */
    public static <T> MikuMikuExcel rows(Object object, String eval, Function<Function<String, String>, MikuMikuExcel> func) {
        return new MikuMikuExcelLayout(dynamic(object, eval, func), null);
    }

    /**
     * 多行
     */
    public static MikuMikuExcel rows(MikuMikuExcel... rows) {
        return new MikuMikuExcelLayout(Arrays.asList(rows), null);
    }

    /**
     * 多列
     */
    public static <T> MikuMikuExcel cols(Collection<T> data, Function<T, MikuMikuExcel> func) {
        return new MikuMikuExcelLayout(null, data.stream().map(func).collect(Collectors.toList()));
    }

    /**
     * 多列
     */
    public static MikuMikuExcel cols(MikuMikuExcel... cols) {
        return new MikuMikuExcelLayout(null, Arrays.asList(cols));
    }

    /**
     * 多列
     */
    public static MikuMikuExcel cols(Object object, String eval, Function<Function<String, String>, MikuMikuExcel> func) {
        return new MikuMikuExcelLayout(null, dynamic(object, eval, func));
    }

    private static List<MikuMikuExcel> dynamic(Object object, String eval, Function<Function<String, String>, MikuMikuExcel> func) {
        List<?> list = (List<?>) EvalAnalysisUtils.eval(eval, object);
        List<MikuMikuExcel> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(func.apply(dynamicEval(eval, i)));
        }
        return result;
    }

    private static Function<String, String> dynamicEval(String eval, int index) {
        final String str = "${";
        return v -> {
            String fv = eval.trim();
            fv = fv.substring(0, fv.length() - 1).trim();
            if (v == null) {
                return fv.concat("[").concat(String.valueOf(index)).concat("]}");
            } else {
                while (v.contains(str + " ")) {
                    v = v.replace(str + " ", str);
                }
                return v.replace(str, fv.concat("[").concat(String.valueOf(index)).concat("]."));
            }
        };
    }

}
