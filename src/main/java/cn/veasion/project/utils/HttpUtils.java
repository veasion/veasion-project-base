package cn.veasion.project.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * HttpUtils
 *
 * @author luozhuowei
 * @date 2021/9/13
 */
public class HttpUtils {

    public static final String CHARSET_DEFAULT = "UTF-8";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM_DATA = "application/x-www-form-urlencoded";
    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final int MAX_CONNECT_TIMEOUT = 8000;
    private static final int MAX_SOCKET_TIMEOUT = 30000;

    static {
        // http
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
        CONNECTION_MANAGER.setMaxTotal(50);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(10);
    }

    public static HttpResponse get(String url, Map<String, Object> params) throws Exception {
        String urlLinks = getUrlLinks(params);
        if (urlLinks != null) {
            if (url.contains("?")) {
                url = url + "&" + urlLinks;
            } else {
                url = url + "?" + urlLinks;
            }
        }
        return request(HttpRequest.build(url, "GET"));
    }

    public static void download(String url, File destFile) throws Exception {
        HttpRequest request = HttpRequest.build(url, "GET");
        request.setResponseHandler(entity -> {
            try {
                FileUtil.mkParentDirs(destFile);
                try (FileOutputStream fs = new FileOutputStream(destFile)) {
                    entity.writeTo(fs);
                }
                return destFile;
            } catch (Exception e) {
                return e;
            }
        });
        HttpResponse response = request(request);
        if (response.getResponse() instanceof Exception) {
            throw (Exception) response.getResponse();
        }
    }

    public static HttpResponse postJson(String url, String bodyJson) throws Exception {
        HttpRequest request = HttpRequest.build(url, "POST").setBody(bodyJson);
        if (request.getHeaders() == null) {
            request.setHeaders(Collections.singletonMap(CONTENT_TYPE, CONTENT_TYPE_JSON));
        } else {
            request.getHeaders().put(CONTENT_TYPE, CONTENT_TYPE_JSON);
        }
        return request(request);
    }

    public static HttpResponse postForm(String url, Map<String, Object> params) throws Exception {
        String urlLinks = getUrlLinks(params);
        HttpRequest request = HttpRequest.build(url, "POST").setBody(urlLinks != null ? urlLinks : "");
        if (request.getHeaders() == null) {
            request.setHeaders(Collections.singletonMap(CONTENT_TYPE, CONTENT_TYPE_FORM_DATA));
        } else {
            request.getHeaders().put(CONTENT_TYPE, CONTENT_TYPE_FORM_DATA);
        }
        return request(request);
    }

    private static String getUrlLinks(Map<String, Object> params) throws UnsupportedEncodingException {
        if (params != null && !params.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (StringUtils.hasText(entry.getKey())) {
                    sb.append(entry.getKey()).append("=");
                    if (entry.getValue() != null) {
                        sb.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                    }
                    sb.append("&");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 通用接口请求
     */
    public static HttpResponse request(HttpRequest request) throws Exception {
        HttpRequestBase requestBase = toRequest(request);
        Map<String, String> headers = request.getHeaders();
        ContentType contentType = null;
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                String value = entry.getValue();
                if (CONTENT_TYPE.equalsIgnoreCase(key) && value != null) {
                    contentType = ContentType.parse(value);
                }
                requestBase.setHeader(key, value);
            }
        }

        // body
        setBodyEntity(requestBase, contentType, request.getBody());

        try {
            HttpClient client = getHttpClient(request, request.getUrl(), requestBase);
            org.apache.http.HttpResponse response;
            long startTime = System.currentTimeMillis();
            response = client.execute(requestBase);
            HttpResponse httpResponse = new HttpResponse();
            httpResponse.setReqTime(System.currentTimeMillis() - startTime);
            httpResponse.setStatus(response.getStatusLine().getStatusCode());
            Header[] allHeaders = response.getAllHeaders();
            if (allHeaders != null && allHeaders.length > 0) {
                httpResponse.setHeaders(new HashMap<>());
                for (Header header : allHeaders) {
                    httpResponse.getHeaders().put(header.getName(), header.getValue());
                }
            }
            if (request.responseHandler != null) {
                httpResponse.setResponse(request.responseHandler.apply(response.getEntity()));
            } else {
                String charset = CHARSET_DEFAULT;
                if (contentType != null && contentType.getCharset() != null) {
                    charset = contentType.getCharset().name();
                }
                httpResponse.setResponse(IOUtils.toString(response.getEntity().getContent(), charset));
            }
            return httpResponse;
        } catch (Exception e) {
            requestBase.abort();
            throw e;
        } finally {
            requestBase.releaseConnection();
        }
    }

    private static void setBodyEntity(HttpRequestBase requestBase, ContentType contentType, Object body) {
        if (body != null && requestBase instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) requestBase;
            if (body instanceof HttpEntity) {
                entityRequest.setEntity((HttpEntity) body);
            } else if (body instanceof String) {
                entityRequest.setEntity(getStringEntity((String) body, contentType));
            } else if (body instanceof byte[]) {
                entityRequest.setEntity(new ByteArrayEntity((byte[]) body, contentType));
            } else if (body instanceof File) {
                entityRequest.setEntity(new FileEntity((File) body, contentType));
            } else if (body instanceof InputStream) {
                entityRequest.setEntity(new InputStreamEntity((InputStream) body, contentType));
            } else if (ContentType.APPLICATION_JSON.equals(contentType)) {
                entityRequest.setEntity(getStringEntity(JSON.toJSONString(body), contentType));
            } else {
                entityRequest.setEntity(getStringEntity(body.toString(), contentType));
            }
        }
    }

    private static StringEntity getStringEntity(String body, ContentType contentType) {
        if (contentType != null && contentType.getCharset() != null) {
            return new StringEntity(body, contentType);
        } else {
            return new StringEntity(body, CHARSET_DEFAULT);
        }
    }

    public static HttpRequestBase toRequest(HttpRequest request) {
        String url = request.getUrl();
        String method = request.getMethod();
        if (!StringUtils.hasText(url)) {
            throw new RuntimeException("url不能为空");
        }
        if (!StringUtils.hasText(method)) {
            method = "GET";
        }
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(url);
            case "POST":
                return new HttpPost(url);
            case "PUT":
                return new HttpPut(url);
            case "PATCH":
                return new HttpPatch(url);
            case "DELETE":
                return new HttpDelete(url);
            default:
                throw new RuntimeException("不支持的请求方式：" + method);
        }
    }

    private static HttpClient getHttpClient(HttpRequest req, String url, HttpRequestBase request) {
        RequestConfig.Builder customReqConf = RequestConfig.custom();
        if (req.getMaxSocketTimeout() != null) {
            customReqConf.setSocketTimeout(req.getMaxSocketTimeout());
        } else {
            customReqConf.setSocketTimeout(MAX_SOCKET_TIMEOUT);
        }
        customReqConf.setConnectTimeout(MAX_CONNECT_TIMEOUT);
        customReqConf.setConnectionRequestTimeout(MAX_CONNECT_TIMEOUT);
        request.setConfig(customReqConf.build());
        if (isHttps(url)) {
            try {
                // ssl https
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new X509TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                    }

                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());
                return HttpClients.custom().setSSLSocketFactory(new SSLConnectionSocketFactory(context)).setConnectionManager(CONNECTION_MANAGER).build();
            } catch (Exception e) {
                e.printStackTrace();
                return HttpClients.createDefault();
            }
        } else {
            return HttpClients.custom().setConnectionManager(CONNECTION_MANAGER).build();
        }
    }

    private static boolean isHttps(String url) {
        return url != null && url.trim().startsWith("https");
    }

    public static String toParamLinks(Map<String, Object> params, boolean encode) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                if (key == null || "".equals(key)) {
                    continue;
                }
                Object value = params.get(key);
                if (value == null || "".equals(value)) {
                    continue;
                }
                if (encode) {
                    sb.append(key).append("=").append(URLEncoder.encode(value.toString(), "UTF-8")).append("&");
                } else {
                    sb.append(key).append("=").append(value).append("&");
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("编码失败", e);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static class HttpRequest implements Serializable {

        private String url;
        private String method;
        private Map<String, String> headers;
        private Object body;
        private Integer maxSocketTimeout;
        private Function<HttpEntity, Object> responseHandler;

        public static HttpRequest build(String url, String method) {
            HttpRequest request = new HttpRequest();
            request.url = url;
            request.method = method;
            return request;
        }

        public String getUrl() {
            return url;
        }

        public HttpRequest setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getMethod() {
            return method;
        }

        public HttpRequest setMethod(String method) {
            this.method = method;
            return this;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public HttpRequest setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public void setContentType(String contentType) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.put(CONTENT_TYPE, contentType);
        }

        public HttpRequest addHeaders(String key, Object value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put(key, value != null ? value.toString() : null);
            return this;
        }

        public Object getBody() {
            return body;
        }

        public HttpRequest setBody(Object body) {
            this.body = body;
            return this;
        }

        public void setMaxSocketTimeout(Integer maxSocketTimeout) {
            this.maxSocketTimeout = maxSocketTimeout;
        }

        public Integer getMaxSocketTimeout() {
            return maxSocketTimeout;
        }

        public Function<HttpEntity, Object> getResponseHandler() {
            return responseHandler;
        }

        public HttpRequest setResponseHandler(Function<HttpEntity, Object> responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }
    }

    public static class HttpResponse implements Serializable {

        private int status;
        private long reqTime;
        private Object response;
        private Map<String, String> headers;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public long getReqTime() {
            return reqTime;
        }

        public void setReqTime(long reqTime) {
            this.reqTime = reqTime;
        }

        public Object getResponse() {
            return response;
        }

        public String getResponseToString() {
            if (response == null) {
                return null;
            }
            return response instanceof String ? (String) response : String.valueOf(response);
        }

        public JSONObject getResponseToJson() {
            return JSON.parseObject(getResponseToString());
        }

        public void setResponse(Object response) {
            this.response = response;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }
}