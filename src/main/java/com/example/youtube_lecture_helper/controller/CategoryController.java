package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.dto.CategoryRequestDto;
import com.example.youtube_lecture_helper.dto.CategoryResponseDto;
import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.security.CustomUserDetails;
import com.example.youtube_lecture_helper.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * 모든 카테고리 계층 구조와 비디오 정보를 조회
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId(); // 인증된 사용자의 ID 추출
        List<CategoryResponseDto> categories = categoryService.getAllCategoryHierarchyWithVideos(userId);
        return ResponseEntity.ok(categories);
    }
    @GetMapping("/test")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategoriesTest() {
        Long userId = 1L;
        List<CategoryResponseDto> categories = categoryService.getAllCategoryHierarchyWithVideos(userId);
        return ResponseEntity.ok(categories);
    }

    /**
     * 카테고리 추가
     */
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(
            @RequestBody CategoryRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        CategoryResponseDto response = categoryService.createCategory(
                request.getName(),
                request.getParentId(),
                userId
        );

        return ResponseEntity.ok(response);
    }
    @PostMapping("/test")
    public ResponseEntity<CategoryResponseDto> createCategoryTest(
            @RequestBody CategoryRequestDto request
    ) {
        CategoryResponseDto response = categoryService.createCategory(
                request.getName(),
                request.getParentId(),
                1L
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 카테고리와 그 비디오 정보를 조회
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDto> getCategoryWithVideos(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        CategoryResponseDto category = categoryService.getCategoryWithVideos(categoryId, userId);
        return ResponseEntity.ok(category);
    }

    /**
     * 카테고리에 비디오 추가
     */
    @PostMapping("/{categoryId}/videos/{videoId}")
    public ResponseEntity<Void> addVideoToCategory(
            @PathVariable Long categoryId,
            @PathVariable String videoId,
            @RequestParam(required = false) String userVideoName,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        categoryService.addVideoToCategory(userId, videoId, categoryId, userVideoName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("/{categoryId}/videos/{videoId}/test")
    public ResponseEntity<Void> addVideoToCategoryTest(
            @PathVariable Long categoryId,
            @PathVariable String videoId,
            @RequestParam(required = false) String userVideoName) {
        Long userId = 1L;
        categoryService.addVideoToCategory(userId, videoId, categoryId, userVideoName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 카테고리에서 비디오 제거
     */
    @DeleteMapping("/{categoryId}/videos/{videoId}")
    public ResponseEntity<Void> removeVideoFromCategory(
            @PathVariable Long categoryId,
            @PathVariable String videoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        categoryService.removeVideoFromCategory(userId, videoId, categoryId);
        return ResponseEntity.noContent().build();
    }

    /*
    * 카테고리 제거
    * */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> removeCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails){
        Long userId = ((CustomUserDetails) userDetails).getId();
        categoryService.removeCategory(userId,categoryId);
        return ResponseEntity.noContent().build();
    }
    
    //카테고리 이동
    @PutMapping("/{fromCategoryId}/videos/{videoId}/move/{toCategoryId}")
    public ResponseEntity<Void> moveVideoToAnotherCategory(
            @PathVariable Long fromCategoryId,
            @PathVariable String videoId,
            @PathVariable Long toCategoryId,
            @RequestParam(required = false) String userVideoName,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((CustomUserDetails) userDetails).getId();
        categoryService.removeVideoFromCategory(userId, videoId, fromCategoryId);
        categoryService.addVideoToCategory(userId, videoId, toCategoryId, userVideoName);
        return ResponseEntity.noContent().build();
    }
}
