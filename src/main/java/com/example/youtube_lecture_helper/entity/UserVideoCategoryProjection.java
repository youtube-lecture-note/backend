package com.example.youtube_lecture_helper.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

public interface UserVideoCategoryProjection {
    Long getCategoryId();
    Long getParentCategoryId();
    String getCategoryName();
}
