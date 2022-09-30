package cn.veasion.project.model;

import java.io.Serializable;

/**
 * IBaseEntity
 *
 * @author luozhuowei
 * @date 2022/6/29
 */
public interface IBasePO<ID> extends IBaseId<ID>, ICreateUpdate, ILogicDelete, Serializable {

}
