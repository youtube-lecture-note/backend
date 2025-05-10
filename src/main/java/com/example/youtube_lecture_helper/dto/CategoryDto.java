package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryDto {
    private Long id;
    private String name;
    private Long parentId;
    private List<CategoryDto> children;
    private List<UserVideoInfoDto> videos;

    public CategoryDto(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.parentId = category.getParentId() != null ? category.getParentId().getId() : null;
        this.children = new ArrayList<>();
        this.videos = new ArrayList<>();
    }
}
