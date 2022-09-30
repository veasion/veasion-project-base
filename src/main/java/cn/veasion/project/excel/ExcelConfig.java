package cn.veasion.project.excel;

import org.apache.poi.ss.usermodel.CellStyle;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;

/**
 * ExcelConfig
 *
 * @author luozhuowei
 * @date 2021/8/7
 */
public class ExcelConfig {

    private boolean hiddenHead = false;
    private CellStyle headCellStyle;
    private CellStyle defaultCellStyle;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private LinkedHashMap<String, FieldConfig> fieldMap = new LinkedHashMap<>();

    public ExcelConfig make(String field, String name) {
        fieldMap.put(field, FieldConfig.of(name));
        return this;
    }

    public ExcelConfig make(String field, FieldConfig fieldConfig) {
        fieldMap.put(field, fieldConfig);
        return this;
    }

    public ExcelConfig make(LinkedHashMap<String, String> fieldNames) {
        fieldNames.forEach((k, v) -> fieldMap.put(k, FieldConfig.of(v)));
        return this;
    }

    public LinkedHashMap<String, FieldConfig> getFieldMap() {
        return fieldMap;
    }

    public CellStyle getHeadCellStyle() {
        return headCellStyle;
    }

    public ExcelConfig setHeadCellStyle(CellStyle headCellStyle) {
        this.headCellStyle = headCellStyle;
        return this;
    }

    public CellStyle getDefaultCellStyle() {
        return defaultCellStyle;
    }

    public ExcelConfig setDefaultCellStyle(CellStyle defaultCellStyle) {
        this.defaultCellStyle = defaultCellStyle;
        return this;
    }

    public DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public ExcelConfig setDecimalFormat(DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
        return this;
    }

    public ExcelConfig setHiddenHead(boolean hiddenHead) {
        this.hiddenHead = hiddenHead;
        return this;
    }

    public boolean isHiddenHead() {
        return hiddenHead;
    }

    public static class FieldConfig {

        private String name;
        private CellStyle cellStyle;
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";
        private FieldCellCallback callback;

        public FieldConfig(String name) {
            this.name = name;
        }

        public static FieldConfig of(String name) {
            return new FieldConfig(name);
        }

        public FieldConfig cellStyle(CellStyle cellStyle) {
            this.cellStyle = cellStyle;
            return this;
        }

        public FieldConfig callback(FieldCellCallback callback) {
            this.callback = callback;
            return this;
        }

        public FieldConfig dateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public String getName() {
            return name;
        }

        public CellStyle getCellStyle() {
            return cellStyle;
        }

        public FieldCellCallback getCallback() {
            return callback;
        }

        public String getDateFormat() {
            return dateFormat;
        }
    }
}
