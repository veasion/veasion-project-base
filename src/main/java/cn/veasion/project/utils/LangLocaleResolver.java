package cn.veasion.project.utils;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * LangLocaleResolver
 *
 * @author luozhuowei
 * @date 2023/11/22
 */
public class LangLocaleResolver implements LocaleResolver {

    private Locale defaultLocale = MessageUtils.DEFAULT_LOCALE;

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String localeValue = MessageUtils.getLocaleValue(request);
        if (localeValue != null && !localeValue.isEmpty()) {
            try {
                return StringUtils.parseLocale(localeValue);
            } catch (Exception ignored) {
            }
        }
        return defaultLocale;
    }

    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
    }

}
