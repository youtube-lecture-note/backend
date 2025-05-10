package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.User;
import com.example.youtube_lecture_helper.security.CustomUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    @Query("select new com.example.youtube_lecture_helper.security.CustomUserDetails(u.id, u.email) from User u where u.email = :email")
    Optional<CustomUserDetails> findUserDetailsByEmail(@Param("email") String email);
}
