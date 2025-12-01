package com.project.skillswap.logic.entity.http;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HttpResponse<T> {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);
    private  String message;
    private T data;

    private Meta meta;


    public HttpResponse(String message) {
        this.message = message;
    }
    public HttpResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public HttpResponse(String message, Meta meta) {
        this.message = message;
        this.meta = meta;
    }

    public HttpResponse(String message, T data, Meta meta) {
        this.message = message;
        this.data = data;
        this.meta = meta;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}
