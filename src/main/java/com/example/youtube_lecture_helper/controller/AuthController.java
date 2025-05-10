package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.security.CustomUserDetails;
import com.example.youtube_lecture_helper.security.JwtTokenProvider;
import com.example.youtube_lecture_helper.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService; // 사용자 정보를 DB에서 조회하거나 새로 생성하는 서비스

    @Value("${google.client-id}")
    private String googleClientId;


    // 프론트에서 구글 로그인 후 ID 토큰을 이쪽으로 보낸다고 가정
    @PostMapping("/google/callback")
    public ResponseEntity<?> googleLoginCallback(@RequestBody GoogleTokenRequest tokenRequest, HttpServletResponse response) {
        String idTokenString = tokenRequest.getIdToken();

        try {
            // === Google ID 토큰 검증 ===
             GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
             GoogleIdToken idToken = verifier.verify(idTokenString);
             if (idToken == null) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID Token");
             }
             Payload payload = idToken.getPayload();
             String email = payload.getEmail();
             String googleUserId = payload.getSubject();
             String name = (String) payload.get("name");


            // DB에서 사용자 조회 또는 신규 생성
            CustomUserDetails userDetails = userService.loadUserByUsername(email); // 또는 findByGoogleId
            if (userDetails == null) {
                // 신규 사용자 생성 로직 (이름, 권한 등 설정)
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                Long newUserId = System.currentTimeMillis() % 100000; // 임시 ID
                userDetails = new CustomUserDetails(newUserId, email, "", authorities);
                userService.createUser(userDetails); // DB에 사용자 저장
            }


            // JWT 생성
            String jwt = tokenProvider.generateTokenFromUserId(userDetails.getId(), userDetails.getUsername(), userDetails.getAuthorities());

            // HTTP-only 쿠키에 JWT 저장
            Cookie jwtCookie = tokenProvider.createTokenCookie(jwt);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok().body("Login successful. JWT set in HttpOnly cookie.");

        } catch (Exception e) {
            // logger.error("Google Sign-In error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = tokenProvider.createLogoutCookie();
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out successfully");
    }

    // GoogleTokenRequest DTO
    static class GoogleTokenRequest {
        private String idToken;
        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
    }
}
