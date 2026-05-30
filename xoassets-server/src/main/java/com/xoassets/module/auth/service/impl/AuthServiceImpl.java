package com.xoassets.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.JwtTokenProvider;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.auth.dto.LoginRequest;
import com.xoassets.module.auth.dto.RegisterRequest;
import com.xoassets.module.auth.service.AuthService;
import com.xoassets.module.auth.vo.LoginVO;
import com.xoassets.module.auth.vo.UserVO;
import com.xoassets.persistence.entity.User;
import com.xoassets.persistence.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务：负责注册、登录和当前用户查询。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 注册新用户，密码只保存 BCrypt 哈希。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        return toVO(user);
    }

    /**
     * 校验用户名和密码，成功后刷新最后登录时间并签发 JWT。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getStatus, 1));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginAt, LocalDateTime.now()));

        return LoginVO.builder()
                .token(jwtTokenProvider.createToken(user.getId(), user.getUsername()))
                .user(toVO(user))
                .build();
    }

    /**
     * 返回当前登录用户资料。
     */
    @Override
    public UserVO me() {
        Long userId = LoginUserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    /**
     * 转换为用户展示对象，避免返回密码哈希。
     */
    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
