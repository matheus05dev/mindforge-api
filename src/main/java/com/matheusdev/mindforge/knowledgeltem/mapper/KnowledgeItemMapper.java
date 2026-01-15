package com.matheusdev.mindforge.knowledgeltem.mapper;

import com.matheusdev.mindforge.document.mapper.DocumentMapper;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemRequest;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DocumentMapper.class})
public interface KnowledgeItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "documents", ignore = true)
    KnowledgeItem toEntity(KnowledgeItemRequest request);

    KnowledgeItemResponse toResponse(KnowledgeItem item);
}
