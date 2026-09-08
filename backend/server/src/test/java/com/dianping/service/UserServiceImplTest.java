package com.dianping.service;

import com.dianping.constant.RedisConstants;
import com.dianping.result.Result;
import com.dianping.dto.LoginFormDTO;
import com.dianping.entity.User;
import com.dianping.security.JwtUtils;
import com.dianping.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtils jwtUtils;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(redisTemplate, jwtUtils);
    }

    @Test
    void validPhoneStoresSixDigitCodeInRedisWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        Result<Void> result = userService.sendCode("13331814920");

        assertEquals(1, result.getCode());
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(RedisConstants.LOGIN_CODE_KEY + "13331814920"),
                codeCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(RedisConstants.LOGIN_CODE_TTL),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
    }

    @Test
    void invalidPhoneDoesNotWriteRedis() {
        Result<Void> result = userService.sendCode("123");

        assertEquals(0, result.getCode());
        assertEquals("手机号格式错误！", result.getMsg());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void successfulLoginReturnsJwtAndConsumesVerificationCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + "13331814920"))
                .thenReturn("568383");
        User user = new User();
        user.setId(1010L);
        user.setPhone("13331814920");
        user.setNickName("user_test");
        UserServiceImpl service = spy(userService);
        doReturn(user).when(service).getOne(any());
        when(jwtUtils.generateToken(any(), any(Map.class))).thenReturn("signed.jwt.token");
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("13331814920");
        form.setCode("568383");

        Result<String> result = service.login(form);

        assertEquals(1, result.getCode());
        assertEquals("signed.jwt.token", result.getData());
        verify(redisTemplate).delete(RedisConstants.LOGIN_CODE_KEY + "13331814920");
    }
}

