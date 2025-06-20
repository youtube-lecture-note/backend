package com.example.youtube_lecture_helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class YoutubeLectureHelperApplication {

	public static void main(String[] args) {
		SpringApplication.run(YoutubeLectureHelperApplication.class, args);
	}

}
