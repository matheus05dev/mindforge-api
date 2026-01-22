package com.matheusdev.mindforge.study.quiz.api;

import com.matheusdev.mindforge.study.quiz.dto.QuizRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizResponse;
import com.matheusdev.mindforge.study.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyQuizRestController {

    private final QuizService quizService;

    @GetMapping("/subjects/{subjectId}/quizzes")
    public ResponseEntity<List<QuizResponse>> getQuizzesBySubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(quizService.getQuizzesBySubject(subjectId));
    }

    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizResponse> getQuizById(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuizById(quizId));
    }

    @PostMapping("/subjects/{subjectId}/quizzes")
    public ResponseEntity<QuizResponse> createQuiz(
            @PathVariable Long subjectId,
            @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.createQuiz(subjectId, request));
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }
}
