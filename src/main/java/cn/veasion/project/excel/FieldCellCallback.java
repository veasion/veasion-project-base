package cn.veasion.project.excel;

import org.apache.poi.ss.usermodel.Cell;

/**
 * FieldCellCallback
 *
 * @author luozhuowei
 * @date 2021/8/7
 */
@FunctionalInterface
public interface FieldCellCallback {

    Object callback(String field, Object value, Cell cell);

}
