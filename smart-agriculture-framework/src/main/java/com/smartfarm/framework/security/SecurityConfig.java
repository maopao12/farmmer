package com.smartfarm.framework.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置
 *
 * @author SmartFarm Team
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（REST API 不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 跨域配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 无状态会话
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 路由权限配置
            .authorizeHttpRequests(auth -> auth
                    // 认证接口无需鉴权
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    // WebSocket 端点放行
                    .requestMatchers("/ws/**").permitAll()
                    // ADMIN ONLY 接口
                    .requestMatchers(HttpMethod.POST, "/api/v1/plot").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/plot/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/plot/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/device/bind").hasRole("ADMIN")
                    .requestMatchers("/api/v1/device/unbind/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/device/unbound").hasRole("ADMIN")
                    // 其他接口需要认证
                    .anyRequest().authenticated()
            )
            // JWT 过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
