package com.revisalab.repository;

import com.revisalab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para Usuário
 * Acesso aos dados de usuários do banco de dados
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Encontra usuário por email
     * @param email Email do usuário
     * @return Usuário encontrado
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se email já existe no banco
     * @param email Email a verificar
     * @return true se existe, false caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Usuários visíveis no ranking/painéis: exclui contas de teste automatizado.
     * Inclui dados de demonstração e usuários reais.
     */
    @Query("SELECT u FROM User u WHERE u.isTest = false OR u.isTest IS NULL ORDER BY u.id")
    List<User> findAllForRanking();
}
