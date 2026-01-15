package com.matheusdev.mindforge.integration.github.service;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.dto.GitHubAccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class GitHubTokenService {

    @Value("${github.client.id}")
    private String githubClientId;

    @Value("${github.client.secret}")
    private String githubClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Refreshes the GitHub access token using the provided refresh token.
     * @param refreshToken The refresh token.
     * @return A response entity containing the new access token details.
     */
    public GitHubAccessTokenResponse refreshAccessToken(String refreshToken) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", githubClientId);
        body.add("client_secret", githubClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GitHubAccessTokenResponse> response = restTemplate.postForEntity(url, entity, GitHubAccessTokenResponse.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BusinessException("Falha ao atualizar o token de acesso do GitHub: resposta inválida do servidor.");
            }

            return response.getBody();
        } catch (Exception ex) {
            throw new BusinessException("Falha ao comunicar com o servidor do GitHub para atualizar o token.", ex);
        }
    }
}
