package cn.veasion.project.session;

import cn.veasion.db.utils.ServiceLoaderUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * SessionHelper
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
public class SessionHelper {

    private static ISessionProvider sessionProvider;
    private static final ThreadLocal<ISessionUser> sessionUserHolder = new ThreadLocal<>();

    static {
        sessionProvider = ServiceLoaderUtils.loadOne(ISessionProvider.class);
    }

    public static void clear() {
        sessionUserHolder.remove();
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

    @SuppressWarnings("unchecked")
    public static <T extends ISessionUser> T getUser() {
        T sessionUser = (T) sessionUserHolder.get();
        if (sessionUser == null && sessionProvider != null) {
            sessionUser = (T) sessionProvider.getSessionUser();
        }
        return sessionUser;
    }

    public static String getUserId() {
        ISessionUser sessionUser = getUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getUserId();
    }

    public static String getUserName() {
        ISessionUser sessionUser = getUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getUserName();
    }

    public static Long getCompanyId() {
        ISessionUser sessionUser = getUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getCompanyId();
    }

    public static List<Long> getAuthCompanyIds() {
        ISessionUser sessionUser = getUser();
        if (sessionUser == null) {
            return null;
        }
        List<Long> authCompanyIds = sessionUser.getAuthCompanyIds();
        if (authCompanyIds == null && sessionUser.getCompanyId() != null) {
            return Collections.singletonList(sessionUser.getCompanyId());
        } else {
            return authCompanyIds;
        }
    }

}
