package com.example.youtube_lecture_helper.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Ban {
    @Id
    private String youtubeId;
    private String owner;   //저작권 보유자
    private LocalDateTime processedDate;    //신고 후 금지 영상 처리 일자


    public Ban(String youtubeId, String owner){
        this.youtubeId = youtubeId;
        this.owner = owner;
    }

    @PrePersist
    public void prePersist(){
        this.processedDate = LocalDateTime.now();
    }
}
