package cn.veasion.project.dao;

import cn.veasion.db.DbException;
import cn.veasion.db.base.Page;
import cn.veasion.db.mybatis.MybatisEntityDao;
import cn.veasion.db.query.AbstractJoinQuery;
import cn.veasion.db.query.AbstractQuery;
import cn.veasion.db.update.AbstractJoinUpdate;
import cn.veasion.db.update.BatchEntityInsert;
import cn.veasion.db.update.Delete;
import cn.veasion.db.update.EntityInsert;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CommonDao
 *
 * @author luozhuowei
 * @date 2022/3/21
 */
public class CommonDao {

    private CommonJdbcEntityDao dao;

    public CommonDao(DataSource dataSource) {
        dao = new CommonJdbcEntityDao();
        dao.setDataSource(Objects.requireNonNull(dataSource));
    }

    public Object add(EntityInsert entityInsert) {
        Object entity = entityInsert.getEntity();
        if (entity == null) {
            throw new DbException("entity 不能为空");
        }
        entityInsert.check(entity.getClass());
        return dao.add(entityInsert);
    }

    public Object[] batchAdd(BatchEntityInsert batchEntityInsert) {
        List<?> entityList = batchEntityInsert.getEntityList();
        if (entityList != null && !entityList.isEmpty()) {
            batchEntityInsert.check(entityList.get(0).getClass());
        } else {
            AbstractQuery<?> query = batchEntityInsert.getInsertSelectQuery();
            if (query instanceof AbstractJoinQuery<?> && query.getEntityClass() != null) {
                batchEntityInsert.check(query.getEntityClass());
            }
        }
        return dao.batchAdd(batchEntityInsert);
    }

    public <E> E queryForType(AbstractJoinQuery<?> query, Class<E> clazz) {
        query.check(query.getEntityClass());
        return dao.queryForType(query, clazz);
    }

    public Map<String, Object> queryForMap(AbstractJoinQuery<?> query, boolean mapUnderscoreToCamelCase) {
        query.check(query.getEntityClass());
        return dao.queryForMap(query, mapUnderscoreToCamelCase);
    }

    public List<Map<String, Object>> listForMap(AbstractJoinQuery<?> query) {
        query.check(query.getEntityClass());
        return listForMap(query, true);
    }

    public List<Map<String, Object>> listForMap(AbstractJoinQuery<?> query, boolean mapUnderscoreToCamelCase) {
        query.check(query.getEntityClass());
        return dao.listForMap(query, mapUnderscoreToCamelCase);
    }

    public <E> List<E> queryList(AbstractJoinQuery<?> query, Class<E> clazz) {
        query.check(query.getEntityClass());
        return dao.queryList(query, clazz);
    }

    public <E> Page<E> queryPage(AbstractJoinQuery<?> query, Class<E> clazz) {
        query.check(query.getEntityClass());
        return dao.queryPage(query, clazz);
    }

    @SuppressWarnings("unchecked")
    public <E> Page<E> queryPage(AbstractJoinQuery<?> query) {
        query.check(query.getEntityClass());
        return (Page<E>) dao.queryPage(query, query.getEntityClass());
    }

    public int update(AbstractJoinUpdate<?> update) {
        update.check(update.getEntityClass());
        return dao.update(update);
    }

    public int delete(Delete delete, Class<?> clazz) {
        delete.check(clazz);
        return dao.delete(delete);
    }

    private static class CommonJdbcEntityDao extends MybatisEntityDao<Object, Object> {
        @Override
        public Object getById(Object _void) {
            throw new DbException("不支持，请通过 queryForType 方式查询");
        }

        @Override
        public int updateById(Object entity) {
            throw new DbException("不支持，请通过 update 方式更新");
        }

        @Override
        public int deleteById(Object _void) {
            throw new DbException("不支持，请通过 delete 方式更新");
        }

        @Override
        public Class<Object> getEntityClass() {
            return null;
        }
    }
}
