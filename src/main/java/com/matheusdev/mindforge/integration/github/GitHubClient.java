package com.matheusdev.mindforge.integration.github;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GitHubClient {

    private final String GITHUB_API_URL = "https://api.github.com";
    private final RestTemplate restTemplate = new RestTemplate();

    public String getFileContent(String accessToken, String repoOwner, String repoName, String filePath) {
        String url = String.format("%s/repos/%s/%s/contents/%s", GITHUB_API_URL, repoOwner, repoName, filePath);
        
        HttpHeaders headers = createAuthHeaders(accessToken);
        headers.set("Accept", "application/vnd.github.v3.raw");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }
}
