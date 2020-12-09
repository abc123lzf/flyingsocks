package com.lzf.flyingsocks.management.global.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;

public abstract class ResponseContext {

    public static void setStatusCode(int code) {
        get().setStatus(code);
    }

    private static HttpServletResponse get() {
        RequestAttributes attr = RequestContextHolder.currentRequestAttributes();
        if (!(attr instanceof ServletRequestAttributes)) {
            throw new IllegalStateException("Context request attributes is not right.");
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) attr;
        return attributes.getResponse();
    }

}
