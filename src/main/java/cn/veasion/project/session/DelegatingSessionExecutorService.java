package cn.veasion.project.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.veasion.project.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DelegatingSessionExecutorService
 *
 * @author luozhuowei
 * @date 2022/11/7
 */
public class DelegatingSessionExecutorService implements ExecutorService {

    private final static Logger LOGGER = LoggerFactory.getLogger(DelegatingSessionExecutorService.class);

    private ExecutorService delegate;
    private boolean useSecurityContext;
    private boolean useLocaleContext;

    public DelegatingSessionExecutorService(ExecutorService delegate) {
        this(delegate, false);
    }

    public DelegatingSessionExecutorService(ExecutorService delegate, boolean useSecurityContext) {
        this(delegate, useSecurityContext, false);
    }

    public DelegatingSessionExecutorService(ExecutorService delegate, boolean useSecurityContext, boolean useLocaleContext) {
        this.delegate = Objects.requireNonNull(delegate);
        this.useSecurityContext = useSecurityContext;
        this.useLocaleContext = useLocaleContext;
    }

    public ExecutorService getDelegate() {
        return delegate;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(createTasks(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(createTasks(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(createTasks(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(createTasks(tasks), timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    private <T> Collection<Callable<T>> createTasks(Collection<? extends Callable<T>> tasks) {
        if (tasks == null) {
            return null;
        }
        List<Callable<T>> results = new ArrayList<>(tasks.size());
        for (Callable<T> callable : tasks) {
            results.add(this.wrap(callable));
        }
        return results;
    }

    protected Runnable wrap(Runnable runnable) {
        SecurityContext context;
        SimpleSessionUser sessionUser = SessionHelper.copySessionUser();
        if (useSecurityContext) {
            context = SecurityContextHolder.createEmptyContext();
            SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
            if (originalSecurityContext != null) {
                context.setAuthentication(originalSecurityContext.getAuthentication());
            }
        } else {
            context = null;
        }
        Locale locale;
        if (useLocaleContext) {
            locale = MessageUtils.getLocale();
        } else {
            locale = null;
        }
        return () -> {
            try {
                SessionHelper.setUser(sessionUser);
                if (useSecurityContext) {
                    SecurityContextHolder.setContext(context);
                }
                if (locale != null) {
                    LocaleContextHolder.setLocale(locale);
                }
                runnable.run();
            } catch (Exception e) {
                LOGGER.error("异步执行异常", e);
                throw e;
            } finally {
                SessionHelper.clear();
                if (useSecurityContext) {
                    SecurityContextHolder.clearContext();
                }
                if (locale != null) {
                    LocaleContextHolder.resetLocaleContext();
                }
            }
        };
    }

    protected <T> Callable<T> wrap(Callable<T> callable) {
        SimpleSessionUser sessionUser = SessionHelper.copySessionUser();
        SecurityContext context;
        if (useSecurityContext) {
            context = SecurityContextHolder.createEmptyContext();
            SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
            if (originalSecurityContext != null) {
                context.setAuthentication(originalSecurityContext.getAuthentication());
            }
        } else {
            context = null;
        }
        Locale locale;
        if (useLocaleContext) {
            locale = MessageUtils.getLocale();
        } else {
            locale = null;
        }
        return () -> {
            try {
                SessionHelper.setUser(sessionUser);
                if (useSecurityContext) {
                    SecurityContextHolder.setContext(context);
                }
                if (locale != null) {
                    LocaleContextHolder.setLocale(locale);
                }
                return callable.call();
            } catch (Exception e) {
                LOGGER.error("异步执行异常", e);
                throw e;
            } finally {
                SessionHelper.clear();
                if (useSecurityContext) {
                    SecurityContextHolder.clearContext();
                }
                if (locale != null) {
                    LocaleContextHolder.resetLocaleContext();
                }
            }
        };
    }

}
