package com.matheusdev.mindforge.study.quiz.dto;

import lombok.Data;

@Data
public class QuizQuestionResponse {
    private Long id;
    private String question;
    private String options;
    private Integer correctAnswer; // Only included for results
    private String explanation;
    private Integer orderIndex;
}
