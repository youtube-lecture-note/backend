package com.example.youtube_lecture_helper.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String youtubeId ;
    
    //긴 데이터 처리
    @Lob
    @Column(columnDefinition = "TEXT")  
    private String summary;
    public Video(String youtubeId, String summary){
        this.youtubeId = youtubeId;
        this.summary = summary;
    }
}
