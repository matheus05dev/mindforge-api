package com.matheusdev.mindforge.integration.github;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.dto.GitHubFileTree;
import com.matheusdev.mindforge.integration.github.service.GitHubTokenService;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final String GITHUB_API_URL = "https://api.github.com";
    private final RestTemplate restTemplate = new RestTemplate();
    private final GitHubTokenService gitHubTokenService;
    private final UserIntegrationRepository userIntegrationRepository;

    public String getFileContent(Long userId, String repoOwner, String repoName, String filePath) {
        UserIntegration integration = userIntegrationRepository
                .findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("Integração GitHub não encontrada para o usuário."));

        String currentAccessToken = integration.getAccessToken();
        String currentRefreshToken = integration.getRefreshToken();

        try {
            return makeGitHubApiCall(currentAccessToken, repoOwner, repoName, filePath);
        } catch (HttpClientErrorException.Unauthorized ex) {
            if (currentRefreshToken != null && !currentRefreshToken.isEmpty()) {
                var newTokenResponse = gitHubTokenService.refreshAccessToken(currentRefreshToken);
                integration.setAccessToken(newTokenResponse.getAccessToken());
                if (newTokenResponse.getRefreshToken() != null) {
                    integration.setRefreshToken(newTokenResponse.getRefreshToken());
                }
                userIntegrationRepository.save(integration);
                return makeGitHubApiCall(newTokenResponse.getAccessToken(), repoOwner, repoName, filePath);
            } else {
                throw new BusinessException(
                        "Token de acesso GitHub expirado e sem refresh token válido para o usuário.", ex);
            }
        } catch (HttpClientErrorException ex) {
            throw new BusinessException("Erro ao acessar o GitHub: " + ex.getStatusCode() + " - " + ex.getStatusText(),
                    ex);
        } catch (Exception ex) {
            throw new BusinessException("Erro inesperado ao acessar o GitHub: " + ex.getMessage(), ex);
        }
    }

    /**
     * Busca a árvore de arquivos de um repositório
     */
    public List<GitHubFileTree> getRepoTree(Long userId, String repoOwner, String repoName, String path) {
        log.info("Fetching repo tree for UserId: {}, Owner: {}, Repo: {}, Path: {}", userId, repoOwner, repoName, path);

        UserIntegration integration = userIntegrationRepository
                .findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> {
                    log.error("GitHub Integration NOT FOUND for UserId: {}", userId);
                    return new BusinessException("Integração GitHub não encontrada para o usuário.");
                }); // Line 67

        String currentAccessToken = integration.getAccessToken();

        log.info("Integration found for UserId: {}. Access Token present.", userId);

        try {
            String url;
            if (path == null || path.isEmpty()) {
                url = String.format("%s/repos/%s/%s/contents", GITHUB_API_URL, repoOwner, repoName);
            } else {
                url = String.format("%s/repos/%s/%s/contents/%s", GITHUB_API_URL, repoOwner, repoName, path);
            }
            HttpHeaders headers = createAuthHeaders(currentAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubFileTree[]> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    GitHubFileTree[].class);
            return List.of(response.getBody());
        } catch (HttpClientErrorException ex) {
            log.error("GitHub API Error for UserId: {}: {}", userId, ex.getMessage());
            throw new BusinessException("Erro ao buscar árvore de arquivos do GitHub: " + ex.getStatusCode(), ex);
        }
    }

    private String makeGitHubApiCall(String accessToken, String repoOwner, String repoName, String filePath) {
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
