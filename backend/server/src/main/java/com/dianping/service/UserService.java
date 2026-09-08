package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.LoginFormDTO;
import com.dianping.result.Result;
import com.dianping.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserService extends IService<User> {

    Result<Void> sendCode(String phone);

    Result<String> login(LoginFormDTO loginForm);
}
