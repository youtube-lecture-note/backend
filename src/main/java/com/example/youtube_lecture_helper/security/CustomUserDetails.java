package com.example.youtube_lecture_helper.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections; // 예시를 위해 추가

public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email; // 혹은 email
    private String password; // JWT 방식에서는 사용하지 않을 수 있음
    private Collection<? extends GrantedAuthority> authorities;
    public CustomUserDetails(Long id, String email){
        this.id = id;
        this.email = email;
    }

    // 생성자, getter 등
    public CustomUserDetails(Long id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password; // JWT 인증 시에는 이 필드가 직접 사용되지는 않음
        this.authorities = authorities;
    }

    // JWT에서 사용자 정보를 복원할 때 사용할 생성자
    public CustomUserDetails(Long id, String email, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = null; // JWT에는 비밀번호가 없으므로 null 또는 빈 문자열
        this.authorities = authorities;
    }


    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : Collections.emptyList();
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}