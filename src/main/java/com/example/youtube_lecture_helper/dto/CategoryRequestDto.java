package com.example.youtube_lecture_helper.dto;

import lombok.Data;

@Data
public class CategoryRequestDto {
    private String name;
    private Long parentId;  //optional
}
