package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置
 *
 * - JWT无状态认证 + BCrypt密码加密 + 角色级权限控制
 * - 注册/登录接口放行，其余接口需携带Token
 * - 管理接口仅 admin 角色可访问
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * BCrypt 密码编码器 Bean
     * 用于注册时加密密码、登录时验证密码
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置源 —— 供 Spring Security 的 .cors() 使用
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 启用 CORS（使用上面定义的 CorsConfigurationSource）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 禁用 CSRF（前后端分离 + REST API 无需 CSRF 保护）
            .csrf(csrf -> csrf.disable())

            // 无状态会话（JWT认证，不创建HttpSession）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL 权限配置（角色级访问控制）
            .authorizeHttpRequests(auth -> auth
                // 上传图片的静态资源 —— 公开访问
                .requestMatchers("/uploads/**").permitAll()

                // 注册与登录接口无需认证
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                // 商品浏览接口 —— 公开访问（用户无需登录即可浏览商家与商品）
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                // 评价浏览 —— 公开访问（用户可在订单详情页查看骑手评价）
                .requestMatchers(HttpMethod.GET, "/api/review/merchant/**", "/api/review/rider/**").permitAll()

                // Token刷新与登出需要携带Token
                .requestMatchers("/api/auth/refresh", "/api/auth/logout").authenticated()

                // 用户个人信息与地址管理 —— 所有已认证用户可访问
                .requestMatchers("/api/user/**").authenticated()

                // 购物车模块 —— 所有已认证用户可访问
                .requestMatchers("/api/cart/**").authenticated()

                // 订单模块（用户端）—— 所有已认证用户可访问
                .requestMatchers("/api/orders/**").authenticated()

                // 支付模块 —— 所有已认证用户可访问
                .requestMatchers("/api/payment/**").authenticated()

                // 管理员接口 —— 仅 admin 角色可访问（后续模块扩展）
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 商家接口 —— 仅 merchant 角色可访问（包含商家订单管理 /api/merchant/orders/**）
                .requestMatchers("/api/merchant/**").hasRole("MERCHANT")

                // 骑手接口 —— 仅 rider 角色可访问（后续模块扩展）
                .requestMatchers("/api/rider/**").hasRole("RIDER")

                // 其他请求需要认证
                .anyRequest().authenticated()
            );

        // 将JWT过滤器添加到Spring Security过滤器链（在用户名密码认证之前）
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
