package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.entity.Ban;
import com.example.youtube_lecture_helper.dto.CopyrightCheckDTO;
import com.example.youtube_lecture_helper.service.CopyrightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.youtube_lecture_helper.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Optional;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CopyrightController {

    private final CopyrightService copyrightService;

    @GetMapping("/api/copyright/check/{videoId}")
    public ResponseEntity<ApiResponse<CopyrightCheckDTO>> check(
            @PathVariable String videoId
    ){
        Optional<CopyrightCheckDTO> result = copyrightService.check(videoId);
        // 저작권자와 처리 일시 전송
        return result.map(copyrightCheckDTO -> ApiResponse.buildResponse(
                HttpStatus.CONFLICT, "DENIED", copyrightCheckDTO)).orElseGet(()
                -> ApiResponse.buildResponse(HttpStatus.OK, "SUCCESS", null));
    }

    @PostMapping("/api/copyright/ban")
    public ApiResponse<String> banVideo(
            @RequestParam String videoId,
            @RequestParam String owner,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()) || "ADMIN".equals(auth.getAuthority()));
        if(isAdmin){
            String result = copyrightService.banVideo(videoId,owner);
            if(result.equals(videoId)){
                return ApiResponse.success(null);
            } else if(result.equals("이미 차단된 영상입니다.")) {
                return ApiResponse.error(400,result);
            } else {
                return ApiResponse.error(500,result);
            }
        }
        return ApiResponse.error(500,"NOT ADMIN");
    }

    @GetMapping("/api/copyright")
    public ResponseEntity<?> getAllBans(@AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()) || "ADMIN".equals(auth.getAuthority()));
        if (!isAdmin) {
            // 권한 없으면 403 Forbidden 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자만 접근할 수 있습니다.");
        }
        List<Ban> bans = copyrightService.getAllBans();
        return ResponseEntity.ok(bans);
    }

}
