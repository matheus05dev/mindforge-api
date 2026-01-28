package com.matheusdev.mindforge.integration.github.api;

import com.matheusdev.mindforge.integration.github.GitHubClient;
import com.matheusdev.mindforge.integration.github.dto.GitHubFileTree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub Integration", description = "GitHub repository integration endpoints")
public class GitHubRestController {

    private final GitHubClient gitHubClient;
    private final com.matheusdev.mindforge.core.auth.service.AuthService authService;

    @Operation(summary = "Get repository file tree", description = "Returns the file tree of a GitHub repository")
    @GetMapping("/repos/{owner}/{repo}/tree")
    public ResponseEntity<List<GitHubFileTree>> getRepoTree(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) String path) {

        final Long userId = authService.getCurrentUser().getId();
        List<GitHubFileTree> tree = gitHubClient.getRepoTree(userId, owner, repo, path);
        return ResponseEntity.ok(tree);
    }
}
