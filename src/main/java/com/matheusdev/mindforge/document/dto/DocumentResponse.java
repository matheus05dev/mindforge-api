package com.matheusdev.mindforge.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadDate;
    private String downloadUri;
}
