package com.revisalab.repository;

import com.revisalab.model.ChatMessage;
import com.revisalab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Últimas 50 mensagens do usuário, da mais recente para a mais antiga. */
    List<ChatMessage> findTop50ByUserOrderByCreatedAtDesc(User user);

    /** Remove todo o histórico de conversa do usuário. */
    void deleteByUser(User user);
}
