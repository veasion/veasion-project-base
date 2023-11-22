package cn.veasion.project.utils;

import org.apache.commons.io.IOUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * i18n 国际化
 *
 * @author luozhuowei
 * @date 2023/8/1
 */
public class MessageUtils {

    public static Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    public static String message(String code, Object... args) {
        MessageSource messageSource = SpringBeanUtils.getBean(MessageSource.class);
        return messageSource.getMessage(code, args, getLocale());
    }

    public static String message(HttpServletRequest request, String code, Object... args) {
        Locale locale = null;
        String localeValue = getLocaleValue(request);
        if (localeValue != null && !localeValue.isEmpty()) {
            try {
                locale = StringUtils.parseLocale(localeValue);
            } catch (Exception ignored) {
            }
        }
        if (locale == null) {
            locale = MessageUtils.DEFAULT_LOCALE;
        }
        MessageSource messageSource = SpringBeanUtils.getBean(MessageSource.class);
        return messageSource.getMessage(code, args, locale);
    }

    public static String messageWithArgCodes(String code, String... codeArgs) {
        MessageSource messageSource = SpringBeanUtils.getBean(MessageSource.class);
        for (int i = 0; i < codeArgs.length; i++) {
            codeArgs[i] = tryMessage(codeArgs[i], codeArgs[i]);
        }
        return messageSource.getMessage(code, codeArgs, getLocale());
    }

    public static String tryMessage(String code) {
        return tryMessage(code, code);
    }

    public static String tryMessage(String code, String def, Object... args) {
        MessageSource messageSource = SpringBeanUtils.getBean(MessageSource.class);
        return messageSource.getMessage(code, args, def, getLocale());
    }

    public static String file(String filename) throws IOException {
        return file(filename, null);
    }

    public static String file(String filename, Map<String, Object> replace) throws IOException {
        Locale locale = getLocale();
        String language = locale.getLanguage();
        String[] filenames;
        if (filename.contains(".")) {
            int idx = filename.lastIndexOf(".");
            String name = filename.substring(0, idx);
            String hz = filename.substring(idx);
            filenames = new String[]{name + "_" + language + hz, filename + "_" + language, filename};
        } else {
            filenames = new String[]{filename + "_" + language, filename};
        }
        for (String s : filenames) {
            try (InputStream is = MessageUtils.class.getClassLoader().getResourceAsStream("i18n/file/" + s)) {
                if (is == null) {
                    continue;
                }
                String content = IOUtils.toString(is, StandardCharsets.UTF_8);
                if (replace != null) {
                    for (Map.Entry<String, Object> entry : replace.entrySet()) {
                        content = content.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                    }
                }
                return content;
            }
        }
        throw new RuntimeException("文件不存在: i18n/file/" + filename);
    }

    public static String getLocaleValue(HttpServletRequest request) {
        String localeValue = request.getParameter("lang");
        if (localeValue == null || localeValue.isEmpty()) {
            localeValue = request.getHeader("lang");
        }
        if (localeValue == null || localeValue.isEmpty()) {
            localeValue = request.getHeader("Accept-Language");
            if (localeValue != null && !localeValue.isEmpty()) {
                localeValue = localeValue.split(",")[0].trim();
            }
        }
        return localeValue;
    }

    public static Locale getLocale() {
        Locale locale = null;
        LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
        if (localeContext != null) {
            locale = localeContext.getLocale();
        }
        if (locale == null) {
            HttpServletRequest request = RequestHolder.getHttpServletRequest();
            if (request != null) {
                String localeValue = getLocaleValue(request);
                if (localeValue != null && !localeValue.isEmpty()) {
                    try {
                        locale = StringUtils.parseLocale(localeValue);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (locale == null) {
            locale = MessageUtils.DEFAULT_LOCALE;
        }
        return locale;
    }

}
