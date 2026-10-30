package com.revisalab.repository;

import com.revisalab.model.Exercise;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para Exercício.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /**
     * Encontra exercícios por ID de trilha (via módulo -> trilha).
     */
    @Query("SELECT e FROM Exercise e JOIN e.module m JOIN m.trail t WHERE t.id = ?1")
    List<Exercise> findByModuleTrailId(Long trailId);

    /**
     * Encontra exercícios por categoria da trilha (via módulo -> trilha).
     */
    @Query("SELECT e FROM Exercise e JOIN e.module m JOIN m.trail t WHERE t.category = ?1")
    List<Exercise> findByCategory(String category, Pageable pageable);

    /**
     * Encontra exercícios aleatórios.
     */
    @Query(value = "SELECT * FROM exercises ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<Exercise> findRandom(int count);
}
