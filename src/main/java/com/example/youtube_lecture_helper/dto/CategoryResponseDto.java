package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryResponseDto {
    private Long id;
    private String name;
    private Long parentId;
    private List<CategoryResponseDto> children;
    private List<UserVideoInfoDto> videos;

    public CategoryResponseDto(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.parentId = category.getParent() != null ? category.getParent().getId() : null;
        this.children = new ArrayList<>();
        this.videos = new ArrayList<>();
    }
    public CategoryResponseDto(){}
}
