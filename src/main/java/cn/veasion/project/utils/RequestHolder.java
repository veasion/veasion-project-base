package cn.veasion.project.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * RequestHolder
 *
 * @author luozhuowei
 */
public class RequestHolder {

    public static HttpServletRequest getHttpServletRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }

    public static String getDomain(HttpServletRequest request) {
        if (request == null) {
            request = getHttpServletRequest();
        }
        if (request == null) {
            return null;
        }
        StringBuffer requestURL = request.getRequestURL();
        int start = requestURL.indexOf("//") + 2;
        int end = requestURL.indexOf("/", start);
        String domain = end > -1 ? requestURL.substring(start, end) : requestURL.substring(start);
        return domain.split(":")[0];
    }

    public static String getDomainUrl(HttpServletRequest request) {
        String _url = request.getRequestURL().toString();
        boolean isHttps = _url.startsWith("https") || _url.contains(":443/");
        return (isHttps ? "https://" : "http://") + getDomain(request);
    }

    public static boolean startContextPath(String contextPath) {
        HttpServletRequest httpServletRequest = getHttpServletRequest();
        if (httpServletRequest == null) {
            return false;
        }
        return httpServletRequest.getContextPath().startsWith(contextPath);
    }

}
