package cn.veasion.project.interceptor;

import cn.veasion.db.AbstractFilter;
import cn.veasion.db.base.Filter;
import cn.veasion.db.interceptor.AbstractInterceptor;
import cn.veasion.db.update.Delete;
import cn.veasion.project.model.ICompanyId;
import cn.veasion.project.session.SessionHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * SAAS数据隔离拦截器
 *
 * @author luozhuowei
 * @date 2022/1/15
 */
public class CompanyInterceptor extends AbstractInterceptor {

    private static final String COMPANY_ID = "companyId";

    private static ThreadLocal<Boolean> skipThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> companyThreadLocal = new ThreadLocal<>();

    public CompanyInterceptor() {
        super(true, true, true, true, false);
    }

    public static boolean isSkip() {
        return Boolean.TRUE.equals(skipThreadLocal.get());
    }

    public static void skip(boolean skip) {
        skipThreadLocal.set(skip);
    }

    public static void clearSkip() {
        skipThreadLocal.remove();
    }

    public static <R> R withSkip(Supplier<R> supplier) {
        Boolean flag = skipThreadLocal.get();
        try {
            skipThreadLocal.set(true);
            return supplier.get();
        } finally {
            if (flag != null) {
                skipThreadLocal.set(flag);
            } else {
                skipThreadLocal.remove();
            }
        }
    }

    public static <R> R withCompanyId(Long companyId, Supplier<R> supplier) {
        Long oldCompanyId = companyThreadLocal.get();
        try {
            companyThreadLocal.set(companyId);
            return supplier.get();
        } finally {
            if (oldCompanyId != null) {
                companyThreadLocal.set(oldCompanyId);
            } else {
                companyThreadLocal.remove();
            }
        }
    }

    public static Long setCompanyId(Long companyId) {
        Long oldCompanyId = companyThreadLocal.get();
        companyThreadLocal.set(companyId);
        return oldCompanyId;
    }

    public static void clearCompanyId() {
        companyThreadLocal.remove();
    }

    @Override
    protected boolean skip() {
        return Boolean.TRUE.equals(skipThreadLocal.get());
    }

    @Override
    protected boolean containSkipClass(Class<?> clazz) {
        if (!ICompanyId.class.isAssignableFrom(clazz)) {
            return true;
        }
        return super.containSkipClass(clazz);
    }

    @Override
    protected void handleDelete(Delete delete) {
        if (!delete.hasFilter(COMPANY_ID)) {
            delete.in(COMPANY_ID, companyIds());
        }
    }

    @Override
    protected void handleOnFilter(Object joinParam, Supplier<List<Filter>> onFilters, Consumer<Filter> onMethod, String tableAs) {
        onMethod.accept(Filter.AND);
        onMethod.accept(Filter.in(COMPANY_ID, companyIds()).fieldAs(tableAs));
    }

    @Override
    protected void handleFilter(AbstractFilter<?> abstractFilter) {
        if (!abstractFilter.hasFilter(COMPANY_ID)) {
            abstractFilter.in(COMPANY_ID, companyIds());
        }
    }

    @Override
    protected void handleInsert(Class<?> entityClass, List<?> entityList, List<Map<String, Object>> fieldValueMapList) {
    }

    private List<Long> companyIds() {
        Long companyId = companyThreadLocal.get();
        if (companyId != null) {
            return Arrays.asList(SessionHelper.DEFAULT_COMPANY_ID, companyId);
        } else {
            return SessionHelper.getAuthCompanyIds();
        }
    }

    public static Long getThreadLocalCompanyId() {
        return companyThreadLocal.get();
    }

}
