package com.qianniuyun.auth.config;

import com.qianniuyun.auth.entity.Role;
import com.qianniuyun.auth.entity.User;
import com.qianniuyun.auth.repository.RoleRepository;
import com.qianniuyun.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultUserInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username:admin}")
    private String adminUsername;

    @Value("${app.default-admin.password:Admin@2025}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode("ADMIN");
                    role.setName("管理员");
                    return roleRepository.save(role);
                });

        var existing = userRepository.findByUsername(adminUsername);
        if (existing.isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRealName("系统管理员");
            admin.setEmail("admin@qianniuyun.com");
            admin.setStatus("ACTIVE");
            admin.setRole(adminRole);
            userRepository.save(admin);
            return;
        }

        User admin = existing.get();
        if (admin.getPassword() == null
                || "__RESET_ON_STARTUP__".equals(admin.getPassword())
                || admin.getPassword().contains("placeholder_bcrypt_hash")) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setStatus("ACTIVE");
            admin.setRole(adminRole);
            userRepository.save(admin);
        }
    }
}
