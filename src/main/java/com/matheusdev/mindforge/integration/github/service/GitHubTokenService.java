package com.matheusdev.mindforge.integration.github.service;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.dto.GitHubAccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubTokenService {

    @Value("${github.client.id}")
    private String githubClientId;

    @Value("${github.client.secret}")
    private String githubClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubAccessTokenResponse exchangeCodeForToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = new HashMap<>();
        body.put("client_id", githubClientId);
        body.put("client_secret", githubClientSecret);
        body.put("code", code);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GitHubAccessTokenResponse> response = restTemplate.postForEntity(url, entity, GitHubAccessTokenResponse.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BusinessException("Falha ao obter o token de acesso do GitHub: resposta inválida.");
            }

            return response.getBody();
        } catch (Exception ex) {
            throw new BusinessException("Erro ao comunicar com o GitHub para trocar o código pelo token.", ex);
        }
    }

    public GitHubAccessTokenResponse refreshAccessToken(String refreshToken) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = new HashMap<>();
        body.put("client_id", githubClientId);
        body.put("client_secret", githubClientSecret);
        body.put("refresh_token", refreshToken);
        body.put("grant_type", "refresh_token");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GitHubAccessTokenResponse> response = restTemplate.postForEntity(url, entity, GitHubAccessTokenResponse.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BusinessException("Falha ao atualizar o token de acesso do GitHub: resposta inválida.");
            }

            return response.getBody();
        } catch (Exception ex) {
            throw new BusinessException("Erro ao comunicar com o GitHub para atualizar o token.", ex);
        }
    }
}
