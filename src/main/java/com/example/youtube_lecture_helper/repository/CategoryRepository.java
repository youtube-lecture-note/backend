package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findByParentIdIsNull();
    List<Category> findByParentId(Category parentCategory);
    List<Category> findByUserId(Long userId);
}
