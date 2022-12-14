package cn.veasion.project.mongo;

import cn.veasion.db.base.Page;
import cn.veasion.project.model.QueryCriteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.function.Consumer;

/**
 * MongoBaseService
 *
 * @author luozhuowei
 * @date 2022/2/5
 */
public interface MongoBaseService<M, Q extends QueryCriteria> {

    M insert(M obj);

    M saveOrUpdate(M obj);

    M getById(Object id);

    <T> List<T> list(Query query, Class<T> clazz);

    <T> List<T> list(Q criteria, Consumer<Query> consumer, Class<T> clazz);

    List<M> list(Q criteria);

    <T> Page<T> listPage(Q criteria, Consumer<Query> consumer, Class<T> clazz);

    Page<M> listPage(Q criteria);

    int deleteById(Object id);

    int delete(Q criteria);

    long count(Q criteria, Consumer<Query> consumer);

}
