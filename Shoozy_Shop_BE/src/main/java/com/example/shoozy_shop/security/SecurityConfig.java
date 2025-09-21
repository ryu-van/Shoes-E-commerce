package com.example.shoozy_shop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${api.prefix}")
    private String apiPrefix;

    public SecurityConfig(UserDetailsServiceImpl uds, JwtAuthenticationFilter jwtF) {
        this.userDetailsService = uds;
        this.jwtAuthenticationFilter = jwtF;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String base = apiPrefix;
        http
                // 1. Tắt CSRF – vì chúng ta dùng JWT
                .csrf(csrf -> csrf.disable())
                // 2. Stateless session (không dùng session)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 3. Đăng ký provider để Spring biết dùng UserDetailsService + BCrypt
                .authenticationProvider(authenticationProvider())
                // 4. Phân quyền endpoint
                .authorizeHttpRequests(auth -> auth

                        // --- Public: register / login / logout ---
                        .requestMatchers(apiPrefix + "/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // --- Profile & Change Password (Customer, Staff, Admin) ---
                        .requestMatchers(HttpMethod.GET, base + "/users/profile")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/users/profile")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.POST, base + "/users/change-password")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        // --- Staff + Admin: chỉ xem được customer list & user detail ---
                        .requestMatchers(HttpMethod.GET, base + "/admin/users/customers")
                        .hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/admin/users/*").hasAnyAuthority("Staff", "Admin")

                        // --- Admin only: quản lý toàn bộ user ---
                        .requestMatchers(HttpMethod.GET, base + "/admin/users").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PATCH, base + "/admin/users/*/status").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.PATCH, base + "/admin/users/*/role").hasAuthority("Admin")

                        // -- Products --
                        .requestMatchers(HttpMethod.GET, base + "/products").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/products/all").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/products/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/products/check-name").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.GET, base + "/products/by-category").permitAll()

                        .requestMatchers(HttpMethod.POST, base + "/products").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/products/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/products/{id}").hasAnyAuthority("Staff", "Admin")

                        // -- Brands --
                        .requestMatchers(HttpMethod.GET, base + "/brands").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/brands/active").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/brands/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, base + "/brands").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/brands/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/brands/{id}").hasAnyAuthority("Staff", "Admin")

                        // -- Carts --
                        .requestMatchers(HttpMethod.POST, base + "/carts/add")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/carts/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/carts/quantity/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/carts/{userId}/items")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        // -- Categories --
                        .requestMatchers(HttpMethod.GET, base + "/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/categories/active").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/categories/{id}").permitAll()

                        .requestMatchers(HttpMethod.POST, base + "/categories").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/categories/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/categories/{id}").hasAnyAuthority("Staff", "Admin")

                        // -- Colors --
                        .requestMatchers(HttpMethod.GET, base + "/colors").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/colors/active").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/colors/{id}").permitAll()

                        .requestMatchers(HttpMethod.POST, base + "/colors").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/colors/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/colors/{id}").hasAnyAuthority("Staff", "Admin")

                        // -- Coupons --
                        .requestMatchers(HttpMethod.GET, base + "/coupons/order").hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/coupons").hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/coupons/filter").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/coupons/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.GET,base+ "/coupons/order").hasAnyAuthority("Customer", "Admin","Staff")
                        .requestMatchers(HttpMethod.POST, base + "/coupons/create").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/coupons/update/{id}").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/coupons/update-status/{id}").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/coupons/delete/{id}").hasAuthority("Admin")

                        // -- Invoice
                        .requestMatchers(HttpMethod.GET, base + "/invoice").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/invoice/{orderId}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.POST, base + "/invoice/{orderId}/send-email")
                        .hasAnyAuthority("Staff", "Admin")

                        // -- Materials --
                        .requestMatchers(HttpMethod.GET, base + "/materials").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/materials/active").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/materials/{id}").permitAll()

                        .requestMatchers(HttpMethod.POST, base + "/materials").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/materials/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/materials/{id}").hasAnyAuthority("Staff", "Admin")

                        // -- Order --
                        .requestMatchers(HttpMethod.GET, base + "/orders").hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/orders/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        .requestMatchers(HttpMethod.POST, base + "/orders")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        .requestMatchers(HttpMethod.PUT, base + "/orders/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/orders/status/{id}").hasAnyAuthority("Staff", "Admin")

                        .requestMatchers(HttpMethod.DELETE, base + "/orders/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        .requestMatchers(HttpMethod.GET, base + "/orders/{orderId}/returnable-items")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        // -- Product Variants --
                        .requestMatchers(HttpMethod.GET, base + "/product-variants").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/product-variants/{id}").permitAll()

                        .requestMatchers(HttpMethod.POST, base + "/product-variants").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/product-variants/{id}")
                        .hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/product-variants/{id}")
                        .hasAnyAuthority("Staff", "Admin")

                        // -- Product Variant Images --
                        .requestMatchers(HttpMethod.GET, base + "/product-variant-images/grouped-images-by-ids/{ids}")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, base + "/product-variant-images/uploads")
                        .hasAnyAuthority("Staff", "Admin")

                        // -- Promotions --
                        .requestMatchers(HttpMethod.GET, base + "/promotions")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/promotions/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.POST, base + "/promotions").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/promotions/{id}").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/promotions/status/{id}").hasAuthority("Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/promotions/active/{id}").hasAuthority("Admin")

                        // -- Reviews --
                        .requestMatchers(HttpMethod.GET, base + "/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/reviews/{id}").permitAll()
                        .requestMatchers(HttpMethod.PUT, base + "/reviews/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        // -- Sizes --
                        .requestMatchers(HttpMethod.GET, base + "/sizes").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/sizes/active").permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/sizes/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, base + "/sizes").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.PUT, base + "/sizes/{id}").hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.DELETE, base + "/sizes/{id}").hasAnyAuthority("Staff", "Admin")

                        // -- return_requests --

                        .requestMatchers(HttpMethod.GET, base + "/returns/user")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/returns/{id}")
                        .hasAnyAuthority("Customer", "Staff", "Admin")
                        .requestMatchers(HttpMethod.GET, base + "/admin/return-requests")
                        .hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.POST, base + "/admin/return-requests/**")
                        .hasAnyAuthority("Staff", "Admin")
                        .requestMatchers(HttpMethod.POST, base + "/returns")
                        .hasAnyAuthority("Customer", "Staff", "Admin")

                        .requestMatchers(HttpMethod.GET,  "/api/v1/payments/vnpay-return").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/payments/vnpay-ipn").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/vnpay-ipn").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/create-vnpay").authenticated()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(base + "/chat/**").permitAll()

                        .requestMatchers(base + "/contact/**").permitAll()
                        // --- Tất cả request khác ---
                        .anyRequest().authenticated());
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("http://localhost:5173"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            config.setAllowedHeaders(List.of("authorization", "content-type", "x-auth-token"));
            config.setExposedHeaders(List.of("x-auth-token"));
            config.setAllowCredentials(true);
            return config;
        }));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
