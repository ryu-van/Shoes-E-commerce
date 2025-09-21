package com.example.shoozy_shop.security;

import com.example.shoozy_shop.model.Token;
import com.example.shoozy_shop.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Lấy JWT từ header, validate, check revoke/expire trong DB,
 * chặn user bị khóa (isActive=false) bằng 401 ACCOUNT_LOCKED.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenRepository tokenRepository;

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = parseJwt(request);

        try {
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // 1) Kiểm tra token trong DB
                Optional<Token> tokenEntityOpt = tokenRepository.findByToken(jwt);
                if (tokenEntityOpt.isEmpty()) {
                    writeUnauthorized(response, "INVALID_TOKEN", "Token không tồn tại");
                    return;
                }
                Token tokenEntity = tokenEntityOpt.get();
                if (Boolean.TRUE.equals(tokenEntity.getRevoked()) || Boolean.TRUE.equals(tokenEntity.getExpired())) {
                    writeUnauthorized(response, "TOKEN_REVOKED", "Token đã bị thu hồi hoặc hết hạn");
                    return;
                }

                // 2) Load user và chặn nếu bị khóa
                String email = jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Nếu dùng UserDetailsImpl, 2 hàm dưới cùng trả theo isActive
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    writeUnauthorized(response, "ACCOUNT_LOCKED", "Tài khoản đã bị khoá");
                    return;
                }

                // 3) OK -> set Authentication
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Có lỗi xác thực -> trả 401 chung
            System.err.println("Error trong JwtAuthenticationFilter: " + e.getMessage());
            writeUnauthorized(response, "UNAUTHORIZED", "Không thể xác thực người dùng");
            return;
        }

        // Tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }
}
