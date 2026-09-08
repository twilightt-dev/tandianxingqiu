package com.dianping.filter;

import com.dianping.dto.UserDTO;
import com.dianping.security.JwtUtils;
import com.dianping.utils.UserHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component                      //springWeb提供的基类，在一次http请求中，确保该过滤器只执行一次
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    //依旧构造器注入
    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        UserDTO user = authenticate(request);
        try {
            if (user != null) {
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                user, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserHolder.saveUser(user);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserHolder.removeUser();
        }
    }

    private UserDTO authenticate(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            UserDTO user = new UserDTO();
            user.setId(Long.valueOf(claims.getSubject()));
            user.setNickName(claims.get("nickName", String.class));
            user.setIcon(claims.get("icon", String.class));
            return user;
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }
}
