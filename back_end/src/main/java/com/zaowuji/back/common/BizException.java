package com.zaowuji.back.common;

/**
 * 业务异常：携带错误码，由全局异常处理器转为统一响应体
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
