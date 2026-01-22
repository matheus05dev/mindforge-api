package com.matheusdev.mindforge.study.quiz.dto;

import com.matheusdev.mindforge.study.quiz.model.Quiz;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuizRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Quiz.DifficultyLevel difficulty;

    private List<QuizQuestionRequest> questions;
}
