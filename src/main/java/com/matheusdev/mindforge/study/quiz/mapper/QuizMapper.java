package com.matheusdev.mindforge.study.quiz.mapper;

import com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizQuestionResponse;
import com.matheusdev.mindforge.study.quiz.dto.QuizRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizResponse;
import com.matheusdev.mindforge.study.quiz.model.Quiz;
import com.matheusdev.mindforge.study.quiz.model.QuizQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuizMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Quiz toEntity(QuizRequest request);

    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(target = "questionCount", expression = "java(quiz.getQuestions() != null ? quiz.getQuestions().size() : 0)")
    QuizResponse toResponse(Quiz quiz);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    QuizQuestion toEntity(QuizQuestionRequest request);

    QuizQuestionResponse toResponse(QuizQuestion question);
}
