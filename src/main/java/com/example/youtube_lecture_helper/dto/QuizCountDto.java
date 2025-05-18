package com.example.youtube_lecture_helper.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QuizCountDto {
    private Long level1Count;  // 난이도 1 개수
    private Long level2Count;  // 난이도 2 개수
    private Long level3Count;  // 난이도 3 개수
    private Long totalCount;   // 전체 개수

    public QuizCountDto() {
        this.level1Count = 0L;
        this.level2Count = 0L;
        this.level3Count = 0L;
        this.totalCount = 0L;
    }

    public QuizCountDto(Long level1Count, Long level2Count, Long level3Count) {
        this.level1Count = level1Count != null ? level1Count : 0L;
        this.level2Count = level2Count != null ? level2Count : 0L;
        this.level3Count = level3Count != null ? level3Count : 0L;
        this.totalCount = this.level1Count + this.level2Count + this.level3Count;
    }

}
