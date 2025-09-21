package com.example.shoozy_shop.security;

import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Lớp này giúp chuyển đổi từ entity User của bạn thành UserDetails cho Spring Security.
 */
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean isActive;

    public UserDetailsImpl(Long id, String email, String password, String roleName, Boolean isActive) {
        this.id = id;
        this.email = email;
        this.password = password;
        // Chỉ có một authority là roleName (ví dụ "Admin" hoặc "Customer")
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));
        this.isActive = isActive;
    }

    /**
     * Chuyển từ entity User thành UserDetailsImpl.
     */
    public static UserDetailsImpl build(User user) {
        Role role = user.getRole();
        return new UserDetailsImpl(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            role.getName(),
            user.getIsActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Nếu isActive = false, coi như account bị khóa
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // Getter bổ sung nếu cần lấy id
    public Long getId() {
        return id;
    }
}
