package com.matheusdev.mindforge.study.quiz.dto;

import com.matheusdev.mindforge.study.quiz.model.Quiz;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizResponse {
    private Long id;
    private Long subjectId;
    private String title;
    private String description;
    private Quiz.DifficultyLevel difficulty;
    private List<QuizQuestionResponse> questions;
    private LocalDateTime createdAt;
    private Integer questionCount;
}
