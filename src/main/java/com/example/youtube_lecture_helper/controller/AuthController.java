package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.security.CustomUserDetails;
import com.example.youtube_lecture_helper.security.JwtTokenProvider;
import com.example.youtube_lecture_helper.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
import com.example.youtube_lecture_helper.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.*;


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
            if (userDetails == null) {  //기존에 없던 사용자
                // 신규 사용자 생성 로직 (이름, 권한 등 설정)
                // 2. DB에 저장 (자동으로 ID가 생성됨)
                User savedUser = userService.createUser(email,"USER");
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(savedUser.getRole()));
                userDetails = new CustomUserDetails(savedUser.getId(), savedUser.getEmail(), "", authorities);
            }
            // JWT 생성
            String jwt = tokenProvider.generateTokenFromUserId(userDetails.getId(), userDetails.getUsername(), userDetails.getAuthorities());
            tokenProvider.addTokenCookie(response,jwt);
            // HTTP-only 쿠키에 JWT 저장
            //Cookie jwtCookie = tokenProvider.createTokenCookie(jwt);
            //response.addCookie(jwtCookie);

            return ResponseEntity.ok().body("Login successful. JWT set in HttpOnly cookie.");

        } catch (Exception e) {
            // logger.error("Google Sign-In error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkAuthentication(HttpServletRequest request) {
        try {
            // 쿠키에서 JWT 토큰 추출
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Optional<Cookie> jwtCookie = Arrays.stream(cookies)
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .findFirst();
            if (jwtCookie.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String token = jwtCookie.get().getValue();
            System.out.println(token);

            // 토큰 검증
            if (!tokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 인증 성공 시 200 상태 코드만 반환
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/check-admin")
    public ResponseEntity<?> checkAdmin(@AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()) || "ADMIN".equals(auth.getAuthority()));

        Map<String, Object> result = new HashMap<>();
        result.put("isAdmin", isAdmin);
        return ResponseEntity.ok(result);
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
