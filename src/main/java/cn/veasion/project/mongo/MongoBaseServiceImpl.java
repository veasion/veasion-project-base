package cn.veasion.project.mongo;

import cn.hutool.core.collection.CollectionUtil;
import cn.veasion.db.base.Page;
import cn.veasion.db.criteria.CommonQueryCriteria;
import cn.veasion.db.query.OrderParam;
import cn.veasion.project.service.InitEntity;
import com.mongodb.client.result.DeleteResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * MongoBaseServiceImpl
 *
 * @author luozhuowei
 * @date 2022/2/5
 */
public abstract class MongoBaseServiceImpl<M, Q extends CommonQueryCriteria> extends InitEntity implements MongoBaseService<M, Q> {

    @Resource
    protected MongoTemplate mongoTemplate;
    private Class<M> entityClass;

    @Override
    public M insert(M obj) {
        initEntity(obj);
        return mongoTemplate.insert(obj);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<M> insertAll(List<M> list) {
        for (M obj : list) {
            initEntity(obj);
        }
        Collection<M> newList = mongoTemplate.insert(list, getEntityClass());
        if (newList instanceof List) {
            return (List) newList;
        } else {
            return new ArrayList<>(newList);
        }
    }

    @Override
    public M saveOrUpdate(M obj) {
        initEntity(obj);
        updateInitEntity(obj);
        return mongoTemplate.save(obj);
    }

    @Override
    public M getById(Object id) {
        return mongoTemplate.findById(id, getEntityClass());
    }

    @Override
    public <T> T findOne(Query query, Class<T> clazz) {
        return mongoTemplate.findOne(query, clazz, mongoTemplate.getCollectionName(getEntityClass()));
    }

    @Override
    public <T> T findOne(Q criteria, Consumer<Query> consumer, Class<T> clazz) {
        Query query = buildQuery(criteria, false);
        if (consumer != null) {
            consumer.accept(query);
        }
        return mongoTemplate.findOne(query, clazz, mongoTemplate.getCollectionName(getEntityClass()));
    }

    @Override
    public <T> List<T> list(Query query, Class<T> clazz) {
        return mongoTemplate.find(query, clazz, mongoTemplate.getCollectionName(getEntityClass()));
    }

    @Override
    public <T> Page<T> listPage(Q criteria, Consumer<Query> consumer, Class<T> clazz) {
        Query query = buildQuery(criteria, false);
        if (consumer != null) {
            consumer.accept(query);
        }
        long count = mongoTemplate.count(query, getEntityClass());
        List<T> list;
        if (count > 0) {
            query.skip((long) criteria.getSize() * (criteria.getPage() - 1)).limit(criteria.getSize());
            list = mongoTemplate.find(query, clazz, mongoTemplate.getCollectionName(getEntityClass()));
        } else {
            list = new ArrayList<>();
        }
        return new Page<>(criteria.getPage(), criteria.getSize(), (int) count, list);
    }

    @Override
    public Page<M> listPage(Q criteria) {
        return listPage(criteria, null, getEntityClass());
    }

    @Override
    public <T> List<T> list(Q criteria, Consumer<Query> consumer, Class<T> clazz) {
        if (criteria.getPage() == null) {
            criteria.setPage(1);
        }
        if (criteria.getSize() == null) {
            criteria.setSize(10000);
        }
        Query query = buildQuery(criteria, true);
        if (consumer != null) {
            consumer.accept(query);
        }
        return mongoTemplate.find(query, clazz, mongoTemplate.getCollectionName(getEntityClass()));
    }

    @Override
    public List<M> list(Q criteria) {
        return list(criteria, null, getEntityClass());
    }

    @Override
    public int deleteById(Object id) {
        if (id == null) {
            return 0;
        }
        Query query = new Query(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query, getEntityClass());
        return (int) result.getDeletedCount();
    }

    @Override
    public int delete(Q criteria) {
        Query query = buildQuery(criteria, false);
        DeleteResult result = mongoTemplate.remove(query, getEntityClass());
        return (int) result.getDeletedCount();
    }

    @Override
    public long count(Q criteria, Consumer<Query> consumer) {
        Query query = buildQuery(criteria, false);
        if (consumer != null) {
            consumer.accept(query);
        }
        return mongoTemplate.count(query, getEntityClass());
    }

    protected Query buildQuery(Q criteria, boolean page) {
        return buildQuery(null, criteria, page);
    }

    protected Query buildQuery(Query query, Q criteria, boolean page) {
        if (query == null) {
            query = new Query();
        }
        if (page && criteria.getPage() != null && criteria.getSize() != null) {
            query.skip(criteria.getSize() * (criteria.getPage() - 1)).limit(criteria.getSize());
        }
        MQueryCriteriaConvert.handleFilters(query, criteria);
        if (CollectionUtil.isNotEmpty(criteria.getOrders())) {
            for (OrderParam order : criteria.getOrders()) {
                if (order.isDesc()) {
                    query.with(Sort.by(order.getField()).descending());
                } else {
                    query.with(Sort.by(order.getField()).ascending());
                }
            }
        }
        return query;
    }

    protected String getCollectionName() {
        Document annotation = getEntityClass().getAnnotation(Document.class);
        if (annotation != null) {
            return annotation.value();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Class<M> getEntityClass() {
        if (entityClass != null) {
            return entityClass;
        }
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class && MongoBaseService.class.isAssignableFrom((Class<?>) rawType)) {
                return (entityClass = (Class<M>) parameterizedType.getActualTypeArguments()[0]);
            }
        }
        throw new RuntimeException("获取实体类型失败，请重写 getEntityClass() 方法");
    }

}
