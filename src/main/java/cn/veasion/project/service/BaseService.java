package cn.veasion.project.service;

import cn.veasion.db.base.Page;
import cn.veasion.db.criteria.CommonQueryCriteria;
import cn.veasion.db.jdbc.EntityDao;
import cn.veasion.db.query.EntityQuery;
import cn.veasion.db.query.Query;
import cn.veasion.db.utils.FieldUtils;

import java.util.List;
import java.util.function.Consumer;

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

    List<VO> list(CommonQueryCriteria queryCriteria, Consumer<EntityQuery> consumer);

    default List<VO> list(CommonQueryCriteria queryCriteria) {
        return list(queryCriteria, null);
    }

    Page<VO> listPage(CommonQueryCriteria queryCriteria, Consumer<EntityQuery> consumer);

    default Page<VO> listPage(CommonQueryCriteria queryCriteria) {
        return listPage(queryCriteria, null);
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

}
