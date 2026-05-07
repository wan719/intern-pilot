package com.internpilot.security;

import com.internpilot.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;

    private final String username;

    private final String password;

    private final Boolean enabled;

    private final List<String> roles;

    private final List<String> permissions;

    public CustomUserDetails(
            User user,
            List<String> roles,
            List<String> permissions
    ) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = user.getEnabled() != null && user.getEnabled() == 1;
        this.roles = roles == null ? List.of() : roles;
        this.permissions = permissions == null ? List.of() : permissions;
    }

    public CustomUserDetails(
            Long userId,
            String username,
            List<String> roles,
            List<String> permissions
    ) {
        this.userId = userId;
        this.username = username;
        this.password = null;
        this.enabled = true;
        this.roles = roles == null ? List.of() : roles;
        this.permissions = permissions == null ? List.of() : permissions;
    }

    public CustomUserDetails(Long userId, String username, String role) {
        this(
                userId,
                username,
                role == null || role.isBlank() ? List.of() : List.of(role),
                List.of()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        return authorities;
    }

    public String getPrimaryRole() {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        return roles.get(0);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
