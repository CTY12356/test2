package com.health.management.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.entity.User;
import com.health.management.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                long userId = Long.parseLong(claims.getSubject());
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
                if (user == null || "BANNED".equals(user.getStatus())) {
                    SecurityContextHolder.clearContext();
                } else {
                    // 与数据库一致；去空格并大写，避免 "ADMIN "、"admin" 导致鉴权失败
                    String raw = user.getRole();
                    String role = (raw == null || raw.isBlank()) ? "USER" : raw.trim().toUpperCase();
                    if (!"ADMIN".equals(role) && !"USER".equals(role)) {
                        role = "USER";
                    }
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
