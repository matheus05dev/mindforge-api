package com.matheusdev.mindforge.document.mapper;

import com.matheusdev.mindforge.document.dto.DocumentResponse;
import com.matheusdev.mindforge.document.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "downloadUri", expression = "java(generateDownloadUri(document))")
    DocumentResponse toResponse(Document document);

    default String generateDownloadUri(Document document) {
        if (document == null || document.getFileName() == null) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/documents/download/")
                .path(document.getFileName())
                .toUriString();
    }
}
