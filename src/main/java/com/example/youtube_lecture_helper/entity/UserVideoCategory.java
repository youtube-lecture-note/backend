package com.example.youtube_lecture_helper.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter @Setter
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
    @JoinColumn(name = "category_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL) // 이 줄 추가
    private Category category;
    private boolean visible;

    @Column(name = "user_video_name")
    private String userVideoName;  // 유저가 지정한 비디오 이름
    public UserVideoCategory(){}
    public UserVideoCategory(User user, Video video, Category category, String userVideoName){
        this.user = user;
        this.video = video;
        this.category = category;
        this.userVideoName = userVideoName;
    }
}
