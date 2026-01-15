package com.matheusdev.mindforge.integration.github.api;

import com.matheusdev.mindforge.integration.github.dto.GitHubAccessTokenResponse;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/github")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "Endpoints para integração com serviços externos como o GitHub")
public class IntegrationController {

    @Value("${github.client.id}")
    private String githubClientId;

    @Value("${github.client.secret}")
    private String githubClientSecret;

    private final UserIntegrationRepository userIntegrationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/connect")
    public void connectToGitHub(HttpServletResponse response) throws IOException {
        String githubAuthUrl = "https://github.com/login/oauth/authorize?client_id=" + githubClientId + "&scope=repo";
        response.sendRedirect(githubAuthUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGitHubCallback(@RequestParam("code") String code) {
        final Long userId = 1L; // Placeholder para o usuário logado

        String accessToken = getAccessToken(code);

        UserIntegration integration = userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElse(new UserIntegration());

        integration.setUserId(userId);
        integration.setProvider(UserIntegration.Provider.GITHUB);
        integration.setAccessToken(accessToken);

        userIntegrationRepository.save(integration);

        return ResponseEntity.ok("Conta do GitHub conectada e token salvo com sucesso!");
    }

    private String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = new HashMap<>();
        body.put("client_id", githubClientId);
        body.put("client_secret", githubClientSecret);
        body.put("code", code);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<GitHubAccessTokenResponse> response = restTemplate.postForEntity(url, entity, GitHubAccessTokenResponse.class);

        if (response.getBody() == null || response.getBody().getAccessToken() == null) {
            throw new RuntimeException("Falha ao obter o token de acesso do GitHub.");
        }

        return response.getBody().getAccessToken();
    }
}
