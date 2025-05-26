package com.example.youtube_lecture_helper.repository;

import com.example.youtube_lecture_helper.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findByParentIdIsNull();
    List<Category> findByParentId(Category parentCategory);
    List<Category> findByUserId(Long userId);
    @Query("select c.user.id from Category c where c.id = :id")
    Optional<Long> findOwnerIdById(@Param("id") Long id);
    void deleteById(Long id);
}
