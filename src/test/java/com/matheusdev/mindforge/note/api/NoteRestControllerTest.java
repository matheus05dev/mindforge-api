package com.matheusdev.mindforge.note.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.note.dto.NoteRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.service.NoteAIService;
import com.matheusdev.mindforge.note.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class NoteRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @MockBean
    private NoteAIService noteAIService;

    @Autowired
    private ObjectMapper objectMapper;

    private NoteResponseDTO testResponseDTO;
    private NoteRequestDTO testRequestDTO;
    private static final Long NOTE_ID = 1L;

    @BeforeEach
    void setUp() {
        testRequestDTO = new NoteRequestDTO();
        testRequestDTO.setTitle("Test Note");
        testRequestDTO.setContent("Test Content");

        testResponseDTO = new NoteResponseDTO();
        testResponseDTO.setId(NOTE_ID);
        testResponseDTO.setTitle("Test Note");
        testResponseDTO.setContent("Test Content");
        testResponseDTO.setCreatedAt(LocalDateTime.now());
        testResponseDTO.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/notes should create note and return 201")
    void createNote_ShouldReturnCreatedNote() throws Exception {
        // Arrange
        when(noteService.createNote(any(NoteRequestDTO.class))).thenReturn(testResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NOTE_ID))
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("Test Content"));

        verify(noteService).createNote(any(NoteRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/notes should return all notes")
    void getAllNotes_ShouldReturnListOfNotes() throws Exception {
        // Arrange
        NoteResponseDTO note2 = new NoteResponseDTO();
        note2.setId(2L);
        note2.setTitle("Note 2");
        note2.setContent("Content 2");

        List<NoteResponseDTO> notes = Arrays.asList(testResponseDTO, note2);
        when(noteService.getAllNotes()).thenReturn(notes);

        // Act & Assert
        mockMvc.perform(get("/api/notes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Test Note"))
                .andExpect(jsonPath("$[1].title").value("Note 2"));

        verify(noteService).getAllNotes();
    }

    @Test
    @DisplayName("GET /api/notes/{id} should return note by ID")
    void getNoteById_ShouldReturnNote() throws Exception {
        // Arrange
        when(noteService.getNoteById(NOTE_ID)).thenReturn(testResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/notes/{id}", NOTE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTE_ID))
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("Test Content"));

        verify(noteService).getNoteById(NOTE_ID);
    }

    @Test
    @DisplayName("GET /api/notes/{id} should return 500 when note not found")
    void getNoteById_ShouldReturnError_WhenNoteNotFound() throws Exception {
        // Arrange
        when(noteService.getNoteById(NOTE_ID))
                .thenThrow(new RuntimeException("Nota não encontrada"));

        // Act & Assert
        mockMvc.perform(get("/api/notes/{id}", NOTE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(noteService).getNoteById(NOTE_ID);
    }

    @Test
    @DisplayName("PUT /api/notes/{id} should update note")
    void updateNote_ShouldReturnUpdatedNote() throws Exception {
        // Arrange
        NoteRequestDTO updateRequest = new NoteRequestDTO();
        updateRequest.setTitle("Updated Title");
        updateRequest.setContent("Updated Content");

        NoteResponseDTO updatedResponse = new NoteResponseDTO();
        updatedResponse.setId(NOTE_ID);
        updatedResponse.setTitle("Updated Title");
        updatedResponse.setContent("Updated Content");

        when(noteService.updateNote(eq(NOTE_ID), any(NoteRequestDTO.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/notes/{id}", NOTE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTE_ID))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));

        verify(noteService).updateNote(eq(NOTE_ID), any(NoteRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} should delete note and return 204")
    void deleteNote_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(noteService).deleteNote(NOTE_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/notes/{id}", NOTE_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(noteService).deleteNote(NOTE_ID);
    }
}
