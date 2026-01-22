package com.matheusdev.mindforge.study.quiz.dto;

import lombok.Data;

@Data
public class QuizQuestionRequest {
    private String question;
    private String options; // JSON: ["A", "B", "C", "D"]
    private Integer correctAnswer;
    private String explanation;
}
