package cn.veasion.project.model;

/**
 * IBaseId
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public interface IBaseId<ID> extends cn.veasion.db.base.IBaseId<ID> {

    default boolean autoIncrement() {
        return false;
    }

}
