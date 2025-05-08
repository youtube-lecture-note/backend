package com.example.youtube_lecture_helper.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class UserVideoCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "user_video_name")
    private String userVideoName;  // 유저가 지정한 비디오 이름
    public UserVideoCategory(){}
    public UserVideoCategory(User user, Video video, Category category, String userVideoName){

    }
}
