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
    private final com.matheusdev.mindforge.ai.service.WebSearchService webSearchService;
    private final com.matheusdev.mindforge.ai.service.AIOrchestrationService aiOrchestrationService;
    private final com.matheusdev.mindforge.study.note.repository.StudyNoteRepository studyNoteRepository;

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
                try {
                    QuizQuestion question = mapper.toEntity(questionReq);
                    question.setQuiz(quiz);
                    question.setOrderIndex(index++);
                    quiz.getQuestions().add(question);
                } catch (Exception e) {
                    throw new RuntimeException("Erro ao processar opções da questão", e);
                }
            }
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        return mapper.toResponse(savedQuiz);
    }

    @Transactional
    public QuizResponse generateQuiz(Long subjectId, String topic, String difficulty, int count) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada com o id: " + subjectId));

        // 1. Gather Context
        StringBuilder contextBuilder = new StringBuilder();

        // Subject Description
        if (subject.getDescription() != null) {
            contextBuilder.append("Subject Description: ").append(subject.getDescription()).append("\n\n");
        }

        // Local Notes (Limit to last 5 notes or specific topic if possible, for now
        // fetching recent ones)
        // Improved: Fetch notes that might be relevant? For now, we dump the last few
        // notes content.
        List<com.matheusdev.mindforge.study.note.model.Note> notes = studyNoteRepository
                .findBySubjectIdOrderByUpdatedAtDesc(subjectId);
        contextBuilder.append("--- NOTAS DO USUÁRIO ---\n");
        notes.stream().limit(5).forEach(note -> {
            contextBuilder.append("Nota: ").append(note.getTitle()).append("\n");
            // Truncate simplistic content to avoid token overflow
            String content = note.getContent();
            if (content != null) {
                if (content.length() > 2000)
                    content = content.substring(0, 2000) + "...";
                contextBuilder.append(content).append("\n\n");
            }
        });

        // Web Search (Tavily)
        if (topic != null && !topic.isBlank()) {
            List<String> webResults = webSearchService
                    .search(topic + " " + subject.getName() + " concepts quiz questions");
            contextBuilder.append("--- CONTEÚDO DA WEB ---\n");
            webResults.forEach(res -> contextBuilder.append(res).append("\n\n"));
        }

        // 2. Generate Questions via AI
        List<QuizQuestionRequest> generatedQuestions = aiOrchestrationService.generateQuizQuestions(
                contextBuilder.toString(),
                topic != null ? topic : subject.getName(),
                difficulty,
                count).join(); // Sync wait for simplicity in this REST call

        if (generatedQuestions.isEmpty()) {
            throw new RuntimeException("Falha ao gerar questões via IA. Tente novamente.");
        }

        // 3. Create Quiz Entity
        QuizRequest quizRequest = new QuizRequest();
        quizRequest.setTitle("Quiz IA: " + (topic != null ? topic : subject.getName()));
        quizRequest.setDescription("Gerado automaticamente por IA com base em notas e web.");
        try {
            quizRequest.setDifficulty(Quiz.DifficultyLevel.valueOf(difficulty.toUpperCase()));
        } catch (Exception e) {
            quizRequest.setDifficulty(Quiz.DifficultyLevel.MEDIUM);
        }
        quizRequest.setQuestions(generatedQuestions);

        return createQuiz(subjectId, quizRequest);
    }

    public void deleteQuiz(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz não encontrado com o id: " + quizId);
        }
        quizRepository.deleteById(quizId);
    }
}
