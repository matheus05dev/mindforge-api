package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.chat.model.ChatMessage;
import com.matheusdev.mindforge.ai.chat.model.ChatSession;
import com.matheusdev.mindforge.ai.chat.repository.ChatMessageRepository;
import com.matheusdev.mindforge.ai.chat.repository.ChatSessionRepository;
import com.matheusdev.mindforge.ai.dto.*;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.ai.memory.service.MemoryService;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.repository.DocumentRepository;
import com.matheusdev.mindforge.document.service.FileStorageService;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.integration.github.GitHubClient;
import com.matheusdev.mindforge.integration.model.UserIntegration;
import com.matheusdev.mindforge.integration.repository.UserIntegrationRepository;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.service.KnowledgeBaseService;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {

    private final PromptCacheService promptCacheService;
    private final MemoryService memoryService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeItemMapper knowledgeItemMapper;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final SubjectRepository subjectRepository;
    private final ProjectRepository projectRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final GitHubClient gitHubClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage analyzeCodeForProficiency(CodeAnalysisRequest request) throws IOException {
        final Long userId = 1L; // Provisório
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + request.getSubjectId()));
        String code = getCodeFromRequest(request);
        UserProfileAI userProfile = memoryService.getProfile(userId);
        String profileSummary = userProfile.getSummary();

        String prompt;
        switch (request.getMode()) {
            case ANALYST:
                prompt = buildAnalystPrompt(code, subject, profileSummary);
                break;
            case DEBUG_ASSISTANT:
                prompt = buildDebugAssistantPrompt(code);
                break;
            case SOCRATIC_TUTOR:
                prompt = buildSocraticTutorPrompt(code);
                break;
            case MENTOR:
            default:
                prompt = buildMentorPrompt(code, subject, profileSummary);
                break;
        }

        ChatSession session = getOrCreateChatSession(subject, request.getMode().name());
        ChatMessage userMessage = saveMessage(session, "user", prompt);
        
        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }

        ChatMessage assistantMessage = saveMessage(session, "assistant", aiResponse.getContent());
        
        List<Map<String, String>> chatHistory = new ArrayList<>();
        chatHistory.add(Map.of("role", "user", "content", userMessage.getContent()));
        chatHistory.add(Map.of("role", "assistant", "content", assistantMessage.getContent()));
        memoryService.updateUserProfile(userId, chatHistory);

        return assistantMessage;
    }

    @Transactional
    public ChatMessage analyzeGitHubFile(GitHubFileAnalysisRequest request) throws IOException {
        final Long userId = 1L; // Provisório
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + request.getProjectId()));
        
        // A verificação de integração agora é feita dentro do GitHubClient, mas mantemos aqui para fail-fast
        userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("O usuário não conectou a conta do GitHub."));
        
        String repoUrl = project.getGithubRepoUrl();
        if (repoUrl == null || repoUrl.isEmpty()) {
            throw new BusinessException("O projeto não está vinculado a um repositório do GitHub.");
        }
        String[] urlParts = repoUrl.replace("https://github.com/", "").split("/");
        String owner = urlParts[0];
        String repoName = urlParts[1];
        
        // MUDANÇA: Passando userId para o GitHubClient
        String fileContent = gitHubClient.getFileContent(userId, owner, repoName, request.getFilePath());
        
        Subject subject = subjectRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum assunto de estudo encontrado para contextualizar a análise."));
        CodeAnalysisRequest internalRequest = new CodeAnalysisRequest();
        internalRequest.setSubjectId(subject.getId());
        internalRequest.setCodeToAnalyze(fileContent);
        internalRequest.setMode(request.getMode());
        return analyzeCodeForProficiency(internalRequest);
    }

    @Transactional
    public KnowledgeItemResponse modifyKnowledgeItemContent(Long itemId, String instruction) {
        KnowledgeItem item = knowledgeBaseService.getItemById(itemId);
        String prompt = buildContentModificationPrompt(item.getContent(), instruction);
        
        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }

        item.setContent(aiResponse.getContent());
        KnowledgeItem updatedItem = knowledgeBaseService.updateItem(itemId, item);
        return knowledgeItemMapper.toResponse(updatedItem);
    }

    @Transactional
    public KnowledgeItemResponse transcribeImageAndAppendToKnowledgeItem(Long documentId, Long itemId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado com o id: " + documentId));

        if (document.getFileType() == null || !document.getFileType().startsWith("image")) {
            throw new BusinessException("O documento fornecido não é uma imagem.");
        }

        Resource resource = fileStorageService.loadFileAsResource(document.getFileName());
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());
        
        String prompt = "Transcreva o texto contido nesta imagem.";
        AIProviderRequest request = new AIProviderRequest(prompt, fileContent, document.getFileType());
        
        AIProviderResponse aiResponse = promptCacheService.executeRequest(request);

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }

        String transcribedText = aiResponse.getContent();

        KnowledgeItem item = knowledgeBaseService.getItemById(itemId);
        String currentContent = item.getContent() == null ? "" : item.getContent();
        
        item.setContent(currentContent + "\n\n--- TEXTO TRANSCRITO DA IMAGEM " + document.getFileName() + " ---\n" + transcribedText);
        
        KnowledgeItem updatedItem = knowledgeBaseService.updateItem(itemId, item);

        return knowledgeItemMapper.toResponse(updatedItem);
    }

    @Transactional
    public ChatMessage analyzeGeneric(GenericAnalysisRequest request) {
        final Long userId = 1L; // Provisório
        String contextInfo = getContextInfo(request);
        UserProfileAI userProfile = memoryService.getProfile(userId);
        String profileSummary = userProfile.getSummary();
        String prompt = buildGenericPrompt(request.getQuestion(), contextInfo, profileSummary);
        ChatSession session = getOrCreateGenericChatSession(request);
        ChatMessage userMessage = saveMessage(session, "user", prompt);
        
        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));
        
        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }
        ChatMessage assistantMessage = saveMessage(session, "assistant", aiResponse.getContent());
        List<Map<String, String>> chatHistory = new ArrayList<>();
        chatHistory.add(Map.of("role", "user", "content", userMessage.getContent()));
        chatHistory.add(Map.of("role", "assistant", "content", assistantMessage.getContent()));
        memoryService.updateUserProfile(userId, chatHistory);
        return assistantMessage;
    }

    @Transactional
    public ChatMessage reviewPortfolio(PortfolioReviewRequest request) throws IOException {
        final Long userId = 1L; // Provisório
        String[] urlParts = request.getGithubRepoUrl().replace("https://github.com/", "").split("/");
        String owner = urlParts[0];
        String repoName = urlParts[1];
        
        // A verificação de integração agora é feita dentro do GitHubClient, mas mantemos aqui para fail-fast
        userIntegrationRepository.findByUserIdAndProvider(userId, UserIntegration.Provider.GITHUB)
                .orElseThrow(() -> new BusinessException("O usuário não conectou a conta do GitHub para esta operação."));
        
        // MUDANÇA: Passando userId para o GitHubClient
        String readmeContent = gitHubClient.getFileContent(userId, owner, repoName, "README.md");
        
        String prompt = buildPortfolioReviewerPrompt(readmeContent);
        ChatSession session = getOrCreateGenericChatSession(null);
        ChatMessage userMessage = saveMessage(session, "user", prompt);
        
        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));
        
        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }
        return saveMessage(session, "assistant", aiResponse.getContent());
    }

    @Transactional
    public ChatMessage thinkAsProductManager(ProductThinkerRequest request) {
        String prompt = buildProductThinkerPrompt(request.getFeatureDescription());
        ChatSession session = getOrCreateGenericChatSession(null);
        ChatMessage userMessage = saveMessage(session, "user", prompt);
        
        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));
        
        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }
        return saveMessage(session, "assistant", aiResponse.getContent());
    }

    private String getCodeFromRequest(CodeAnalysisRequest request) throws IOException {
        if (request.getCodeToAnalyze() != null && !request.getCodeToAnalyze().isEmpty()) {
            return request.getCodeToAnalyze();
        }
        if (request.getDocumentId() != null) {
            Document doc = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado com o id: " + request.getDocumentId()));
            Resource resource = fileStorageService.loadFileAsResource(doc.getFileName());
            return new String(Files.readAllBytes(resource.getFile().toPath()));
        }
        throw new BusinessException("Nenhum código ou ID de documento foi fornecido para análise.");
    }

    private ChatSession getOrCreateChatSession(Subject subject, String mode) {
        ChatSession session = new ChatSession();
        session.setSubject(subject);
        session.setTitle(String.format("Análise de Código (%s) para %s", mode, subject.getName()));
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    private ChatSession getOrCreateGenericChatSession(GenericAnalysisRequest request) {
        ChatSession session = new ChatSession();
        if (request != null && request.getSubjectId() != null) {
            session.setSubject(subjectRepository.findById(request.getSubjectId()).orElse(null));
        }
        if (request != null && request.getProjectId() != null) {
            session.setProject(projectRepository.findById(request.getProjectId()).orElse(null));
        }
        String title = (request != null) ? request.getQuestion() : "Sessão Genérica";
        session.setTitle("Análise: " + title.substring(0, Math.min(title.length(), 20)) + "...");
        session.setCreatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    private String getContextInfo(GenericAnalysisRequest request) {
        if (request.getSubjectId() != null) {
            return "no contexto do assunto de estudo: " + subjectRepository.findById(request.getSubjectId()).map(Subject::getName).orElse("Assunto desconhecido");
        }
        if (request.getProjectId() != null) {
            return "no contexto do projeto: " + projectRepository.findById(request.getProjectId()).map(Project::getName).orElse("Projeto desconhecido");
        }
        return "em um contexto geral";
    }

    private ChatMessage saveMessage(ChatSession session, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        return chatMessageRepository.save(message);
    }

    private String buildMentorPrompt(String code, Subject subject, String profileSummary) {
        return String.format(
                "Assuma duas personalidades em sua análise: a de um **Tech Lead sênior**, focado em qualidade de código e mercado, e a de um **Professor didático**, focado no aprendizado e evolução do aluno.\n\n" +
                        "**Contexto do Aluno:**\n" +
                        "- **Resumo do Perfil:** %s\n" +
                        "- **Assunto:** %s\n" +
                        "- **Nível de Proficiência Atual:** %s\n" +
                        "- **Nível Profissional Atual:** %s\n\n" +
                        "**Sua Tarefa:**\n" +
                        "Analise o código abaixo e forneça um feedback estruturado em 4 seções, usando Markdown:\n\n" +
                        "### 1. Análise do Tech Lead (Avaliação de Mercado)\n" +
                        "Seja direto e profissional. Avalie o código como se estivesse em uma code review para um projeto real. Comente sobre clareza, eficiência, manutenibilidade e aderência a boas práticas (SOLID, DRY, etc.).\n\n" +
                        "### 2. Análise do Professor (Avaliação Didática)\n" +
                        "Seja encorajador e didático. Identifique os **pontos fortes** do código, elogiando o que o aluno acertou, especialmente considerando seu nível atual. Identifique os **gaps de conhecimento** (os 'porquês' por trás dos erros) de forma construtiva.\n\n" +
                        "### 3. Plano de Ação (O Caminho a Seguir)\n" +
                        "Com base nos gaps identificados, crie um plano de ação claro e prático. Para cada ponto a melhorar, forneça:\n" +
                        "- **O Conceito:** Qual conceito fundamental o aluno precisa aprender (ex: 'Injeção de Dependência', 'Programação Funcional com Streams').\n" +
                        "- **Breve Explicação:** Uma explicação curta e simples do conceito.\n" +
                        "- **Próximo Passo Prático:** Uma sugestão de como aplicar o conceito para corrigir ou melhorar o código apresentado.\n\n" +
                        "### 4. Resumo e Próximo Nível\n" +
                        "Finalize com uma frase de encorajamento e aponte qual é a principal habilidade ou conceito que, se dominado, levará o aluno para o próximo nível de proficiência.\n\n" +
                        "--- CÓDIGO PARA ANÁLISE ---\n" +
                        "```\n" +
                        "%s\n" +
                        "```",
                profileSummary,
                subject.getName(),
                subject.getProficiencyLevel(),
                subject.getProfessionalLevel(),
                code
        );
    }

    private String buildAnalystPrompt(String code, Subject subject, String profileSummary) {
        return String.format(
                "Aja como um **Analista de Código Sênior e pragmático**. Sua única prioridade é a qualidade técnica e a prontidão para produção. Ignore o nível do desenvolvedor e foque exclusivamente no código.\n\n" +
                        "**Contexto Adicional (Perfil do Desenvolvedor):** %s\n\n" +
                        "**Sua Tarefa:**\n" +
                        "Forneça uma análise direta e objetiva do código abaixo. A resposta deve ser em formato de lista (bullet points), sem rodeios ou encorajamento. Para cada ponto, seja específico e, se aplicável, mostre um pequeno trecho de como o código deveria ser.\n\n" +
                        "**Estrutura da Resposta:**\n" +
                        "- **Pontos Positivos:** (Se houver, liste o que está bom e por quê)\n" +
                        "- **Pontos Negativos Críticos:** (Bugs, falhas de segurança, problemas de performance)\n" +
                        "- **Sugestões de Refatoração:** (Melhorias de legibilidade, design patterns, boas práticas)\n\n" +
                        "--- CÓDIGO PARA ANÁLISE ---\n" +
                        "```\n" +
                        "%s\n" +
                        "```",
                profileSummary,
                code
        );
    }

    private String buildContentModificationPrompt(String currentContent, String instruction) {
        return String.format(
                "Aja como um assistente de escrita. O usuário forneceu um texto e uma instrução. Sua tarefa é aplicar a instrução ao texto e retornar **APENAS** o texto modificado, sem nenhum comentário ou introdução.\n\n" +
                        "**Instrução do Usuário:** \"%s\"\n\n" +
                        "--- TEXTO ORIGINAL ---\n" +
                        "%s",
                instruction,
                currentContent
        );
    }

    private String buildGenericPrompt(String question, String contextInfo, String profileSummary) {
        return String.format(
                "Você é um assistente especialista e multifacetado. Sua tarefa é responder à pergunta do usuário da forma mais completa e didática possível, considerando o contexto fornecido.\n\n" +
                        "**Contexto da Pergunta:** A pergunta foi feita %s.\n" +
                        "**Contexto do Usuário:** %s\n\n" +
                        "**Personalidade a Assumir:**\n" +
                        "- Se a pergunta for sobre **criar um plano de estudos ou roadmap de carreira**, assuma o papel de um **Study Architect e Mentor de Carreira**. Forneça uma resposta estruturada com fases, ordem de aprendizado e recursos sugeridos.\n" +
                        "- Se a pergunta for sobre **gestão de projetos** e nenhuma metodologia for especificada, assuma o papel de um **Agile Coach** e estruture sua resposta usando conceitos do **Scrum** (ex: Épicos, User Stories, Tarefas).\n" +
                        "- Se a pergunta for sobre **matemática, física ou outra ciência**, assuma o papel de um **Professor Universitário**, explicando o conceito passo a passo, mostrando as fórmulas e resolvendo com um exemplo prático.\n" +
                        "- Para outros assuntos, atue como um especialista didático na área.\n\n" +
                        "**Pergunta do Usuário:**\n" +
                        "\"%s\"",
                contextInfo,
                profileSummary,
                question
        );
    }

    private String buildDebugAssistantPrompt(String code) {
        return String.format(
                "Aja como um **Debug Assistant Sênior**. O usuário forneceu um trecho de código que provavelmente contém um bug ou um erro de lógica. Sua tarefa é analisar o código e fornecer uma hipótese clara sobre a causa raiz do problema.\n\n" +
                        "**Estrutura da Resposta:**\n" +
                        "1. **Hipótese do Problema:** Descreva qual você acredita ser o bug mais provável e por quê.\n" +
                        "2. **Linha(s) Suspeita(s):** Aponte a(s) linha(s) exata(s) onde o erro pode estar ocorrendo.\n" +
                        "3. **Sugestão de Correção:** Forneça um trecho de código corrigido e explique por que a correção funciona.\n" +
                        "4. **Como Depurar:** Sugira como o usuário poderia ter chegado a essa conclusão sozinho, recomendando o uso de logs, breakpoints ou testes unitários.\n\n" +
                        "--- CÓDIGO PARA ANÁLISE ---\n```\n%s\n```",
                code
        );
    }

    private String buildSocraticTutorPrompt(String code) {
        return String.format(
                "Aja como um **Tutor Socrático**. Seu objetivo não é dar respostas, mas sim **fazer perguntas** que guiem o aluno a encontrar a solução por si mesmo. Analise o código abaixo e, em vez de apontar os erros, faça 2-3 perguntas abertas e instigantes sobre as partes mais fracas do código.\n\n" +
                        "**REGRAS:**\n" +
                        "- Não dê a resposta diretamente.\n" +
                        "- Suas perguntas devem estimular o pensamento crítico sobre design de código, performance ou legibilidade.\n\n" +
                        "--- CÓDIGO PARA ANÁLISE ---\n```\n%s\n```",
                code
        );
    }

    private String buildPortfolioReviewerPrompt(String readmeContent) {
        return String.format(
                "Aja como um **Tech Recruiter e Engenheiro de Software Sênior** do Google, avaliando o portfólio de um candidato para uma vaga de nível Mid-Level.\n\n" +
                        "**Sua Tarefa:** Analise o README.md do projeto e forneça um feedback acionável para o candidato, estruturado em 3 seções:\n\n" +
                        "### 1. Análise do README.md\n" +
                        "O README é claro? Ele vende bem o projeto? Faltam seções importantes (ex: arquitetura, como rodar)? Sugira melhorias diretas no texto.\n\n" +
                        "### 2. Pontos Fortes a Destacar\n" +
                        "Com base no que está descrito, quais são os 2-3 pontos que o candidato deveria destacar em seu LinkedIn ou em uma entrevista?\n\n" +
                        "### 3. Pontos Fracos (Perguntas de Entrevista)\n" +
                        "Quais são os 2-3 pontos fracos ou débitos técnicos implícitos no README que você, como entrevistador, questionaria? Formule a pergunta como se estivesse na entrevista.\n\n" +
                        "--- README.md PARA ANÁLISE ---\n%s",
                readmeContent
        );
    }

    private String buildProductThinkerPrompt(String featureDescription) {
        return String.format(
                "Aja como um **Product Manager (PM) experiente** da Atlassian (Jira, Confluence).\n\n" +
                        "**Sua Tarefa:** Analise a seguinte ideia de funcionalidade para a plataforma MindForge e forneça uma análise de produto concisa.\n\n" +
                        "**Estrutura da Análise:**\n" +
                        "1. **User Story:** Reescreva a ideia como uma User Story clara no formato 'Como um [usuário], eu quero [fazer algo], para que [eu obtenha este valor]'.\n" +
                        "2. **Sugestões de UX:** Como essa funcionalidade deveria se parecer na interface? Descreva o fluxo do usuário em 3-4 passos simples.\n" +
                        "3. **Trade-offs Técnicos:** Quais são os principais desafios ou decisões técnicas a serem consideradas?\n" +
                        "4. **Métricas de Sucesso:** Como saberíamos se essa feature é um sucesso após o lançamento? Sugira 1-2 métricas.\n\n" +
                        "--- IDEIA DA FUNCIONALIDADE ---\n%s",
                featureDescription
        );
    }
}
