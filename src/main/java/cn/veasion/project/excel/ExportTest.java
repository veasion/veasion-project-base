package cn.veasion.project.excel;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * ExportTest
 *
 * @author luozhuowei
 * @date 2021/8/7
 */
public class ExportTest {

    private static String DIR = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();

    public static void main(String[] args) throws Exception {
        List<ExportTest> list = new ArrayList<>();
        list.add(build(1L, "小明", 1));
        list.add(build(2L, "小红", 2));
        list.add(build(3L, "小张", 1));

        // 简单 Excel
        simpleTable(list);

        // 复杂 Excel
        mikuMikuTable(list);

        // 复杂模板 Excel
        mikuMikuTableOfEval(list);

        System.out.println("完成！");
    }

    private static void simpleTable(List<ExportTest> list) throws Exception {

        Workbook workbook = new XSSFWorkbook();
        ExcelConfig excelConfig = new ExcelConfig();
        excelConfig.make("id", "ID");
        excelConfig.make("name", "名称");
        excelConfig.make("sex", ExcelConfig.FieldConfig.of("性别").callback((f, v, c) -> "1".equals(String.valueOf(v)) ? "男" : "女"));
        excelConfig.make("birthday", "生日");

        ExcelExportUtils.createSheet(workbook, excelConfig, list);
        workbook.write(new FileOutputStream(new File(DIR + "\\simple.xlsx")));
    }

    private static void mikuMikuTable(List<ExportTest> list) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        MikuMikuExcel mikuMikuExcel = MikuMikuExcelLayout.cols(
                MikuMikuExcelLayout.rows(
                        MikuMikuExcelCell.dynamicCols(2, "测试表格"),
                        MikuMikuExcelLayout.cols(
                                MikuMikuExcelTable.of(new LinkedHashMap<String, String>() {{
                                    put("id", "ID");
                                    put("name", "名称");
                                    put("sex", "性别");
                                    put("birthday", "生日");
                                }}, list, (field, value, cell) -> {
                                    if ("sex".equals(field)) {
                                        return "1".equals(String.valueOf(value)) ? "男" : "女";
                                    }
                                    return value;
                                }, null),
                                MikuMikuExcelLayout.rows(
                                        MikuMikuExcelCell.dynamicCols("排行"),
                                        MikuMikuExcelLayout.rows(list, data ->
                                                MikuMikuExcelLayout.cols(
                                                        MikuMikuExcelCell.fixed(data.id),
                                                        MikuMikuExcelCell.fixed(data.name)
                                                )
                                        )
                                )
                        ),
                        MikuMikuExcelCell.dynamicCols(null),
                        MikuMikuExcelCell.dynamicCols("======== ↓ ========"),
                        MikuMikuExcelCell.dynamicCols(null),
                        MikuMikuExcelLayout.rows(list, data ->
                                MikuMikuExcelLayout.cols(
                                        MikuMikuExcelCell.dynamicCols(data.name),
                                        MikuMikuExcelCell.dynamicCols(data.birthday)
                                )
                        )
                ),
                MikuMikuExcelCell.dynamicRows("测试1"),
                MikuMikuExcelLayout.rows(
                        MikuMikuExcelCell.dynamicRows("测试2"),
                        MikuMikuExcelCell.dynamicRows("测试3")
                )
        );

        ExcelExportUtils.createSheet(workbook, mikuMikuExcel);
        workbook.write(new FileOutputStream(new File(DIR + "\\mikuMiku.xlsx")));
    }

    private static void mikuMikuTableOfEval(List<ExportTest> list) throws Exception {

        JSONObject object = new JSONObject();
        object.put("name", "项目名称");
        object.put("code", "项目编码");
        object.put("purchaserName", "哈哈");
        object.put("createTime", new Date());
        object.put("headerList", list);
        object.put("bidSupperNames", "xxx供应商、哈哈哈供应商");

        object.put("itemList", Arrays.asList(
                JSONObject.parseObject("{\"merchantName\":\"xxx供应商1\",\"purchasePlanCode\":\"xxx1\",\"productNum\": 100}"),
                JSONObject.parseObject("{\"merchantName\":\"xxx供应商2\",\"purchasePlanCode\":\"xxx2\",\"productNum\": 200}")
        ));
        object.put("quotationItems", Arrays.asList(
                JSONObject.parseObject("{\"supplierName\":\"供应商1\",\"items\":[{\"discountRate\": 0.1,\"winExp\": 1,\"productBrand\":\"中国1\"}, {\"discountRate\": 0.11,\"winExp\": 11,\"productBrand\":\"中国11\"}]}"),
                JSONObject.parseObject("{\"supplierName\":\"供应商2\",\"items\":[{\"discountRate\": 0.2,\"winExp\": 2,\"productBrand\":\"中国2\"}, {\"discountRate\": 0.22,\"winExp\": 22,\"productBrand\":\"中国22\"}]}")
        ));

        Workbook workbook = new XSSFWorkbook();

        MikuMikuExcel mikuMikuExcel = MikuMikuExcelLayout.rows(
                MikuMikuExcelCell.dynamicCols("${name}综合评议表"),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.dynamicCols(1, null, false), MikuMikuExcelCell.fixed("编号：${code}")),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.dynamicCols(1, null, false), MikuMikuExcelCell.fixed("${createTime}")),
                MikuMikuExcelTable.ofEval(object, "${headerList}", new LinkedHashMap<String, String>() {{
                    put("lineNumber", "序号");
                    put("id", "ID");
                    put("name", "名称");
                    put("sex", "性别");
                    put("birthday", "生日");
                }}),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.fixed("评议标准"), MikuMikuExcelCell.dynamicCols(null)),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.fixed("中标规则"), MikuMikuExcelCell.dynamicCols(null)),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.fixed("拟中标单位："), MikuMikuExcelCell.dynamicCols("${bidSupperNames}")),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.fixed("理由"), MikuMikuExcelCell.dynamicCols(null)),
                MikuMikuExcelLayout.cols(MikuMikuExcelCell.fixed("备注"), MikuMikuExcelCell.dynamicCols(null)),
                MikuMikuExcelLayout.cols(
                        MikuMikuExcelCell.fixed("总经理："), MikuMikuExcelCell.dynamicCols(null),
                        MikuMikuExcelCell.fixed("评标人员："), MikuMikuExcelCell.dynamicCols(null),
                        MikuMikuExcelCell.fixed("招标人"), MikuMikuExcelCell.dynamicCols("${purchaserName}"),
                        MikuMikuExcelCell.fixed("监督人员")
                )
        );

        MikuMikuExcel mikuMikuExcel2 = MikuMikuExcelLayout.rows(
                MikuMikuExcelCell.dynamicCols("${name}"),
                MikuMikuExcelLayout.cols(
                        MikuMikuExcelLayout.cols(
                                MikuMikuExcelLayout.rows(
                                        MikuMikuExcelCell.dynamicCols("供应商名称").setAfterCell((wb, cell) -> ExcelStyleUtils.cloneCellStyle(wb, cell).setAlignment(HorizontalAlignment.LEFT)),
                                        MikuMikuExcelTable.ofEval(object, "${itemList}", new LinkedHashMap<String, String>() {{
                                            put("lineNumber", "序号");
                                            put("merchantName", "采购单位名称");
                                            put("productName", "物料名称");
                                            put("productSpecs", "系列");
                                            put("productUnit", "计量单位");
                                            put("remark", "备注");
                                        }})
                                )
                        ),
                        MikuMikuExcelLayout.cols(object, "${quotationItems}", parent ->
                                MikuMikuExcelLayout.rows(
                                        MikuMikuExcelCell.dynamicCols(parent.apply("${supplierName}")),
                                        MikuMikuExcelTable.ofEval(object, parent.apply("${items}"), new LinkedHashMap<String, String>() {{
                                            put("discountRate", "折扣率");
                                            put("winExp", "质保期（月）");
                                            put("productBrand", "品牌/产地");
                                            put("remark", "备注");
                                        }})
                                )
                        )
                )
        );
        Sheet sheet = ExcelExportUtils.createSheet(workbook, mikuMikuExcel, object);
        ExcelExportUtils.appendToDown(workbook, sheet, mikuMikuExcel2, object, 2, 2);
        ExcelExportUtils.append(workbook, sheet, mikuMikuExcel2, object, 2, sheet.getRow(0).getLastCellNum() + 1);
        workbook.write(new FileOutputStream(new File(DIR + "\\mikuMikuTableOfEval.xlsx")));
    }

    private Long id;
    private String name;
    private Integer sex;
    private Date birthday;

    private static ExportTest build(Long id, String name, Integer sex) {
        ExportTest test = new ExportTest();
        test.id = id;
        test.name = name;
        test.sex = sex;
        test.birthday = new Date();
        return test;
    }

}
