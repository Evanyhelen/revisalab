package com.revisalab.repository;

import com.revisalab.model.Trail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrailRepository extends JpaRepository<Trail, Long> {
    List<Trail> findByCategory(String category);
    List<Trail> findByDifficulty(Integer difficulty);
}