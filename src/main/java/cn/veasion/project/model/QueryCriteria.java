package cn.veasion.project.model;

import cn.veasion.db.criteria.AutoCriteria;
import cn.veasion.db.query.OrderParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QueryCriteria
 *
 * @author luozhuowei
 * @date 2022/8/12
 */
public class QueryCriteria {

    @AutoCriteria
    private Map<String, Object> filters;

    private Integer page;
    private Integer size;

    private List<OrderParam> orders;

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Integer getPage() {
        return page;
    }

    public QueryCriteria setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public QueryCriteria setSize(Integer size) {
        this.size = size;
        return this;
    }

    public void setOrder(OrderParam orderParam) {
        if (orderParam != null) {
            if (orders == null) {
                orders = new ArrayList<>();
            }
            orders.add(orderParam);
        }
    }

    public List<OrderParam> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderParam> orders) {
        this.orders = orders;
    }

    public void withLike(String key) {
        withLike(key, true, true);
    }

    public void withLike(String key, boolean left, boolean right) {
        Object v;
        if (filters != null && (v = filters.get(key)) != null) {
            String value = v.toString().trim();
            if ("".equals(value)) {
                return;
            }
            if (left && !value.startsWith("%")) {
                value = "%" + value;
            }
            if (right && !value.endsWith("%")) {
                value += "%";
            }
            filters.put(key, value);
        }
    }

    public QueryCriteria addFilter(String key, Object value) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.put(key, value);
        return this;
    }

    public Object remove(String key) {
        if (filters != null) {
            return filters.remove(key);
        }
        return null;
    }

}
