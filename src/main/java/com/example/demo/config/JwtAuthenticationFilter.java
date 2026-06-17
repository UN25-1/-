package com.example.demo.config;

import com.example.demo.repository.MerchantDetailRepository;
import com.example.demo.repository.RiderDetailRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 —— 每个请求拦截校验Token，设置安全上下文
 * 同时实时校验商家/骑手的 enabled 状态，防止禁用后 Token 未过期期间仍能访问
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final TokenBlacklist tokenBlacklist;
    private final MerchantDetailRepository merchantDetailRepository;
    private final RiderDetailRepository riderDetailRepository;

    public JwtAuthenticationFilter(JwtUtils jwtUtils,
                                    TokenBlacklist tokenBlacklist,
                                    MerchantDetailRepository merchantDetailRepository,
                                    RiderDetailRepository riderDetailRepository) {
        this.jwtUtils = jwtUtils;
        this.tokenBlacklist = tokenBlacklist;
        this.merchantDetailRepository = merchantDetailRepository;
        this.riderDetailRepository = riderDetailRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            // 无Token，继续过滤器链（由SecurityConfig决定是否放行）
            filterChain.doFilter(request, response);
            return;
        }

        // 检查Token是否已在黑名单中（登出失效）
        if (tokenBlacklist.isBlacklisted(token)) {
            log.warn("Token已被注销，拒绝访问: {}", getShortToken(token));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token已失效，请重新登录\"}");
            return;
        }

        // 校验Token有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token无效或已过期: {}", getShortToken(token));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期，请重新登录\"}");
            return;
        }

        // Token有效，提取用户信息并实时校验账户状态
        try {
            Integer userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            String role = jwtUtils.getRoleFromToken(token);

            // 实时校验：商家/骑手是否被管理员禁用（白名单：资质上传相关接口放行）
            if ("merchant".equalsIgnoreCase(role)) {
                merchantDetailRepository.findByUserId(userId).ifPresent(detail -> {
                    if (Boolean.FALSE.equals(detail.getEnabled())) {
                        // 允许未入驻商家访问资质上传相关接口
                        if (!isQualificationEndpoint(request)) {
                            log.warn("商家已被禁用，拒绝访问: userId={}, username={}", userId, username);
                            throw new AccountDisabledException("您的店铺已被平台禁用，请联系管理员");
                        }
                    }
                });
            } else if ("rider".equalsIgnoreCase(role)) {
                riderDetailRepository.findByUserId(userId).ifPresent(detail -> {
                    if (Boolean.FALSE.equals(detail.getEnabled())) {
                        if (!isQualificationEndpoint(request)) {
                            log.warn("骑手已被禁用，拒绝访问: userId={}, username={}", userId, username);
                            throw new AccountDisabledException("您的骑手账号已被禁用，请联系管理员");
                        }
                    }
                });
            }

            // 构建认证对象，角色前加 "ROLE_" 前缀以匹配 Spring Security
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT认证成功: userId={}, username={}, role={}", userId, username, role);
        } catch (AccountDisabledException e) {
            log.warn("账户状态异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"" + e.getMessage() + "\"}");
            return;
        } catch (Exception e) {
            log.error("解析Token失败", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getShortToken(String token) {
        if (token.length() <= 20) return token;
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }

    /** 判断是否为资质上传相关接口（未入驻商家/骑手的白名单） */
    private boolean isQualificationEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/qualification/") || uri.startsWith("/api/user/profile");
    }

    /** 内部异常：商家/骑手被禁用时抛出，在 doFilterInternal 中统一捕获处理 */
    private static class AccountDisabledException extends RuntimeException {
        AccountDisabledException(String message) { super(message); }
    }
}
