package cn.veasion.project.mongo;

import cn.veasion.db.FilterException;
import cn.veasion.db.base.Operator;
import cn.veasion.db.criteria.AutoCriteria;
import cn.veasion.db.criteria.CommonQueryCriteria;
import cn.veasion.db.criteria.QueryCriteria;
import cn.veasion.db.utils.FieldUtils;
import cn.veasion.project.utils.DateUtils;
import cn.veasion.project.utils.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MQueryCriteriaConvert
 *
 * @author luozhuowei
 * @date 2022/2/6
 */
public class MQueryCriteriaConvert {

    public static final Pattern FIELD_PATTERN = Pattern.compile("[_0-9a-zA-Z.]+");

    @SuppressWarnings("unchecked")
    public static Map<String, Criteria> handleFilters(Query query, CommonQueryCriteria criteria) {
        Objects.requireNonNull(query, "query is null");
        Map<String, Criteria> map = new HashMap<>();
        if (criteria == null) {
            return map;
        }
        Map<String, Field> fields = FieldUtils.fields(criteria.getClass());
        for (Field field : fields.values()) {
            QueryCriteria queryCriteria = field.getAnnotation(QueryCriteria.class);
            AutoCriteria autoCriteria = field.getAnnotation(AutoCriteria.class);
            if (queryCriteria == null && autoCriteria == null) {
                continue;
            }
            Object value = FieldUtils.getValue(criteria, field.getName(), true);
            if (value == null) {
                continue;
            }
            if (queryCriteria != null) {
                handleQueryCriteria(map, query, field, queryCriteria, value);
            } else if (value instanceof Map) {
                handleAutoCriteria(map, query, autoCriteria, (Map<String, Object>) value);
            } else {
                handleAutoCriteria(map, query, autoCriteria, Collections.singletonMap(field.getName(), value));
            }
        }
        return map;
    }

    public static Criteria handleFilters(Criteria criteria, Map<String, Object> filters) {
        return handleFilters(criteria, filters, true);
    }

    public static Criteria handleFilters(Criteria criteria, Map<String, Object> filters, boolean skipEmpty, String... keys) {
        Set<String> filterKeys = keys != null && keys.length > 0 ? new HashSet<>(Arrays.asList(keys)) : null;
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (skipEmpty && isEmpty(value)) {
                continue;
            }
            boolean xdEQ = key.startsWith("-");
            if (xdEQ) {
                key = key.substring(1);
            }
            if (!FIELD_PATTERN.matcher(key).matches() || key.length() > 30) {
                throw new FilterException("非法字段：" + key);
            }
            KVOperator kvOperator = buildKVOperator(key, value, xdEQ);
            if (filterKeys != null && !filterKeys.contains(kvOperator.field)) {
                continue;
            }
            criteria = addFilter(criteria, kvOperator);
        }
        return criteria;
    }

    public static ProjectionOperation project(Class<?> clazz, String objectField) {
        return project(clazz, objectField, null);
    }

    public static ProjectionOperation project(Class<?> clazz, String objectField, Map<String, String> asMap) {
        ProjectionOperation operation = Aggregation.project();
        Map<String, Field> fieldMap = FieldUtils.fields(clazz);
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            if (entry.getValue().getAnnotation(Transient.class) != null) {
                continue;
            }
            String name = entry.getKey();
            if ("id".equals(name) || entry.getValue().getAnnotation(Id.class) != null) {
                name = "_id";
            }
            String asName = name;
            if (asMap != null && asMap.containsKey(name)) {
                asName = asMap.get(name);
            }
            if (StringUtils.isNotEmpty(objectField)) {
                operation = operation.and(objectField + "." + name).as(asName);
            } else {
                operation = operation.and(name).as(asName);
            }
        }
        return operation;
    }

    private static void handleAutoCriteria(Map<String, Criteria> map, Query query, AutoCriteria annotation, Map<String, Object> filters) {
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (annotation.skipEmpty() && isEmpty(value)) {
                continue;
            }
            boolean xdEQ = key.startsWith("-");
            if (xdEQ) {
                key = key.substring(1);
            }
            if (!FIELD_PATTERN.matcher(key).matches() || key.length() > 30) {
                throw new FilterException("非法字段：" + key);
            }
            addFilter(map, query, buildKVOperator(key, value, xdEQ));
        }
    }

    private static KVOperator buildKVOperator(String key, Object value, boolean xdEQ) {
        Operator operator = Operator.EQ;
        if (value instanceof Collection || value instanceof Object[]) {
            if (!xdEQ && key.startsWith("neq_")) {
                key = key.substring(4);
                operator = Operator.NOT_IN;
            } else {
                operator = Operator.IN;
            }
        } else if (!xdEQ) {
            if (key.startsWith("neq_")) {
                key = key.substring(4);
                operator = Operator.NEQ;
            } else if (key.startsWith("gt_")) {
                key = key.substring(3);
                operator = Operator.GT;
            } else if (key.startsWith("gte_")) {
                key = key.substring(4);
                operator = Operator.GTE;
            } else if (key.startsWith("start_")) {
                key = key.substring(6);
                operator = Operator.GTE;
            } else if (key.startsWith("lt_")) {
                key = key.substring(3);
                operator = Operator.LT;
            } else if (key.startsWith("lte_") || key.startsWith("end_")) {
                key = key.substring(4);
                operator = Operator.LTE;
            } else if (value instanceof String &&
                    (String.valueOf(value).startsWith("%") || String.valueOf(value).endsWith("%"))) {
                operator = Operator.LIKE;
            }
        }
        if (value instanceof String) {
            String v = (String) value;
            if (v.length() == 19 && v.contains(":")) {
                try {
                    value = DateUtils.parse(value.toString(), "yyyy-MM-dd HH:mm:ss");
                } catch (Exception ignored) {
                }
            } else if (v.length() == 10 && v.contains("-")) {
                try {
                    value = DateUtils.parse(value.toString(), "yyyy-MM-dd");
                } catch (Exception ignored) {
                }
            }
        }
        return new KVOperator(key, operator, value);
    }

    private static void handleQueryCriteria(Map<String, Criteria> map, Query query, Field field, QueryCriteria annotation, Object value) {
        if (annotation.skipEmpty() && isEmpty(value)) {
            return;
        }
        String fieldName = "".equals(annotation.field()) ? field.getName() : annotation.field();
        Operator operator = annotation.value();
        String[] orFields = annotation.orFields();
        if (orFields.length > 0) {
            query.addCriteria(new Criteria().orOperator(Arrays.stream(orFields).map(key -> appendCriteria(Criteria.where(key), operator, value)).collect(Collectors.toList())));
        } else {
            addFilter(map, query, new KVOperator(fieldName, operator, value));
        }
    }

    private static void addFilter(Map<String, Criteria> map, Query query, KVOperator kvOperator) {
        Criteria criteria = map.get(kvOperator.field);
        if (criteria != null) {
            appendCriteria(criteria, kvOperator.operator, kvOperator.value);
        } else {
            criteria = Criteria.where(kvOperator.field);
            appendCriteria(criteria, kvOperator.operator, kvOperator.value);
            query.addCriteria(criteria);
            map.put(kvOperator.field, criteria);
        }
    }

    private static Criteria addFilter(Criteria criteria, KVOperator kvOperator) {
        String field = "id".equals(kvOperator.field) ? "_id" : kvOperator.field;
        if (criteria == null) {
            criteria = new Criteria(field);
        } else {
            criteria = criteria.and(field);
        }
        return appendCriteria(criteria, kvOperator.operator, kvOperator.value);
    }

    public static Criteria appendCriteria(Criteria criteria, Operator operator, Object value) {
        if (Operator.EQ.equals(operator)) {
            return criteria.is(value);
        } else if (Operator.NEQ.equals(operator)) {
            return criteria.ne(value);
        } else if (Operator.GT.equals(operator)) {
            return criteria.gt(value);
        } else if (Operator.GTE.equals(operator)) {
            return criteria.gte(value);
        } else if (Operator.LT.equals(operator)) {
            return criteria.lt(value);
        } else if (Operator.LTE.equals(operator)) {
            return criteria.lte(value);
        } else if (Operator.IN.equals(operator)) {
            if (value instanceof Collection) {
                return criteria.in((Collection<?>) value);
            } else if (value instanceof Object[]) {
                return criteria.in((Object[]) value);
            } else {
                throw new FilterException("Operator.IN 类型必须是集合或者数组");
            }
        } else if (Operator.NOT_IN.equals(operator)) {
            if (value instanceof Collection) {
                return criteria.nin((Collection<?>) value);
            } else if (value instanceof Object[]) {
                return criteria.nin((Object[]) value);
            } else {
                throw new FilterException("Operator.IN 类型必须是集合或者数组");
            }
        } else if (Operator.LIKE.equals(operator)) {
            String str = String.valueOf(value);
            if (value instanceof String) {
                if (str.startsWith("%") && str.endsWith("%")) {
                    str = str.substring(1, str.length() - 1);
                } else if (str.startsWith("%")) {
                    str = "^" + str.substring(1);
                } else if (str.endsWith("%")) {
                    str = str.substring(0, str.length() - 1) + "$";
                }
            }
            return criteria.regex(str);
        } else if (Operator.BETWEEN.equals(operator)) {
            if (value instanceof Collection) {
                Iterator<?> iterator = ((Collection<?>) value).iterator();
                return criteria.gte(iterator.next()).lte(iterator.next());
            } else if (value instanceof Object[]) {
                Object[] objects = (Object[]) value;
                return criteria.gte(objects[0]).lte(objects[1]);
            } else {
                throw new FilterException("Operator.BETWEEN 类型必须是集合或者数组");
            }
        } else if (Operator.NULL.equals(operator) && !Boolean.FALSE.equals(value)) {
            // criteria.exists(false);
            return criteria.is(null);
        } else if (Operator.NOT_NULL.equals(operator) && !Boolean.FALSE.equals(value)) {
            // criteria.exists(true);
            return criteria.ne(null);
        } else {
            throw new FilterException("不支持 Operator." + operator.name());
        }
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return "".equals(value);
        } else if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return false;
    }

    static class KVOperator {
        public String field;
        public Operator operator;
        public Object value;

        public KVOperator(String field, Operator operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
    }

}
