package cn.veasion.project.model;

import java.util.List;

/**
 * Page
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class Page<T> extends R<List<T>> {

    private Long total;

    public Page() {
    }

    public Page(List<T> data, Long total) {
        super(data);
        this.total = total;
    }

    public static <T> Page<T> ok(cn.veasion.db.base.Page<T> page) {
        return ok(page.getList(), page.getCount());
    }

    public static <T> Page<T> ok(List<T> data, Long total) {
        return new Page<>(data, total);
    }

    public Long getTotal() {
        return total;
    }

    public Page<T> setTotal(Long total) {
        this.total = total;
        return this;
    }
}
