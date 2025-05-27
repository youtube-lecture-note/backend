package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.CopyrightCheckDTO;
import com.example.youtube_lecture_helper.entity.Ban;
import com.example.youtube_lecture_helper.repository.BanRepository;
import com.example.youtube_lecture_helper.repository.QuizRepository;
import com.example.youtube_lecture_helper.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CopyrightService {

    private final BanRepository banRepository;
    private final VideoRepository videoRepository;  //잠시 보류.
    private final QuizRepository quizRepository;

    public Optional<CopyrightCheckDTO> check(String youtubeId){
        Optional<Ban> result = banRepository.findByYoutubeId(youtubeId);
        if(result.isPresent()){
            CopyrightCheckDTO info = new CopyrightCheckDTO(result.get());
            return Optional.of(info);
        }else{
            return Optional.empty();
        }
    }

    @Transactional
    public String banVideo(String videoId,String owner){
        Ban banned = new Ban(videoId,owner);
        Optional<Ban> check = banRepository.findByYoutubeId(videoId);
        if(!check.isEmpty()){
            return "이미 차단된 영상입니다.";
        } else{
            try{
                return banRepository.save(banned).getYoutubeId();
            } catch(Exception e){
                return "처리 중 오류 발생. 다시 시도해주세요.";
            }
        }
    }
}
