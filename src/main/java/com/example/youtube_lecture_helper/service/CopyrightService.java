package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.dto.CopyrightCheckDTO;
import com.example.youtube_lecture_helper.entity.*;
import com.example.youtube_lecture_helper.repository.*;
import com.example.youtube_lecture_helper.exception.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CopyrightService {

    private final BanRepository banRepository;
    private final VideoRepository videoRepository;
    private final QuizRepository quizRepository;
    private final QuizSetRepository quizSetRepository;
    private final QuizSetMultiRepository quizSetMultiRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserVideoCategoryRepository userVideoCategoryRepository;

    public Optional<CopyrightCheckDTO> check(String youtubeId){
        Optional<Ban> result = banRepository.findByYoutubeId(youtubeId);
        if(result.isPresent()){
            CopyrightCheckDTO info = new CopyrightCheckDTO(result.get());
            return Optional.of(info);
        }else{
            return Optional.empty();
        }
    }

    public List<Ban> getAllBans(){
        return banRepository.findAll();
    }

    //밴하고 연관된 rows 모두 삭제
    @Transactional
    public String banVideo(String youtubeId, String owner) {
        // 이미 밴된 영상인지 확인
        if (banRepository.findByYoutubeId(youtubeId).isPresent()) {
            return "이미 차단된 영상입니다.";
        }

        // 밴 엔티티 생성
        Ban banned = new Ban(youtubeId, owner);

        // 1. video 조회 (없어도 예외 던지지 않음)
        Optional<Video> videoOpt = videoRepository.findByYoutubeId(youtubeId);

        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();
            Long videoId = video.getId();

            // 2. quiz 조회 (youtubeId로)
            List<Quiz> quizzes = quizRepository.findAllByYoutubeId(youtubeId);

            for (Quiz quiz : quizzes) {
                // 3. quiz_set 찾기 (quiz_id로)
                List<QuizSet> quizSets = quizSetRepository.findAllByQuizId(quiz.getId());

                for (QuizSet quizSet : quizSets) {
                    if (!quizSet.isMultiVideo()) {
                        // 4. quiz_attempt 삭제
                        quizAttemptRepository.deleteByQuizSetId(quizSet.getId());
                        // 5. quiz_set_multi 삭제
                        quizSetMultiRepository.deleteByQuizSetId(quizSet.getId());
                        // 6. quiz_set 삭제
                        quizSetRepository.delete(quizSet);
                    } else {
                        // multi_video인 경우, quiz_attempt/quiz_set_multi에서 해당 quiz만 삭제
                        quizAttemptRepository.deleteByQuizIdAndQuizSetId(quiz.getId(), quizSet.getId());
                        quizSetMultiRepository.deleteByQuizIdAndQuizSetId(quiz.getId(), quizSet.getId());
                    }
                }
                quizAttemptRepository.deleteByQuizId(quiz.getId());
                // 7. quiz 삭제
                quizRepository.delete(quiz);
            }

            // 8. user_video_category 삭제
            userVideoCategoryRepository.deleteByVideoId(videoId);

            // 9. video 삭제
            videoRepository.delete(video);
        } else {
            // video가 없는 경우에도 quiz, user_video_category 등 삭제 시도 (youtubeId 기준)
            List<Quiz> quizzes = quizRepository.findAllByYoutubeId(youtubeId);

            for (Quiz quiz : quizzes) {
                List<QuizSet> quizSets = quizSetRepository.findAllByQuizId(quiz.getId());
                for (QuizSet quizSet : quizSets) {
                    if (!quizSet.isMultiVideo()) {
                        quizAttemptRepository.deleteByQuizSetId(quizSet.getId());
                        quizSetMultiRepository.deleteByQuizSetId(quizSet.getId());
                        quizSetRepository.delete(quizSet);
                    } else {
                        quizAttemptRepository.deleteByQuizIdAndQuizSetId(quiz.getId(), quizSet.getId());
                        quizSetMultiRepository.deleteByQuizIdAndQuizSetId(quiz.getId(), quizSet.getId());
                    }
                }
                quizAttemptRepository.deleteByQuizId(quiz.getId());
                quizRepository.delete(quiz);
            }
            // videoId를 알 수 없으므로 userVideoCategoryRepository.deleteByVideoId()는 생략
            // videoRepository.delete()도 생략
        }

        try {
            return banRepository.save(banned).getYoutubeId();
        } catch (Exception e) {
            return "처리 중 오류 발생. 다시 시도해주세요.";
        }
    }

}
