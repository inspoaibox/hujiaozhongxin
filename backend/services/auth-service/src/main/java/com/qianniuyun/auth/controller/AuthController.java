package com.qianniuyun.auth.controller;

import com.qianniuyun.auth.dto.LoginRequest;
import com.qianniuyun.auth.dto.LoginResponse;
import com.qianniuyun.auth.service.AuthService;
import com.qianniuyun.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 作者：深圳市千牛云科技有限公司
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        LoginResponse response = authService.login(request, ipAddress);
        return Result.success(response);
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId,
                               HttpServletRequest httpRequest) {
        authService.logout(userId, getClientIp(httpRequest));
        return Result.success();
    }

    /**
     * 修改密码
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam String oldPassword,
                                       @RequestParam String newPassword) {
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
