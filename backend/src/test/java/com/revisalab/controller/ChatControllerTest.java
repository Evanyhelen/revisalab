package com.revisalab.controller;

import com.revisalab.dto.request.ChatRequest;
import com.revisalab.dto.response.ChatResponse;
import com.revisalab.model.User;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ChatController chatController;

    @Test
    void testSendMessage_ValidQuestion_Success() {
        // Setup
        String testQuestion = "Como fazer uma boa redação?";
        ChatResponse chatResponse = ChatResponse.builder()
                .question(testQuestion)
                .answer("Para uma boa redação, comece com uma introdução clara...")
                .xpEarned(5)
                .userXp(105)
                .userLevel(1)
                .success(true)
                .responseTime(150L)
                .timestamp(System.currentTimeMillis())
                .build();

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(1L);

        User mockUser = new User();
        mockUser.setId(1L);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(chatService.askQuestion(mockUser, testQuestion, "general")).thenReturn(chatResponse);

        // Execute
        ResponseEntity<?> response = chatController.sendMessage(
                new ChatRequest(testQuestion),
                authentication
        );

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ChatResponse responseBody = (ChatResponse) response.getBody();
        assertTrue(responseBody.isSuccess());
        assertEquals(testQuestion, responseBody.getQuestion());
        assertEquals(chatResponse.getAnswer(), responseBody.getAnswer());
        assertEquals(5, responseBody.getXpEarned());
        assertEquals(Integer.valueOf(105), responseBody.getUserXp());
        assertEquals(Integer.valueOf(1), responseBody.getUserLevel());
        assertTrue(responseBody.getResponseTime() >= 0);
        assertTrue(responseBody.getTimestamp() > 0);

        verify(chatService).askQuestion(mockUser, testQuestion, "general");
    }

    @Test
    void testSendMessage_EmptyQuestion_BadRequest() {
        // Execute
        ResponseEntity<?> response = chatController.sendMessage(
                new ChatRequest(""),
                authentication
        );

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Pergunta não pode ser vazia", responseBody.get("error"));
    }

    @Test
    void testSendMessage_NullQuestion_BadRequest() {
        // Execute
        ResponseEntity<?> response = chatController.sendMessage(
                new ChatRequest(null),
                authentication
        );

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Pergunta não pode ser vazia", responseBody.get("error"));
    }

    @Test
    void testSendMessage_QuestionTooLong_BadRequest() {
        // Setup
        String longQuestion = "A".repeat(501);

        // Execute
        ResponseEntity<?> response = chatController.sendMessage(
                new ChatRequest(longQuestion),
                authentication
        );

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Pergunta muito longa (máximo 500 caracteres)", responseBody.get("error"));
    }

    @Test
    void testSendMessage_AiServiceThrowsException_ServiceUnavailable() {
        // Setup
        String testQuestion = "Como fazer uma boa redação?";
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(1L);

        User mockUser = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(chatService.askQuestion(mockUser, testQuestion, "general"))
                .thenThrow(new IllegalStateException("Serviço de IA não configurado"));

        // Execute
        ResponseEntity<?> response = chatController.sendMessage(
                new ChatRequest(testQuestion),
                authentication
        );

        // Verify
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Serviço de IA temporariamente indisponível", responseBody.get("error"));
    }

    @Test
    void testHealth_ReturnsOK() {
        // Execute
        ResponseEntity<?> response = chatController.health();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("OK", responseBody.get("status"));
        assertEquals("Chat service is running", responseBody.get("message"));
        assertNotNull(responseBody.get("timestamp"));
    }
}
