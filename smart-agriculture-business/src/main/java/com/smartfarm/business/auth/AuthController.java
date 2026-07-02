package com.smartfarm.business.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.User;
import com.smartfarm.business.mapper.UserMapper;
import com.smartfarm.common.R;
import com.smartfarm.common.exception.BizException;
import com.smartfarm.framework.security.JwtUtils;
import com.smartfarm.framework.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 登录/获取当前用户
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (user == null || user.getEnabled() == 0) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return R.ok(new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole()
        ));
    }

    @GetMapping("/me")
    public R<UserInfo> me() {
        Long userId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return R.ok(new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getPhone()
        ));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    public record LoginResponse(String token, Long userId, String username,
                                 String realName, String role) {}

    public record UserInfo(Long id, String username, String realName,
                            String role, String phone) {}
}
