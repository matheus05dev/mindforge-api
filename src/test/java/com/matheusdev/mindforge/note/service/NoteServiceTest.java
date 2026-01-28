package com.matheusdev.mindforge.note.service;

import com.matheusdev.mindforge.note.dto.NoteRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.mapper.NoteMapper;
import com.matheusdev.mindforge.note.model.Note;
import com.matheusdev.mindforge.note.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteMapper noteMapper;

    @InjectMocks
    private NoteService noteService;

    private Note testNote;
    private NoteRequestDTO testRequestDTO;
    private NoteResponseDTO testResponseDTO;
    private static final Long NOTE_ID = 1L;

    @BeforeEach
    void setUp() {
        testNote = new Note();
        testNote.setId(NOTE_ID);
        testNote.setTitle("Test Note");
        testNote.setContent("Test Content");
        testNote.setCreatedAt(LocalDateTime.now());
        testNote.setUpdatedAt(LocalDateTime.now());

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
    @DisplayName("Should create note successfully")
    void createNote_ShouldReturnNoteResponse() {
        // Arrange
        when(noteMapper.toEntity(testRequestDTO)).thenReturn(testNote);
        when(noteRepository.save(testNote)).thenReturn(testNote);
        when(noteMapper.toDTO(testNote)).thenReturn(testResponseDTO);

        // Act
        NoteResponseDTO result = noteService.createNote(testRequestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(NOTE_ID, result.getId());
        assertEquals("Test Note", result.getTitle());
        assertEquals("Test Content", result.getContent());
        verify(noteMapper).toEntity(testRequestDTO);
        verify(noteRepository).save(testNote);
        verify(noteMapper).toDTO(testNote);
    }

    @Test
    @DisplayName("Should return all notes")
    void getAllNotes_ShouldReturnListOfNotes() {
        // Arrange
        Note note2 = new Note();
        note2.setId(2L);
        note2.setTitle("Note 2");
        note2.setContent("Content 2");

        NoteResponseDTO responseDTO2 = new NoteResponseDTO();
        responseDTO2.setId(2L);
        responseDTO2.setTitle("Note 2");
        responseDTO2.setContent("Content 2");

        List<Note> notes = Arrays.asList(testNote, note2);

        when(noteRepository.findAll()).thenReturn(notes);
        when(noteMapper.toDTO(testNote)).thenReturn(testResponseDTO);
        when(noteMapper.toDTO(note2)).thenReturn(responseDTO2);

        // Act
        List<NoteResponseDTO> result = noteService.getAllNotes();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Note", result.get(0).getTitle());
        assertEquals("Note 2", result.get(1).getTitle());
        verify(noteRepository).findAll();
        verify(noteMapper, times(2)).toDTO(any(Note.class));
    }

    @Test
    @DisplayName("Should return empty list when no notes exist")
    void getAllNotes_ShouldReturnEmptyList_WhenNoNotesExist() {
        // Arrange
        when(noteRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<NoteResponseDTO> result = noteService.getAllNotes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(noteRepository).findAll();
    }

    @Test
    @DisplayName("Should return note by ID when it exists")
    void getNoteById_ShouldReturnNote_WhenNoteExists() {
        // Arrange
        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.of(testNote));
        when(noteMapper.toDTO(testNote)).thenReturn(testResponseDTO);

        // Act
        NoteResponseDTO result = noteService.getNoteById(NOTE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(NOTE_ID, result.getId());
        assertEquals("Test Note", result.getTitle());
        verify(noteRepository).findById(NOTE_ID);
        verify(noteMapper).toDTO(testNote);
    }

    @Test
    @DisplayName("Should throw exception when note not found by ID")
    void getNoteById_ShouldThrowException_WhenNoteNotFound() {
        // Arrange
        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.getNoteById(NOTE_ID));
        assertTrue(exception.getMessage().contains("Nota não encontrada"));
        verify(noteRepository).findById(NOTE_ID);
        verify(noteMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("Should update note successfully")
    void updateNote_ShouldReturnUpdatedNote() {
        // Arrange
        NoteRequestDTO updateRequest = new NoteRequestDTO();
        updateRequest.setTitle("Updated Title");
        updateRequest.setContent("Updated Content");

        Note updatedNote = new Note();
        updatedNote.setId(NOTE_ID);
        updatedNote.setTitle("Updated Title");
        updatedNote.setContent("Updated Content");

        NoteResponseDTO updatedResponse = new NoteResponseDTO();
        updatedResponse.setId(NOTE_ID);
        updatedResponse.setTitle("Updated Title");
        updatedResponse.setContent("Updated Content");

        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.of(testNote));
        doNothing().when(noteMapper).updateEntityFromDTO(updateRequest, testNote);
        when(noteRepository.save(testNote)).thenReturn(updatedNote);
        when(noteMapper.toDTO(updatedNote)).thenReturn(updatedResponse);

        // Act
        NoteResponseDTO result = noteService.updateNote(NOTE_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());
        verify(noteRepository).findById(NOTE_ID);
        verify(noteMapper).updateEntityFromDTO(updateRequest, testNote);
        verify(noteRepository).save(testNote);
        verify(noteMapper).toDTO(updatedNote);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent note")
    void updateNote_ShouldThrowException_WhenNoteNotFound() {
        // Arrange
        NoteRequestDTO updateRequest = new NoteRequestDTO();
        updateRequest.setTitle("Updated Title");

        when(noteRepository.findById(NOTE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.updateNote(NOTE_ID, updateRequest));
        assertTrue(exception.getMessage().contains("Nota não encontrada"));
        verify(noteRepository).findById(NOTE_ID);
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete note successfully")
    void deleteNote_ShouldDeleteNote() {
        // Arrange
        doNothing().when(noteRepository).deleteById(NOTE_ID);

        // Act
        noteService.deleteNote(NOTE_ID);

        // Assert
        verify(noteRepository).deleteById(NOTE_ID);
    }
}
