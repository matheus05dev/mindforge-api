package com.matheusdev.mindforge.study.quiz.dto;

import lombok.Data;

@Data
public class QuizQuestionRequest {
    private String question;
    private java.util.List<String> options; // JSON Array
    private Integer correctAnswer;
    private String explanation;
}
