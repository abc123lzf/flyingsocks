package com.lzf.flyingsocks.management.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Objects;

public class DefaultAuthenticationToken extends AbstractAuthenticationToken {

    private final UserPrincipal principal;

    public DefaultAuthenticationToken(UserPrincipal principal) {
        super(Objects.requireNonNull(principal).getAuthorities());
        this.principal = principal;
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return null;
    }
}
