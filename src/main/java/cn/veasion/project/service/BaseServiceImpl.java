package cn.veasion.project.service;

import cn.veasion.db.FilterException;
import cn.veasion.db.base.Page;
import cn.veasion.db.criteria.CommonQueryCriteria;
import cn.veasion.db.criteria.QueryCriteriaConvert;
import cn.veasion.db.jdbc.EntityDao;
import cn.veasion.db.query.AbstractQuery;
import cn.veasion.db.query.EntityQuery;
import cn.veasion.db.query.OrderParam;
import cn.veasion.db.query.SubQuery;
import cn.veasion.db.update.AbstractJoinUpdate;
import cn.veasion.db.update.AbstractUpdate;
import cn.veasion.db.update.BatchEntityInsert;
import cn.veasion.db.update.Delete;
import cn.veasion.db.update.EntityInsert;
import cn.veasion.project.model.ICreateUpdate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * BaseServiceImpl
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public abstract class BaseServiceImpl<VO, PO, ID> extends InitEntity implements BaseService<VO, PO, ID> {

    protected Class<VO> voClass;

    protected abstract EntityDao<PO, ID> getEntityDao();

    @Override
    public VO queryById(ID id) {
        return queryById(id, getVoClass());
    }

    @Override
    public List<VO> list(CommonQueryCriteria queryCriteria, Consumer<EntityQuery> consumer) {
        return queryList(queryCriteria, getVoClass(), consumer);
    }

    @Override
    public <E> List<E> queryList(CommonQueryCriteria queryCriteria, Class<E> clazz, Consumer<EntityQuery> consumer) {
        QueryCriteriaConvert convert = new QueryCriteriaConvert(queryCriteria, getEntityClass());
        handleQueryCriteria(convert, queryCriteria);
        EntityQuery entityQuery = convert.getEntityQuery();
        if (consumer != null) {
            consumer.accept(entityQuery);
        }
        List<E> list = queryList(entityQuery, clazz);
        if (list != null && !list.isEmpty()) {
            handleQueryCriteriaResult(convert, queryCriteria, list);
        }
        return list;
    }

    @Override
    public Page<VO> listPage(CommonQueryCriteria queryCriteria, Consumer<EntityQuery> consumer) {
        return queryPage(queryCriteria, getVoClass(), consumer);
    }

    @Override
    public <E> Page<E> queryPage(CommonQueryCriteria queryCriteria, Class<E> clazz, Consumer<EntityQuery> consumer) {
        QueryCriteriaConvert convert = new QueryCriteriaConvert(queryCriteria, getEntityClass());
        handleQueryCriteria(convert, queryCriteria);
        EntityQuery entityQuery = convert.getEntityQuery();
        if (entityQuery.getPageParam() == null) {
            entityQuery.page(1, 10);
        }
        if (consumer != null) {
            consumer.accept(entityQuery);
        }
        Page<E> page = queryPage(entityQuery, clazz);
        List<E> list = page.getList();
        if (list != null && !list.isEmpty()) {
            handleQueryCriteriaResult(convert, queryCriteria, list);
        }
        return page;
    }

    protected void handleQueryCriteria(QueryCriteriaConvert convert, CommonQueryCriteria queryCriteria) {
        EntityQuery entityQuery = convert.getEntityQuery().selectAll();
        if (queryCriteria.getPage() != null && queryCriteria.getSize() != null) {
            entityQuery.page(queryCriteria.getPage(), queryCriteria.getSize());
        }
        if (queryCriteria.getOrders() != null) {
            for (OrderParam order : queryCriteria.getOrders()) {
                String field = order.getField();
                if (!QueryCriteriaConvert.FIELD_PATTERN.matcher(field).matches() || field.length() > 30) {
                    throw new FilterException("非法字段：" + field);
                }
                entityQuery.order(order);
            }
        }
        handleQuery(entityQuery);
        if (entityQuery.getOrders() == null && ICreateUpdate.class.isAssignableFrom(getEntityClass())) {
            entityQuery.desc("createTime");
        }
    }

    protected void handleQueryCriteriaResult(QueryCriteriaConvert convert, CommonQueryCriteria queryCriteria, List<?> list) {
        convert.handleResultLoadRelation(getEntityDao(), list);
    }

    @Override
    public ID add(EntityInsert entityInsert) {
        Object entity = entityInsert.getEntity();
        initEntity(entity);
        return getEntityDao().add(entityInsert);
    }

    @Override
    public ID[] batchAdd(BatchEntityInsert batchEntityInsert) {
        List<?> entityList = batchEntityInsert.getEntityList();
        if (entityList != null) {
            for (Object entity : entityList) {
                initEntity(entity);
            }
        }
        return getEntityDao().batchAdd(batchEntityInsert);
    }

    @Override
    public <E> E queryForType(AbstractQuery<?> abstractQuery, Class<E> aClass) {
        if (!(abstractQuery instanceof SubQuery)) {
            handleQuery(abstractQuery);
        }
        return getEntityDao().queryForType(abstractQuery, aClass);
    }

    @Override
    public Map<String, Object> queryForMap(AbstractQuery<?> abstractQuery, boolean b) {
        if (!(abstractQuery instanceof SubQuery)) {
            handleQuery(abstractQuery);
        }
        return getEntityDao().queryForMap(abstractQuery, b);
    }

    @Override
    public List<Map<String, Object>> listForMap(AbstractQuery<?> abstractQuery, boolean b) {
        if (!(abstractQuery instanceof SubQuery)) {
            handleQuery(abstractQuery);
        }
        return getEntityDao().listForMap(abstractQuery, b);
    }

    @Override
    public <E> List<E> queryList(AbstractQuery<?> abstractQuery, Class<E> aClass) {
        if (!(abstractQuery instanceof SubQuery)) {
            handleQuery(abstractQuery);
        }
        return getEntityDao().queryList(abstractQuery, aClass);
    }

    @Override
    public <E> Page<E> queryPage(AbstractQuery<?> abstractQuery, Class<E> aClass) {
        if (!(abstractQuery instanceof SubQuery)) {
            handleQuery(abstractQuery);
        }
        return getEntityDao().queryPage(abstractQuery, aClass);
    }

    @Override
    public int update(AbstractUpdate<?> abstractUpdate) {
        if (abstractUpdate instanceof AbstractJoinUpdate<?>) {
            updateInitEntity(((AbstractJoinUpdate<?>) abstractUpdate).getEntity());
        }
        return getEntityDao().update(abstractUpdate);
    }

    protected void handleQuery(AbstractQuery<?> abstractQuery) {
    }

    @Override
    public int delete(Delete delete) {
        return getEntityDao().delete(delete);
    }

    @Override
    public Class<PO> getEntityClass() {
        return getEntityDao().getEntityClass();
    }

    @SuppressWarnings("unchecked")
    protected Class<VO> getVoClass() {
        if (voClass != null) {
            return voClass;
        }
        Type genericSuperclass = this.getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class && BaseServiceImpl.class.isAssignableFrom((Class<?>) rawType)) {
                return this.voClass = (Class<VO>) parameterizedType.getActualTypeArguments()[0];
            }
        }

        throw new RuntimeException("获取VO类型失败，请重写 getVoClass() 方法");
    }

}
