package com.matheusdev.mindforge.mindmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindMapResponse {
    private Long id;
    private String name;
    private String nodesJson;
    private String edgesJson;
    private Long workspaceId;
}
