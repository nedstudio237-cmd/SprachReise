package com.sprachreise.api.repository;

import com.sprachreise.api.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' ORDER BY c.levelId, c.createdAt")
    List<Course> findAllPublished();

    @Query("SELECT c FROM Course c WHERE c.levelId = :levelId AND c.status = 'PUBLISHED' ORDER BY c.createdAt")
    List<Course> findByLevelIdPublished(@Param("levelId") Long levelId);
}
