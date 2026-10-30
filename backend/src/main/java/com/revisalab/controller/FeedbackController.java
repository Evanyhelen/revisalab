package com.revisalab.controller;

import com.revisalab.model.Feedback;
import com.revisalab.model.User;
import com.revisalab.repository.FeedbackRepository;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody FeedbackRequest request, Authentication authentication) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "A avaliação (1 a 5) é obrigatória."));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Feedback feedback = Feedback.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .user(user)
                .build();
        feedbackRepository.save(feedback);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Feedback recebido. Obrigado por contribuir!");
        response.put("id", feedback.getId());
        return ResponseEntity.ok(response);
    }

    public static class FeedbackRequest {
        private Integer rating;
        private String comment;

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
