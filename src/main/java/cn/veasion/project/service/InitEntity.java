package cn.veasion.project.service;

import cn.veasion.project.model.IBaseId;
import cn.veasion.project.model.ICompanyId;
import cn.veasion.project.model.ICreateUpdate;
import cn.veasion.project.model.ILogicDelete;
import cn.veasion.project.session.SessionHelper;
import cn.veasion.project.utils.IdGenUtils;

import java.util.Date;

/**
 * InitEntity
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
public class InitEntity {

    @SuppressWarnings("unchecked")
    protected void initEntity(Object entity) {
        if (entity instanceof IBaseId) {
            IBaseId<Object> baseId = (IBaseId<Object>) entity;
            if (baseId.getId() == null && !baseId.autoIncrement()) {
                baseId.setId(IdGenUtils.genId());
            }
        }
        if (entity instanceof ILogicDelete) {
            if (((ILogicDelete) entity).getIsDeleted() == null) {
                ((ILogicDelete) entity).setIsDeleted(0);
            }
        }
        if (entity instanceof ICompanyId) {
            if (((ICompanyId) entity).getCompanyId() == null) {
                ((ICompanyId) entity).setCompanyId(SessionHelper.getCompanyId());
            }
        }
        if (entity instanceof ICreateUpdate) {
            ICreateUpdate createUpdate = (ICreateUpdate) entity;
            if (createUpdate.getCreateTime() == null) {
                createUpdate.setCreateTime(new Date());
            }
            if (createUpdate.getCreateUser() == null) {
                createUpdate.setCreateUser(SessionHelper.getUserName());
            }
        }
        updateInitEntity(entity);
    }

    protected void updateInitEntity(Object entity) {
        if (entity instanceof ICreateUpdate) {
            ICreateUpdate createUpdate = (ICreateUpdate) entity;
            if (createUpdate.getUpdateTime() == null) {
                createUpdate.setUpdateTime(new Date());
            }
            if (createUpdate.getUpdateUser() == null) {
                createUpdate.setUpdateUser(SessionHelper.getUserName());
            }
        }
    }

}
