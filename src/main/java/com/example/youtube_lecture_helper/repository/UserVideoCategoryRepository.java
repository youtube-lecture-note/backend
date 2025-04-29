package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.UserVideoCategory;
import com.example.youtube_lecture_helper.entity.UserVideoCategoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserVideoCategoryRepository extends JpaRepository<UserVideoCategory, Long> {
    @Query("SELECT uvc.user.id AS userId, uvc.userVideoName AS userVideoName, c.name AS categoryName " +
            "FROM UserVideoCategory uvc " +
            "JOIN uvc.category c " +
            "WHERE uvc.user.id = :userId")
        //videoName은 user_video_name을 이용하므로 join 필요없음.
    List<UserVideoCategoryProjection> findUserCategoryByUserId(@Param("userId") Long userId);
}
