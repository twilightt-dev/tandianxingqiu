package com.dianping.security;

import com.dianping.dto.UserDTO;
import com.dianping.filter.JwtAuthenticationFilter;
import com.dianping.utils.UserHolder;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXM=";

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
        UserHolder.removeUser();
    }

    @Test
    void validBearerTokenAuthenticatesForCurrentRequestAndCleansUserHolder() throws Exception {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofMinutes(30));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);
        UserDTO user = new UserDTO();
        user.setId(1010L);
        user.setNickName("user_test");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtUtils.generateToken(user.getId().toString(), java.util.Map.of("nickName", user.getNickName())));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();
        FilterChain chain = (req, res) -> {
            chainCalled.set(true);
            assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
            assertEquals(1010L, UserHolder.getUser().getId());
        };

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertNull(UserHolder.getUser());
    }

    @Test
    void invalidTokenDoesNotAuthenticateRequest() throws Exception {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofMinutes(30));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> chainCalled.set(true));

        assertTrue(chainCalled.get());
        assertFalse(SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }
}

