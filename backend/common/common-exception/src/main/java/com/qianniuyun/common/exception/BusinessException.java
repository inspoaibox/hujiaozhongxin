package com.qianniuyun.common.exception;

/**
 * 业务异常基类
 * 作者：深圳市千牛云科技有限公司
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public int getCode() {
        return code;
    }
}
