package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.dto.UserVideoInfoDto;
import com.example.youtube_lecture_helper.entity.UserVideoCategory;
import com.example.youtube_lecture_helper.entity.UserVideoCategoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserVideoCategoryRepository extends JpaRepository<UserVideoCategory, Long> {
    List<UserVideoCategory> findByCategoryId(Long categoryId);
    @Query("SELECT " +
            "v.id as videoId, " +
            "uvc.userVideoName as userVideoName " +
            "FROM UserVideoCategory uvc " +
            "JOIN uvc.video v " +
            "WHERE uvc.category.id = :categoryId " +
            "AND uvc.user.id = :userId")
    List<UserVideoInfoDto> findVideosByCategoryIdAndUserId(Long categoryId, Long userId);

    @Query("SELECT uvc FROM UserVideoCategory uvc " +
            "WHERE uvc.user.id = :userId " +
            "AND uvc.category.id = :categoryId " +
            "AND uvc.video.id = :videoId")
    Optional<UserVideoCategory> findByUserIdCategoryIdAndVideoId(Long userId, Long categoryId, Long videoId);

    @Query( "SELECT uvc " +
            "FROM UserVideoCategory uvc " +
            "JOIN FETCH uvc.video " +
            "WHERE uvc.category.id = :categoryId")
    List<UserVideoCategory> findByCategoryIdWithVideo(Long categoryId);

    @Query("SELECT uvc FROM UserVideoCategory uvc " +
            "JOIN FETCH uvc.video " +
            "WHERE uvc.category.id IN :categoryIds and uvc.visible = true")
    List<UserVideoCategory> findByCategoryIdsWithVideo(List<Long> categoryIds);

    List<UserVideoCategory> findByUserIdAndCategoryId(Long userId, Long categoryId);

    Optional<UserVideoCategory> findByUserIdAndVideoId(Long userId, Long videoId);

    void deleteByVideoId(Long videoId);

    @Query("SELECT COUNT(DISTINCT uvc.video.id) FROM UserVideoCategory uvc WHERE uvc.user.id = :userId")
    Integer countStudiedVideosByUserId(@Param("userId") Long userId);

    Optional<UserVideoCategory> findByVideoIdAndUserId(Long videoId, Long userId);
    
    boolean existsByCategoryIdAndVisibleIsTrue(Long categoryId);
}
