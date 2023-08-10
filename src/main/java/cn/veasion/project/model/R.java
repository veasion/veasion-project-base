package cn.veasion.project.model;

import java.io.Serializable;

/**
 * R
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class R<T> implements Serializable {

    public static final int SUCCESS = 0;
    public static final int ERROR = -1;

    private T data;
    private int code;
    private String message;

    public R() {
    }

    public R(ErrCodeType errCodeType) {
        this.code = errCodeType.getCode();
        this.message = errCodeType.getMessage();
    }

    public R(T data) {
        this(SUCCESS, "ok", data);
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static R<Object> ok() {
        return new R<>((Object) null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(data);
    }

    public static <T> R<T> error(String message) {
        return new R<>(ERROR, message, null);
    }

    public int getCode() {
        return code;
    }

    public R<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public R<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    public T getData() {
        return data;
    }

    public R<T> setData(T data) {
        this.data = data;
        return this;
    }

}
