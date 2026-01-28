package com.matheusdev.mindforge.study.quiz.service;

import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.ai.service.WebSearchService;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.note.model.Note;
import com.matheusdev.mindforge.study.note.repository.StudyNoteRepository;
import com.matheusdev.mindforge.study.quiz.dto.QuizQuestionRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizRequest;
import com.matheusdev.mindforge.study.quiz.dto.QuizResponse;
import com.matheusdev.mindforge.study.quiz.mapper.QuizMapper;
import com.matheusdev.mindforge.study.quiz.model.Quiz;
import com.matheusdev.mindforge.study.quiz.model.QuizQuestion;
import com.matheusdev.mindforge.study.quiz.repository.QuizRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private QuizMapper mapper;

    @Mock
    private WebSearchService webSearchService;

    @Mock
    private AIOrchestrationService aiOrchestrationService;

    @Mock
    private StudyNoteRepository studyNoteRepository;

    @InjectMocks
    private QuizService quizService;

    private Subject testSubject;
    private Quiz testQuiz;
    private QuizResponse testQuizResponse;
    private static final Long SUBJECT_ID = 1L;
    private static final Long QUIZ_ID = 1L;

    @BeforeEach
    void setUp() {
        testSubject = new Subject();
        testSubject.setId(SUBJECT_ID);
        testSubject.setName("Mathematics");
        testSubject.setDescription("Advanced Mathematics");

        testQuiz = new Quiz();
        testQuiz.setId(QUIZ_ID);
        testQuiz.setTitle("Math Quiz");
        testQuiz.setDescription("Test your math skills");
        testQuiz.setSubject(testSubject);
        testQuiz.setQuestions(new ArrayList<>());

        testQuizResponse = new QuizResponse();
        testQuizResponse.setId(QUIZ_ID);
        testQuizResponse.setTitle("Math Quiz");
        testQuizResponse.setDescription("Test your math skills");
    }

    @Test
    @DisplayName("Should return quizzes by subject")
    void getQuizzesBySubject_ShouldReturnQuizzes() {
        // Arrange
        when(quizRepository.findBySubjectId(SUBJECT_ID)).thenReturn(Arrays.asList(testQuiz));
        when(mapper.toResponse(testQuiz)).thenReturn(testQuizResponse);

        // Act
        List<QuizResponse> result = quizService.getQuizzesBySubject(SUBJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Math Quiz", result.get(0).getTitle());
        verify(quizRepository).findBySubjectId(SUBJECT_ID);
    }

    @Test
    @DisplayName("Should return quiz by ID when it exists")
    void getQuizById_ShouldReturnQuiz_WhenQuizExists() {
        // Arrange
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(testQuiz));
        when(mapper.toResponse(testQuiz)).thenReturn(testQuizResponse);

        // Act
        QuizResponse result = quizService.getQuizById(QUIZ_ID);

        // Assert
        assertNotNull(result);
        assertEquals(QUIZ_ID, result.getId());
        assertEquals("Math Quiz", result.getTitle());
        verify(quizRepository).findById(QUIZ_ID);
    }

    @Test
    @DisplayName("Should throw exception when quiz not found")
    void getQuizById_ShouldThrowException_WhenQuizNotFound() {
        // Arrange
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> quizService.getQuizById(QUIZ_ID));
        assertTrue(exception.getMessage().contains("Quiz não encontrado"));
        verify(quizRepository).findById(QUIZ_ID);
    }

    @Test
    @DisplayName("Should create quiz with questions")
    void createQuiz_ShouldCreateQuizWithQuestions() throws Exception {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setTitle("New Quiz");
        request.setDescription("New Description");
        request.setDifficulty(Quiz.DifficultyLevel.MEDIUM);

        QuizQuestionRequest questionRequest = new QuizQuestionRequest();
        questionRequest.setQuestion("What is 2+2?");
        questionRequest.setCorrectAnswer(0); // Index of correct answer
        request.setQuestions(Arrays.asList(questionRequest));

        Quiz newQuiz = new Quiz();
        newQuiz.setTitle("New Quiz");
        newQuiz.setQuestions(new ArrayList<>());

        QuizQuestion question = new QuizQuestion();
        question.setQuestion("What is 2+2?");

        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(testSubject));
        when(mapper.toEntity(request)).thenReturn(newQuiz);
        when(mapper.toEntity(questionRequest)).thenReturn(question);
        when(quizRepository.save(any(Quiz.class))).thenReturn(newQuiz);
        when(mapper.toResponse(newQuiz)).thenReturn(testQuizResponse);

        // Act
        QuizResponse result = quizService.createQuiz(SUBJECT_ID, request);

        // Assert
        assertNotNull(result);
        verify(subjectRepository).findById(SUBJECT_ID);
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should throw exception when creating quiz for non-existent subject")
    void createQuiz_ShouldThrowException_WhenSubjectNotFound() {
        // Arrange
        QuizRequest request = new QuizRequest();
        request.setTitle("New Quiz");

        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> quizService.createQuiz(SUBJECT_ID, request));
        assertTrue(exception.getMessage().contains("Matéria não encontrada"));
        verify(subjectRepository).findById(SUBJECT_ID);
        verify(quizRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate quiz using AI")
    void generateQuiz_ShouldGenerateQuizWithAI() throws Exception {
        // Arrange
        String topic = "Algebra";
        String difficulty = "MEDIUM";
        int count = 5;

        Note testNote = new Note();
        testNote.setTitle("Algebra Notes");
        testNote.setContent("Basic algebra concepts");

        QuizQuestionRequest generatedQuestion = new QuizQuestionRequest();
        generatedQuestion.setQuestion("Solve x + 2 = 5");
        generatedQuestion.setCorrectAnswer(0); // Index of correct answer

        List<QuizQuestionRequest> generatedQuestions = Arrays.asList(generatedQuestion);

        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(testSubject));
        when(studyNoteRepository.findBySubjectIdOrderByUpdatedAtDesc(SUBJECT_ID))
                .thenReturn(Arrays.asList(testNote));
        when(webSearchService.search(anyString())).thenReturn(Arrays.asList("Web result 1"));
        when(aiOrchestrationService.generateQuizQuestions(anyString(), anyString(), anyString(), eq(count)))
                .thenReturn(CompletableFuture.completedFuture(generatedQuestions));

        Quiz newQuiz = new Quiz();
        newQuiz.setTitle("Quiz IA: Algebra");
        newQuiz.setQuestions(new ArrayList<>());

        QuizQuestion question = new QuizQuestion();
        when(mapper.toEntity(any(QuizRequest.class))).thenReturn(newQuiz);
        when(mapper.toEntity(any(QuizQuestionRequest.class))).thenReturn(question);
        when(quizRepository.save(any(Quiz.class))).thenReturn(newQuiz);
        when(mapper.toResponse(newQuiz)).thenReturn(testQuizResponse);

        // Act
        QuizResponse result = quizService.generateQuiz(SUBJECT_ID, topic, difficulty, count);

        // Assert
        assertNotNull(result);
        verify(subjectRepository, times(2)).findById(SUBJECT_ID);
        verify(aiOrchestrationService).generateQuizQuestions(anyString(), eq(topic), eq(difficulty), eq(count));
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    @DisplayName("Should delete quiz successfully")
    void deleteQuiz_ShouldDeleteQuiz_WhenQuizExists() {
        // Arrange
        when(quizRepository.existsById(QUIZ_ID)).thenReturn(true);
        doNothing().when(quizRepository).deleteById(QUIZ_ID);

        // Act
        quizService.deleteQuiz(QUIZ_ID);

        // Assert
        verify(quizRepository).existsById(QUIZ_ID);
        verify(quizRepository).deleteById(QUIZ_ID);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent quiz")
    void deleteQuiz_ShouldThrowException_WhenQuizNotFound() {
        // Arrange
        when(quizRepository.existsById(QUIZ_ID)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> quizService.deleteQuiz(QUIZ_ID));
        assertTrue(exception.getMessage().contains("Quiz não encontrado"));
        verify(quizRepository).existsById(QUIZ_ID);
        verify(quizRepository, never()).deleteById(any());
    }
}
