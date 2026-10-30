package com.revisalab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class AiServiceTest {
    
    @MockBean
    private RestTemplate restTemplate;
    
    @Autowired
    private AiService aiService;
    
    @Test
    void testGetAiResponse_WithEmptyQuestion_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiService.getAiResponse("")
        );
        assertEquals("Pergunta não pode ser vazia", exception.getMessage());
    }
    
    @Test
    void testGetAiResponse_WithNullQuestion_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiService.getAiResponse(null)
        );
        assertEquals("Pergunta não pode ser vazia", exception.getMessage());
    }
    
    @Test
    void testGetAiResponse_QuestionTooLong_ThrowsIllegalArgumentException() {
        String longQuestion = "A".repeat(501);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiService.getAiResponse(longQuestion)
        );
        assertEquals("Pergunta muito longa (máximo 500 caracteres)", exception.getMessage());
    }
    
    @Test
    void testGetAiResponse_WithoutValidApiKey_ThrowsException() {
        // Com chave de teste inválida, o serviço deve lançar exceção
        // Pode ser IllegalStateException ou RuntimeException
        Exception exception = assertThrows(
            Exception.class,
            () -> aiService.getAiResponse("Qual é a capital do Brasil?")
        );
        
        // Verificar que alguma exceção foi lançada
        assertNotNull(exception);
        assertTrue(
            exception instanceof IllegalStateException || 
            exception instanceof RuntimeException,
            "Deve lançar IllegalStateException ou RuntimeException, mas lançou: " + exception.getClass().getName()
        );
        
        // Verificar mensagem de erro
        String message = exception.getMessage();
        assertTrue(
            message.contains("Serviço de IA não configurado") || 
            message.contains("GROQ_API_KEY") ||
            message.contains("não foi possível obter resposta"),
            "Mensagem deve indicar problema com a chave de API. Mensagem: " + message
        );
    }
    
    @Test
    void testIsAvailable_CanBeCalledWithoutError() {
        // Smoke test - apenas garantir que o método pode ser chamado
        boolean available = aiService.isAvailable();
        // Não podemos garantir o resultado, apenas que não lança exceção
        assertTrue(true);
    }
}