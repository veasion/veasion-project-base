package cn.veasion.project.session;

import cn.veasion.db.utils.ServiceLoaderUtils;
import cn.veasion.project.interceptor.CompanyInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * SessionHelper
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
public class SessionHelper {

    public static final Long DEFAULT_COMPANY_ID = -1L;
    private static ISessionProvider sessionProvider;
    private static final ThreadLocal<ISessionUser> sessionUserHolder = new ThreadLocal<>();

    static {
        sessionProvider = ServiceLoaderUtils.loadOne(ISessionProvider.class);
    }

    public static <R> R withUser(ISessionUser user, Supplier<R> supplier) {
        ISessionUser oldUser = sessionUserHolder.get();
        try {
            sessionUserHolder.set(user);
            return supplier.get();
        } finally {
            if (oldUser != null) {
                sessionUserHolder.set(oldUser);
            } else {
                sessionUserHolder.remove();
            }
        }
    }

    public static void setUser(ISessionUser user) {
        sessionUserHolder.set(user);
    }

    public static void clear() {
        sessionUserHolder.remove();
    }

    public static SimpleSessionUser copySessionUser() {
        return new SimpleSessionUser(getSessionUser());
    }

    public static ISessionUser getSessionUser() {
        ISessionUser sessionUser = sessionUserHolder.get();
        if (sessionUser == null && sessionProvider != null) {
            sessionUser = sessionProvider.getSessionUser();
        }
        return sessionUser;
    }

    public static String getUserId() {
        ISessionUser sessionUser = getSessionUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getUserId();
    }

    public static String getUserName() {
        ISessionUser sessionUser = getSessionUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getUserName();
    }

    public static Long getSessionCompanyId() {
        ISessionUser sessionUser = getSessionUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getCompanyId();
    }

    public static Long getCompanyId() {
        Long companyId = getSessionCompanyId();
        if (companyId == null) {
            companyId = CompanyInterceptor.getThreadLocalCompanyId();
        }
        return companyId;
    }

    public static List<Long> getAuthCompanyIds() {
        ISessionUser sessionUser = getSessionUser();
        if (sessionUser == null) {
            return null;
        }
        List<Long> authCompanyIds = sessionUser.getAuthCompanyIds();
        if (authCompanyIds == null && sessionUser.getCompanyId() != null) {
            return Arrays.asList(DEFAULT_COMPANY_ID, sessionUser.getCompanyId());
        } else {
            return authCompanyIds;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOriginalUser() {
        ISessionUser sessionUser = getSessionUser();
        if (sessionUser == null) {
            return null;
        }
        return (T) sessionUser.getOriginalUser();
    }

}
