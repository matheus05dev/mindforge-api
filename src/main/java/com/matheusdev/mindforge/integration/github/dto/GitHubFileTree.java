package com.matheusdev.mindforge.integration.github.dto;

import lombok.Data;

@Data
public class GitHubFileTree {
    private String path;
    private String type; // "file" or "dir"
    private Long size;
    private String sha;
    private String url;
}
