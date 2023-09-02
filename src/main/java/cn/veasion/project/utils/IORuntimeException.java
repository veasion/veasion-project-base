package cn.veasion.project.utils;

/**
 * IORuntimeException
 *
 * @author luozhuowei
 * @date 2023/8/30
 */
public class IORuntimeException extends RuntimeException {

    public IORuntimeException(String message) {
        super(message);
    }

    public IORuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IORuntimeException(Throwable cause) {
        super(cause != null ? (cause.getClass().getSimpleName() + ": " + cause.getMessage()) : "null", cause);
    }

}
