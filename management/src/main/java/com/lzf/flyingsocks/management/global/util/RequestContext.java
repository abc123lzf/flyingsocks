package com.lzf.flyingsocks.management.global.util;

import com.lzf.flyingsocks.management.global.security.DefaultAuthenticationToken;
import com.lzf.flyingsocks.management.global.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Objects;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

public abstract class RequestContext {

    public static void storeContextPrincipal(UserDetails userDetails) {
        Objects.requireNonNull(userDetails, "Principal should not null");
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx == null) {
            throw new IllegalStateException();
        }

        if (!(userDetails instanceof UserPrincipal)) {
            throw new IllegalArgumentException();
        }

        ctx.setAuthentication(new DefaultAuthenticationToken((UserPrincipal) userDetails));
        sessionStore(SPRING_SECURITY_CONTEXT_KEY, ctx);
    }


    /**
     * 存储Session
     * @param key 键
     * @param value 值
     */
    public static void sessionStore(String key, Object value) {
        HttpSession session = contextSession();
        session.setAttribute(key, value);
    }

    /**
     * 删除Session
     * @param key 键
     */
    public static void sessionRemove(String key) {
        HttpSession session = contextSession();
        session.removeAttribute(key);
    }

    /**
     * 获取Session
     * @param key 键
     * @param <T> 值的类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSessionValue(String key) {
        HttpSession session = contextSession();
        return (T) session.getAttribute(key);
    }

    public static HttpServletRequest contextRequest() {
        return contextServletRequestAttributes().getRequest();
    }

    public static HttpServletResponse contextResponse() {
        return contextServletRequestAttributes().getResponse();
    }

    private static HttpSession contextSession() {
        return contextServletRequestAttributes().getRequest().getSession();
    }

    private static ServletRequestAttributes contextServletRequestAttributes() {
        RequestAttributes attr = RequestContextHolder.currentRequestAttributes();
        if (!(attr instanceof ServletRequestAttributes)) {
            throw new IllegalStateException("Context request attributes is not right.");
        }

        return (ServletRequestAttributes) attr;
    }
}
