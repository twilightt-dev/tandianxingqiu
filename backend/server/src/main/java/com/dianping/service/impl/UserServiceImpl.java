package com.dianping.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dianping.constant.RedisConstants;
import com.dianping.constant.SystemConstants;
import com.dianping.security.JwtUtils;
import com.dianping.utils.RegexUtils;
import com.dianping.dto.LoginFormDTO;
import com.dianping.result.Result;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;
//构造器注入
    public UserServiceImpl(StringRedisTemplate stringRedisTemplate, JwtUtils jwtUtils) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Result<Void> sendCode(String phone) {
        //先校验手机号
        //如果格式不对就直接return
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.error("手机号格式错误！") ;
        }
        // 格式正确就生成验证码并保存到 Redis
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_CODE_KEY + phone,
                code,
                RedisConstants.LOGIN_CODE_TTL,
                TimeUnit.MINUTES);
        log.debug("发送短信验证码成功，验证码:{}" , code) ;
        return Result.success() ;
    }

    @Override
    //TODO 没实现密码登录啊
    public Result<String> login(LoginFormDTO loginForm) {
        //校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.error("手机号格式错误！") ;
        }
        //校验验证码
        String code = loginForm.getCode();
        String codeKey = RedisConstants.LOGIN_CODE_KEY + phone;
        String cacheCode = stringRedisTemplate.opsForValue().get(codeKey);
        if(cacheCode == null || !cacheCode.equals(code)){
            return Result.error("验证码错误") ;
        }
        //验证码一致，就根据手机号查询用户
        User user = getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone));
        //如果用户不存在就先创建用户
        if(user == null){
            user = createUserWithPhone(phone) ;
        }
        //只选择 JWT 所需的公开信息，避免令牌工具依赖业务 DTO
        Map<String, Object> claims = new HashMap<>();
        claims.put("nickName", user.getNickName());
        claims.put("icon", user.getIcon());
        String token = jwtUtils.generateToken(user.getId().toString(), claims);

        //已经完成了登录认证，所以要删掉验证码
        stringRedisTemplate.delete(codeKey);
        return Result.success(token) ;
    }

    private User createUserWithPhone(String phone){
        //创建用户
        User user = new User() ;
        user.setPhone(phone) ;
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10)) ;
        //保存用户到数据库
        save(user) ;
        return user ;
    }
}
