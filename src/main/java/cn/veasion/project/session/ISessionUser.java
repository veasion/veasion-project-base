package cn.veasion.project.session;

import java.util.List;

/**
 * ISessionUser
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
public interface ISessionUser {

    String getUserId();

    String getUserName();

    Long getCompanyId();

    List<Long> getAuthCompanyIds();

    Object getOriginalUser();

}
