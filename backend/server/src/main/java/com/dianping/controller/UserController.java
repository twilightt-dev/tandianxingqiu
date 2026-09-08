package com.dianping.controller;


import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.UserDTO;
import com.dianping.entity.UserInfo;
import com.dianping.result.Result;
import com.dianping.service.IUserInfoService;
import com.dianping.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import com.dianping.utils.UserHolder;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("/code")
    @Operation(summary = "发送手机验证码")
    public Result<Void> sendCode(@RequestParam("phone") String phone) {

        return userService.sendCode(phone) ;
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    //review：注解@RequestBody就是把前端传来的json反序列化为java对象
    public Result<String> login(@RequestBody LoginFormDTO loginForm){
        return userService.login(loginForm) ;
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    @Operation(summary = "用户退出登录")
    public Result<Void> logout(){
        // JWT 是无状态的，服务端无需删除会话；前端负责删除本地 Token
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录用户")
    public Result<UserDTO> me(){

        return Result.success(UserHolder.getUser());
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "查询用户详情")
    public Result<UserInfo> info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.success();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.success(info);
    }
}
