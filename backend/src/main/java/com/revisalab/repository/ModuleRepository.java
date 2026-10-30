package com.revisalab.repository;

import com.revisalab.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para Módulo
 * Acesso aos dados de módulos do banco de dados
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    /**
     * Encontra módulos por ID de trilha, ordenados por número de ordem
     * @param trailId ID da trilha
     * @return Lista de módulos ordenados
     */
    List<Module> findByTrailIdOrderByOrderNumber(Long trailId);

    /**
     * Encontra módulos por ID de trilha
     * @param trailId ID da trilha
     * @return Lista de módulos
     */
    List<Module> findByTrailId(Long trailId);
}
