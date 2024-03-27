package cn.veasion.project.utils;

import com.alibaba.fastjson.JSON;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SimpleSseEventBuilder
 *
 * @author luozhuowei
 * @date 2024/3/2
 */
public class SimpleSseEventBuilder implements SseEmitter.SseEventBuilder {

    private String data;

    private SimpleSseEventBuilder() {
    }

    public SseEmitter.SseEventBuilder id(String id) {
        throw new UnsupportedOperationException("id");
    }

    public SseEmitter.SseEventBuilder name(String name) {
        throw new UnsupportedOperationException("name");
    }

    public SseEmitter.SseEventBuilder reconnectTime(long reconnectTimeMillis) {
        throw new UnsupportedOperationException("reconnectTime");
    }

    public SseEmitter.SseEventBuilder comment(String comment) {
        throw new UnsupportedOperationException("comment");
    }

    public SseEmitter.SseEventBuilder data(Object object) {
        return this.data(object, MediaType.APPLICATION_JSON);
    }

    public SseEmitter.SseEventBuilder data(Object object, @Nullable MediaType mediaType) {
        this.data = object instanceof String ? (String) object : JSON.toJSONString(object);
        return this;
    }

    public Set<ResponseBodyEmitter.DataWithMediaType> build() {
        if (this.data != null) {
            Set<ResponseBodyEmitter.DataWithMediaType> set = new LinkedHashSet<>(2);
            set.add(new ResponseBodyEmitter.DataWithMediaType("data: " + this.data, MediaType.TEXT_PLAIN));
            set.add(new ResponseBodyEmitter.DataWithMediaType("\n\n", MediaType.TEXT_PLAIN));
            return set;
        } else {
            return Collections.emptySet();
        }
    }

    public static SseEmitter.SseEventBuilder build(Object data) {
        return new SimpleSseEventBuilder().data(data);
    }

}
