package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.UserVideoCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVideoInfoDto {
    private Long videoId;
    private String userVideoName;

    public UserVideoInfoDto(UserVideoCategory userVideoCategory) {
        this.videoId = userVideoCategory.getVideo().getId();
        this.userVideoName = userVideoCategory.getUserVideoName();
    }
}
