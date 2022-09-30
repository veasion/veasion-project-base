package cn.veasion.project.model;

import java.util.Date;

/**
 * ICreateUpdate
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public interface ICreateUpdate {

    Date getCreateTime();

    void setCreateTime(Date createTime);

    default String getCreateUser() {
        return null;
    }

    default void setCreateUser(String createUser) {
    }

    default Date getUpdateTime() {
        return null;
    }

    default void setUpdateTime(Date updateTime) {
    }

    default String getUpdateUser() {
        return null;
    }

    default void setUpdateUser(String updateUser) {
    }

}
