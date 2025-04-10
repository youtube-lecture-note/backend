package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
}
