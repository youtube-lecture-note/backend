package com.example.youtube_lecture_helper.dto;

import com.example.youtube_lecture_helper.entity.Ban;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class CopyrightCheckDTO {
    private String owner;
    private String processedDate;

    public CopyrightCheckDTO (Ban copyright){
        this.owner = copyright.getOwner();
        this.processedDate = copyright.getProcessedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
