package com.example.youtube_lecture_helper.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import lombok.Getter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ParticipantResultDto {
    private String userName;
    private String userEmail;
    private int correctCount;
    
    // JPQL 생성자용 (totalQuizCount 제거)
    public ParticipantResultDto(String userName, String userEmail, Long correctCount) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.correctCount = correctCount.intValue();
    }
}
