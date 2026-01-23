package com.matheusdev.mindforge.mindmap.dto;

import lombok.Data;

@Data
public class SaveMindMapRequest {
    private String nodesJson;
    private String edgesJson;
}
