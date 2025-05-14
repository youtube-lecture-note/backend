package com.example.youtube_lecture_helper.service;

import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.repository.UserRepository;
import com.example.youtube_lecture_helper.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public CustomUserDetails loadUserByUsername(String email) {
        return userRepository.findUserDetailsByEmail(email)
                .orElse(null); // null 허용, 구글 로그인 시 신규 생성 위해
    }
    //email만 참고
    public User createUser(String email, String role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }
}
