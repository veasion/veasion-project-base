package cn.veasion.project.service;

import cn.veasion.db.base.Page;
import cn.veasion.db.jdbc.EntityDao;
import cn.veasion.db.query.AbstractQuery;
import cn.veasion.db.query.Query;
import cn.veasion.db.query.SubQuery;
import cn.veasion.db.update.Delete;
import cn.veasion.db.utils.FieldUtils;
import cn.veasion.project.model.QueryCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * BaseService
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public interface BaseService<VO, PO, ID> extends EntityDao<PO, ID> {

    VO queryById(ID id);

    default <E> E queryById(ID id, Class<E> clazz) {
        return queryForType(new Query().eq(getIdField(), id), clazz);
    }

    List<VO> list(QueryCriteria queryCriteria);

    Page<VO> listPage(QueryCriteria queryCriteria);

    default int deleteById(ID id) {
        return deleteByIds(Collections.singletonList(id));
    }

    default int deleteByIds(List<ID> ids) {
        return delete(new Delete().in(getIdField(), ids));
    }

    @SuppressWarnings("unchecked")
    default ID saveOrUpdate(PO entity) {
        Object id = FieldUtils.getValue(entity, getIdField(), true);
        if (id == null || "".equals(id)) {
            return add(entity);
        } else {
            updateById(entity);
        }
        return (ID) id;
    }

    default int queryCount(AbstractQuery<?> query) {
        Integer count = queryForType(new SubQuery(query, "t").selectExpression("count(1)", "count"), Integer.class);
        return count != null ? count : 0;
    }

    default <K, E, V> Map<K, V> groupQuery(AbstractQuery<?> query, Class<E> resultClass, Function<? super E, K> keyMapper, Function<? super E, V> valueMapper) {
        List<E> list = queryList(query, resultClass);
        if (list == null || list.isEmpty()) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.toMap(keyMapper, valueMapper, (a, b) -> a));
    }

    default <K, E> Map<K, List<E>> groupListQuery(AbstractQuery<?> query, Class<E> resultClass, Function<? super E, K> keyMapper) {
        List<E> list = queryList(query, resultClass);
        if (list == null || list.isEmpty()) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.groupingBy(keyMapper));
    }

    default <K, E, V> Map<K, List<V>> groupListQuery(AbstractQuery<?> query, Class<E> resultClass, Function<? super E, K> keyMapper, Function<? super E, V> valueMapper) {
        List<E> list = queryList(query, resultClass);
        if (list == null || list.isEmpty()) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.groupingBy(keyMapper, Collector.of(ArrayList::new, (l, t) -> l.add(valueMapper.apply(t)), (a, b) -> {
            a.addAll(b);
            return a;
        })));
    }

}
