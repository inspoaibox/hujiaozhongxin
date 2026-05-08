package com.qianniuyun.auth.service;

import com.qianniuyun.auth.dto.LoginRequest;
import com.qianniuyun.auth.dto.LoginResponse;
import com.qianniuyun.auth.entity.User;
import com.qianniuyun.auth.repository.UserRepository;
import com.qianniuyun.common.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}")
    private long jwtExpiration;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("用户 {} 登录失败：密码错误, IP={}", request.getUsername(), ipAddress);
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // 生成 JWT Token
        String token = generateToken(user);

        // 记录审计日志
        auditLogService.log(user.getId(), "LOGIN", "用户登录", ipAddress);

        log.info("用户 {} 登录成功, IP={}", user.getUsername(), ipAddress);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole().getCode())
                .expiresIn(jwtExpiration)
                .build();
    }

    /**
     * 用户登出
     */
    public void logout(Long userId, String ipAddress) {
        auditLogService.log(userId, "LOGOUT", "用户登出", ipAddress);
        log.info("用户 {} 登出, IP={}", userId, ipAddress);
    }

    /**
     * 生成 JWT Token
     */
    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().getCode());
        claims.put("realName", user.getRealName());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration * 1000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.log(userId, "CHANGE_PASSWORD", "修改密码", null);
    }
}
