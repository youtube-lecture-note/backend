package com.example.youtube_lecture_helper.controller;

import com.example.youtube_lecture_helper.openai_api.OpenAIGptClient;
import com.example.youtube_lecture_helper.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class SummaryController {
    //        //SzrVaYDLiHs : 빅뱅
//        //yHzJikY3LBo : 수소폭탄
//        //nJ-R9G1pepU : 사물궁이
//        //Bh6WtpsStpM : 중학교 2학년 과학 (17분)
//        //veTpPfu1-o8 : 뱀(58분)
//        //vLaFAKnaRJU : 영어강의
    private final SummaryService summaryService;
    public SummaryController(SummaryService summaryService){
        this.summaryService = summaryService;
    }

    @GetMapping(value = "/api/summary", produces = "application/json")
    //ApiResponse<String>
    public ResponseEntity<ApiResponse<String>> getSummary(@RequestParam String videoId) {
        String summary = summaryService.getSummary(videoId);
        if(summary == null){
            return ApiResponse.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,"cannot create summary","");
        }
        if(summary.equals("-1")){
            return ApiResponse.buildResponse(HttpStatus.BAD_REQUEST, "강의 영상 아님", "");
        }
        System.out.println("summary: " + summary);
        return ApiResponse.buildResponse(HttpStatus.OK, "성공", summary);
    }
}
