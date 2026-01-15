package com.matheusdev.mindforge.integration.github;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.dto.GitHubAccessTokenResponse;
import com.matheusdev.mindforge.integration.github.service.GitHubTokenService;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final String GITHUB_API_URL = "https://api.github.com";
    private final RestTemplate restTemplate = new RestTemplate();
    private final GitHubTokenService gitHubTokenService;
    private final UserIntegrationRepository userIntegrationRepository;

    public String getFileContent(Long userId, String repoOwner, String repoName, String filePath) {
        // Tenta buscar a integração do usuário
        UserIntegration integration = userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("Integração GitHub não encontrada para o usuário."));

        String currentAccessToken = integration.getAccessToken();
        String currentRefreshToken = integration.getRefreshToken();

        try {
            // Tenta a chamada original
            return makeGitHubApiCall(currentAccessToken, repoOwner, repoName, filePath);
        } catch (HttpClientErrorException.Unauthorized ex) {
            // Se o token expirou (401 Unauthorized)
            if (currentRefreshToken != null && !currentRefreshToken.isEmpty()) {
                // Tenta refrescar o token
                GitHubAccessTokenResponse newTokenResponse = gitHubTokenService.refreshAccessToken(currentRefreshToken);

                // Atualiza a integração do usuário com os novos tokens
                integration.setAccessToken(newTokenResponse.getAccessToken());
                if (newTokenResponse.getRefreshToken() != null) { // GitHub pode não retornar um novo refresh token
                    integration.setRefreshToken(newTokenResponse.getRefreshToken());
                }
                userIntegrationRepository.save(integration);

                // Retenta a chamada com o novo access token
                return makeGitHubApiCall(newTokenResponse.getAccessToken(), repoOwner, repoName, filePath);
            } else {
                // Não há refresh token ou ele é inválido, lança a exceção original
                throw new BusinessException("Token de acesso GitHub expirado e sem refresh token válido para o usuário.", ex);
            }
        } catch (HttpClientErrorException ex) {
            // Outros erros HTTP
            throw new BusinessException("Erro ao acessar o GitHub: " + ex.getStatusCode() + " - " + ex.getStatusText(), ex);
        } catch (Exception ex) {
            // Outros erros inesperados
            throw new BusinessException("Erro inesperado ao acessar o GitHub: " + ex.getMessage(), ex);
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
