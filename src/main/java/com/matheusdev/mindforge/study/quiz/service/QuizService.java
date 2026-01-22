package com.matheusdev.mindforge.study.quiz.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizResponse;
import com.matheusdev.mindforge.study.quiz.mapper.QuizMapper;
import com.matheusdev.mindforge.study.quiz.model.Quiz;
import com.matheusdev.mindforge.study.quiz.model.QuizQuestion;
import com.matheusdev.mindforge.study.quiz.repository.QuizRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final SubjectRepository subjectRepository;
    private final QuizMapper mapper;

    public List<QuizResponse> getQuizzesBySubject(Long subjectId) {
        return quizRepository.findBySubjectId(subjectId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public QuizResponse getQuizById(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com o id: " + quizId));
        return mapper.toResponse(quiz);
    }

    @Transactional
    public QuizResponse createQuiz(Long subjectId, QuizRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada com o id: " + subjectId));

        Quiz quiz = mapper.toEntity(request);
        quiz.setSubject(subject);

        // Add questions if provided
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int index = 0;
            for (QuizQuestionRequest questionReq : request.getQuestions()) {
                QuizQuestion question = mapper.toEntity(questionReq);
                question.setQuiz(quiz);
                question.setOrderIndex(index++);
                quiz.getQuestions().add(question);
            }
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        return mapper.toResponse(savedQuiz);
    }

    public void deleteQuiz(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz não encontrado com o id: " + quizId);
        }
        quizRepository.deleteById(quizId);
    }
}
