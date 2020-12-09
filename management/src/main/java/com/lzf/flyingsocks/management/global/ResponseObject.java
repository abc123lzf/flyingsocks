package com.lzf.flyingsocks.management.global;

import org.springframework.lang.Nullable;

public interface ResponseObject<T> {

    /**
     * @return 执行结果代码，一般0表示成功，其含义由控制层自定义
     */
    int getCode();


    /**
     * @return 附加信息
     */
    default String getMsg() {
        return getCode() == 0 ? "SUCCESS" : "FAILURE";
    }


    /**
     * @return 获取封装数据
     */
    @Nullable
    default T getData() {
        return null;
    }

    /**
     * 按照模版生存新的ResponseObject
     */
    <U>ResponseObject<U> build(U data);

    /**
     * 构建无data的ResponseObject
     */
    static <T> ResponseObject<T> build(int code, String msg) {
        return new StandardResponseObject<>(code, msg, null);
    }

    /**
     * 构建有data的ResponseObject
     */
    static <T> ResponseObject<T> build(int code, String msg, T data) {
        return new StandardResponseObject<>(code, msg, data);
    }


    @SuppressWarnings("unchecked")
    static <T> ResponseObject<T> success() {
        return (ResponseObject<T>) StandardResponseObject.SUCCESS;
    }

    @SuppressWarnings("unchecked")
    static <T> ResponseObject<T> failure() {
        return (ResponseObject<T>) StandardResponseObject.FAILURE;
    }
}


class StandardResponseObject<T> implements ResponseObject<T> {

    @SuppressWarnings("rawtypes")
    static final StandardResponseObject FAILURE = new StandardResponseObject<>(0, "FAILURE", null);

    @SuppressWarnings("rawtypes")
    static final StandardResponseObject SUCCESS = new StandardResponseObject<>(0, "SUCCESS", null);

    private final int code;
    private final String msg;
    private final T data;

    StandardResponseObject(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public T getData() {
        return data;
    }

    public <U> StandardResponseObject<U> build(U data) {
        return new StandardResponseObject<>(this.code, this.msg, data);
    }
}