package com.matheusdev.mindforge.integration.github.api;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.dto.GitHubAccessTokenResponse;
import com.matheusdev.mindforge.integration.github.service.GitHubTokenService;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/integrations/github")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "Endpoints para integração com serviços externos como o GitHub")
public class IntegrationController {

    @Value("${github.client.id}")
    private String githubClientId;

    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubTokenService gitHubTokenService;
    private final com.matheusdev.mindforge.core.auth.service.AuthService authService;

    @GetMapping("/connect")
    public void connectToGitHub(HttpServletResponse response) throws IOException {
        // Oescopo 'repo' é para acesso a repositórios. Adicione 'read:user' se
        // precisar de dados do perfil.
        // O parâmetro 'prompt=consent' força o usuário a re-autorizar, o que é útil
        // para garantir a obtenção de um refresh token.
        String redirectUri = "http://localhost:8080/api/integrations/github/callback";
        String githubAuthUrl = "https://github.com/login/oauth/authorize?client_id=" + githubClientId
                + "&scope=repo&prompt=consent&redirect_uri=" + redirectUri;
        log.info("Redirecting to GitHub Auth: {}", githubAuthUrl);
        response.sendRedirect(githubAuthUrl);
    }

    @GetMapping("/callback")
    public void handleGitHubCallback(@RequestParam("code") Optional<String> code,
            @RequestParam("error") Optional<String> error, HttpServletResponse response) {
        if (error.isPresent()) {
            log.error("GitHub Auth Error: {}", error.get());
            throw new BusinessException("Acesso negado pelo usuário no GitHub: " + error.get());
        }

        if (code.isEmpty()) {
            log.error("GitHub Auth Code missing");
            throw new BusinessException("Código de autorização do GitHub não encontrado no callback.");
        }

        Long userId;
        try {
            userId = authService.getCurrentUser().getId();
            log.info("Authenticated user resolved for callback: {}", userId);
        } catch (Exception e) {
            log.warn("User not authenticated during callback (Browser Redirect). Falling back to ID 1L. Error: {}",
                    e.getMessage());
            userId = 1L;
        }

        log.info("Processing GitHub Callback for UserId: {}", userId);

        GitHubAccessTokenResponse tokenResponse = gitHubTokenService.exchangeCodeForToken(code.get());
        log.info("Token exchanged successfully. Access Token present: {}", (tokenResponse.getAccessToken() != null));

        UserIntegration integration = userIntegrationRepository
                .findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElse(new UserIntegration());

        integration.setUserId(userId);
        integration.setProvider(UserIntegration.Provider.GITHUB);
        integration.setAccessToken(tokenResponse.getAccessToken());

        // O GitHub só retorna um novo refresh token na primeira autorização ou se a
        // anterior for revogada.
        // Portanto, só atualizamos o refresh token se um novo for fornecido.
        if (tokenResponse.getRefreshToken() != null) {
            integration.setRefreshToken(tokenResponse.getRefreshToken());
        }

        userIntegrationRepository.save(integration);

        try {
            response.sendRedirect("http://localhost:3000/settings/integrations?success=true");
        } catch (IOException e) {
            throw new BusinessException("Erro ao redirecionar para o frontend: " + e.getMessage());
        }
    }

}
