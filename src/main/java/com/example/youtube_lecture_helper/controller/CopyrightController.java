package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.dto.CopyrightCheckDTO;
import com.example.youtube_lecture_helper.service.CopyrightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class CopyrightController {

    private CopyrightService copyrightService;

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

    @PostMapping("/api/copyright/ban/")
    public ApiResponse<String> banVideo(
            @RequestParam String videoId,
            @RequestParam String owner
    ){
        String result = copyrightService.banVideo(videoId,owner);
        if(result.equals(videoId)){
            return ApiResponse.success(null);
        } else if(result.equals("이미 차단된 영상입니다.")) {
            return ApiResponse.error(400,result);
        } else {
            return ApiResponse.error(500,result);
        }
    }
}
