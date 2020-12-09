package com.lzf.flyingsocks.management.global.security;

import org.springframework.security.core.GrantedAuthority;

public enum Authority implements GrantedAuthority {

    ;

    private final String key;

    Authority(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getAuthority() {
        return key;
    }
}
