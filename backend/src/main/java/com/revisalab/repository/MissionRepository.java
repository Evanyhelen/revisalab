package com.revisalab.repository;

import com.revisalab.model.Mission;
import com.revisalab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {
    Optional<Mission> findByCode(String code);
    List<Mission> findByActiveTrue();
    List<Mission> findByType(String type);

    /** Missões globais ativas (compartilhadas por todos os usuários). */
    @Query("SELECT m FROM Mission m WHERE m.active = true AND (m.isPersonal = false OR m.isPersonal IS NULL) ORDER BY m.id")
    List<Mission> findGlobalActiveMissions();

    /** Missões pessoais ativas do usuário. */
    @Query("SELECT m FROM Mission m WHERE m.user = :user AND m.isPersonal = true AND m.active = true ORDER BY m.id")
    List<Mission> findPersonalActiveMissions(@Param("user") User user);

    /** Missão pessoal do usuário (para edição/exclusão). */
    Optional<Mission> findByIdAndUserAndIsPersonalTrue(Long id, User user);
}