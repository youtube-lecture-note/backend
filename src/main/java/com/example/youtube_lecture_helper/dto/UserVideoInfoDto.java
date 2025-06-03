package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.UserVideoCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVideoInfoDto {
    private String videoId;
    private String userVideoName;

    public UserVideoInfoDto(UserVideoCategory userVideoCategory) {
        this.videoId = userVideoCategory.getVideo().getYoutubeId();
        this.userVideoName = userVideoCategory.getUserVideoName();
    }
}
