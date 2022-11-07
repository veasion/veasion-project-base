package cn.veasion.project.session;

import java.util.List;

/**
 * SimpleSessionUser
 *
 * @author luozhuowei
 * @date 2022/11/7
 */
public class SimpleSessionUser implements ISessionUser {

    private String userId;
    private String userName;
    private Long companyId;
    private List<Long> authCompanyIds;
    private Object originalUser;

    public SimpleSessionUser(String userId, String userName, Long companyId, List<Long> authCompanyIds, Object originalUser) {
        this.userId = userId;
        this.userName = userName;
        this.companyId = companyId;
        this.authCompanyIds = authCompanyIds;
        this.originalUser = originalUser;
    }

    public SimpleSessionUser(ISessionUser sessionUser) {
        if (sessionUser == null) {
            return;
        }
        this.userId = sessionUser.getUserId();
        this.userName = sessionUser.getUserName();
        this.companyId = sessionUser.getCompanyId();
        this.authCompanyIds = sessionUser.getAuthCompanyIds();
        this.originalUser = sessionUser.getOriginalUser();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public Long getCompanyId() {
        return companyId;
    }

    @Override
    public List<Long> getAuthCompanyIds() {
        return authCompanyIds;
    }

    @Override
    public Object getOriginalUser() {
        return originalUser;
    }

}
