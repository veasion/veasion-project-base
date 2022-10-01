package cn.veasion.project;

import cn.veasion.project.model.ErrCodeType;
import cn.veasion.project.model.R;

/**
 * BusinessException
 *
 * @author luozhuowei
 * @date 2022/10/1
 */
public class BusinessException extends RuntimeException {

    private int code = R.ERROR;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrCodeType errCodeType) {
        this(errCodeType.getCode(), errCodeType.getMessage());
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
