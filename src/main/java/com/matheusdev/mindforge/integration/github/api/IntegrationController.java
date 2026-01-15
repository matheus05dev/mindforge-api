package com.matheusdev.mindforge.integration.github.api;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.integration.github.dto.GitHubAccessTokenResponse;
import com.matheusdev.mindforge.integration.github.service.GitHubTokenService;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/integrations/github")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "Endpoints para integração com serviços externos como o GitHub")
public class IntegrationController {

    @Value("${github.client.id}")
    private String githubClientId;

    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubTokenService gitHubTokenService;

    @GetMapping("/connect")
    public void connectToGitHub(HttpServletResponse response) throws IOException {
        // O escopo 'repo' é para acesso a repositórios. Adicione 'read:user' se precisar de dados do perfil.
        // O parâmetro 'prompt=consent' força o usuário a re-autorizar, o que é útil para garantir a obtenção de um refresh token.
        String githubAuthUrl = "https://github.com/login/oauth/authorize?client_id=" + githubClientId + "&scope=repo&prompt=consent";
        response.sendRedirect(githubAuthUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGitHubCallback(@RequestParam("code") Optional<String> code, @RequestParam("error") Optional<String> error) {
        if (error.isPresent()) {
            // Se o usuário negar, o GitHub redireciona com um parâmetro 'error'.
            throw new BusinessException("Acesso negado pelo usuário no GitHub: " + error.get());
        }

        if (code.isEmpty()) {
            throw new BusinessException("Código de autorização do GitHub não encontrado no callback.");
        }

        final Long userId = 1L; // Placeholder para o usuário logado

        GitHubAccessTokenResponse tokenResponse = gitHubTokenService.exchangeCodeForToken(code.get());

        UserIntegration integration = userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElse(new UserIntegration());

        integration.setUserId(userId);
        integration.setProvider(UserIntegration.Provider.GITHUB);
        integration.setAccessToken(tokenResponse.getAccessToken());
        
        // O GitHub só retorna um novo refresh token na primeira autorização ou se a anterior for revogada.
        // Portanto, só atualizamos o refresh token se um novo for fornecido.
        if (tokenResponse.getRefreshToken() != null) {
            integration.setRefreshToken(tokenResponse.getRefreshToken());
        }

        userIntegrationRepository.save(integration);

        return ResponseEntity.ok("Conta do GitHub conectada e token salvo com sucesso!");
    }
}
