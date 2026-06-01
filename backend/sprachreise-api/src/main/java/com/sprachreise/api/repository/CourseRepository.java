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

    @Query("SELECT c FROM Course c WHERE c.trainer.id = :trainerId AND c.status = 'PUBLISHED' ORDER BY c.createdAt DESC")
    List<Course> findByTrainerIdPublished(@Param("trainerId") Long trainerId);

    @Query("SELECT c FROM Course c WHERE c.trainer.id = :trainerId AND c.status = :status ORDER BY c.createdAt DESC")
    List<Course> findByTrainerIdAndStatus(@Param("trainerId") Long trainerId, @Param("status") Course.CourseStatus status);

    @Query("SELECT c FROM Course c WHERE c.trainer.id = :trainerId ORDER BY c.createdAt DESC")
    List<Course> findAllByTrainerId(@Param("trainerId") Long trainerId);
}
