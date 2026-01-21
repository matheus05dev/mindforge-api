package com.matheusdev.mindforge.knowledgeltem.dto;

import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeVersionResponse {

    private Long id;
    private Long knowledgeItemId;
    private String title;
    private String contentPreview; // First 200 chars
    private String fullContent; // Only included when specifically requested
    private LocalDateTime createdAt;
    private KnowledgeVersion.ChangeType changeType;
    private String proposalId;
    private String changeSummary;

    public static KnowledgeVersionResponse fromEntity(KnowledgeVersion version, boolean includeFullContent) {
        String preview = version.getContent().length() > 200
                ? version.getContent().substring(0, 200) + "..."
                : version.getContent();

        return KnowledgeVersionResponse.builder()
                .id(version.getId())
                .knowledgeItemId(version.getKnowledgeItemId())
                .title(version.getTitle())
                .contentPreview(preview)
                .fullContent(includeFullContent ? version.getContent() : null)
                .createdAt(version.getCreatedAt())
                .changeType(version.getChangeType())
                .proposalId(version.getProposalId())
                .changeSummary(version.getChangeSummary())
                .build();
    }
}
