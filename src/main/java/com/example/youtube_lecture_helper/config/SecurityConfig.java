package com.example.youtube_lecture_helper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //CORS 설정
                //추후 토큰 관리 필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()   //일단 모두 허용
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 설정
        // configuration.setAllowedOrigins(Arrays.asList(
        //         "http://localhost:3000",
        //         "https://example.com"
        // ));
        configuration.setAllowedOrigins(Arrays.asList("*"));

        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // 브라우저가 접근할 수 있는 헤더 설정
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // 자격 증명(쿠키 등) 허용
        configuration.setAllowCredentials(true);

        // Max Age 설정 (초 단위)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 적용

        return source;
    }
}