package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.CategoryDto;
import com.example.youtube_lecture_helper.dto.UserVideoInfoDto;
import com.example.youtube_lecture_helper.entity.Category;
import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.entity.UserVideoCategory;
import com.example.youtube_lecture_helper.entity.Video;
import com.example.youtube_lecture_helper.exception.AccessDeniedException;
import com.example.youtube_lecture_helper.exception.EntityNotFoundException;
import com.example.youtube_lecture_helper.repository.CategoryRepository;
import com.example.youtube_lecture_helper.repository.UserRepository;
import com.example.youtube_lecture_helper.repository.UserVideoCategoryRepository;
import com.example.youtube_lecture_helper.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserVideoCategoryRepository userVideoCategoryRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public List<CategoryDto> getAllCategoryHierarchyWithVideos(Long userId) {
        // 루트 카테고리만 먼저 조회
        List<Category> rootCategories = categoryRepository.findByParentIdIsNull();

        // 모든 카테고리들의 계층 구조를 만들기
        List<CategoryDto> result = new ArrayList<>();
        for (Category rootCategory : rootCategories) {
            CategoryDto rootDto = buildCategoryHierarchy(rootCategory);
            // 해당 유저의 카테고리인 경우만 추가
            if (rootCategory.getUser() == null || rootCategory.getUser().getId().equals(userId)) {
                result.add(rootDto);
            }
        }

        // 모든 카테고리 ID 추출
        List<Long> allCategoryIds = getAllCategoryIds(result);

        // 비어있지 않은 경우만 조회 수행
        if (!allCategoryIds.isEmpty()) {
            // 모든 카테고리의 비디오 정보를 한 번에 조회
            List<UserVideoCategory> allVideos = userVideoCategoryRepository
                    .findByCategoryIdsWithVideo(allCategoryIds);

            // 비디오 정보를 각 카테고리 DTO에 할당
            mapVideosToCategories(result, allVideos, userId);
        }

        return result;
    }

    /**
     * 특정 카테고리와 그 하위 카테고리의 계층 구조를 재귀적으로 구성
     */
    private CategoryDto buildCategoryHierarchy(Category category) {
        CategoryDto dto = new CategoryDto(category);

        List<Category> childCategories = categoryRepository.findByParentId(category);
        for (Category child : childCategories) {
            dto.getChildren().add(buildCategoryHierarchy(child));
        }

        return dto;
    }

    /**
     * 모든 카테고리 ID를 추출하는 헬퍼 메서드
     */
    private List<Long> getAllCategoryIds(List<CategoryDto> categories) {
        List<Long> result = new ArrayList<>();
        for (CategoryDto category : categories) {
            result.add(category.getId());
            result.addAll(getAllCategoryIds(category.getChildren()));
        }
        return result;
    }

    /**
     * 비디오 정보를 카테고리 DTO에 매핑하는 헬퍼 메서드
     */
    private void mapVideosToCategories(List<CategoryDto> categories, List<UserVideoCategory> videos, Long userId) {
        // 카테고리 ID -> 카테고리 DTO 매핑을 위한 맵 생성
        Map<Long, CategoryDto> categoryMap = new HashMap<>();
        buildCategoryMap(categories, categoryMap);

        // 비디오를 해당 카테고리에 할당
        for (UserVideoCategory video : videos) {
            // 해당 유저의 비디오만 추가
            if (video.getUser().getId().equals(userId)) {
                Long categoryId = video.getCategory().getId();
                CategoryDto categoryDto = categoryMap.get(categoryId);
                if (categoryDto != null) {
                    categoryDto.getVideos().add(new UserVideoInfoDto(video));
                }
            }
        }
    }

    /**
     * 카테고리 ID -> DTO 매핑을 위한 헬퍼 메서드
     */
    private void buildCategoryMap(List<CategoryDto> categories, Map<Long, CategoryDto> categoryMap) {
        for (CategoryDto category : categories) {
            categoryMap.put(category.getId(), category);
            buildCategoryMap(category.getChildren(), categoryMap);
        }
    }

    /**
     * 특정 카테고리의 비디오만 조회
     */
    /**
     * 특정 카테고리의 비디오만 조회
     */
    public CategoryDto getCategoryWithVideos(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + categoryId));

        // 해당 카테고리가 유저에게 접근 권한이 있는지 확인
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("해당 카테고리에 접근할 권한이 없습니다.");
        }

        CategoryDto dto = new CategoryDto(category);

        // 하위 카테고리 구성 - ID만 사용하는 쿼리로 최적화
        List<Category> childCategories = categoryRepository.findByParentId(category);
        for (Category child : childCategories) {
            dto.getChildren().add(new CategoryDto(child));
        }

        // 비디오 정보 추가 - 필요한 필드만 포함하는 쿼리 사용
        List<UserVideoCategory> userVideos = userVideoCategoryRepository.findByCategoryIdWithVideo(categoryId);
        for (UserVideoCategory userVideo : userVideos) {
            // 해당 유저의 비디오만 추가
            if (userVideo.getUser().getId().equals(userId)) {
                dto.getVideos().add(new UserVideoInfoDto(userVideo));
            }
        }

        return dto;
    }

    /**
     * 카테고리에 비디오 추가
     */
    public void addVideoToCategory(Long userId, Long videoId, Long categoryId, String userVideoName) {
        // 엔티티 로드를 최소화
        Optional<UserVideoCategory> existingEntry = userVideoCategoryRepository
                .findByUserIdCategoryIdAndVideoId(userId, categoryId, videoId);

        if (existingEntry.isPresent()) {
            throw new IllegalStateException("이미 해당 비디오가 카테고리에 존재합니다.");
        }

        // 카테고리 접근 권한 확인
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다: " + categoryId));

        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("해당 카테고리에 접근할 권한이 없습니다.");
        }
        // 프록시 객체 사용으로 최적화
        User user = new User();
        user.setId(userId);

        Video video = new Video();
        video.setId(videoId);

        // 필요한 엔티티 참조만 설정
        UserVideoCategory userVideoCategory = new UserVideoCategory(
                user,video,category,userVideoName
        );
        userVideoCategoryRepository.save(userVideoCategory);
    }

    /**
     * 카테고리에서 비디오 제거
     */
    public void removeVideoFromCategory(Long userId, Long videoId, Long categoryId) {
        Optional<UserVideoCategory> entryToRemove = userVideoCategoryRepository
                .findByUserIdCategoryIdAndVideoId(userId, categoryId, videoId);

        if (entryToRemove.isEmpty()) {
            throw new EntityNotFoundException("해당 카테고리에 비디오가 존재하지 않습니다.");
        }

        userVideoCategoryRepository.delete(entryToRemove.get());
    }

}
