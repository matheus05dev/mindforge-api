package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.study.subject.model.Subject;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PromptBuilderService {

    private String buildBaseSystemPrompt(UserProfileAI userProfile, Optional<Subject> subject, Optional<Project> project) {
        StringBuilder sb = new StringBuilder(
                "Você é um assistente de IA especialista em aprendizado e desenvolvimento de software. " +
                "Seu objetivo é ajudar o usuário a atingir seu potencial máximo. " +
                "Adapte sua linguagem e profundidade técnica com base no perfil do usuário fornecido."
        );

        sb.append("\n\n--- CONTEXTO DO USUÁRIO ---\n");
        sb.append("Perfil Resumido: ").append(userProfile.getSummary()).append("\n");

        subject.ifPresent(s -> {
            sb.append("Assunto de Estudo Atual: ").append(s.getName()).append("\n");
            sb.append("Nível de Proficiência: ").append(s.getProficiencyLevel()).append("\n");
        });

        project.ifPresent(p -> sb.append("Projeto Atual: ").append(p.getName()).append("\n"));

        return sb.toString();
    }

    public PromptPair buildMentorPrompt(String code, UserProfileAI profile, Subject subject) {
        String systemPrompt = buildBaseSystemPrompt(profile, Optional.of(subject), Optional.empty());
        String userPrompt = String.format(
                "Realize uma revisão de código educacional com foco no crescimento, seguindo o formato de saída especificado.\n\n" +
                "<output_format>\n" +
                "### \uD83D\uDD2C Visão Técnica (Code Review)\n" +
                "(Feedback objetivo sobre Clean Code, SOLID e Performance)\n\n" +
                "### \uD83C\uDF93 Momento Didático\n" +
                "(Explique UM conceito chave que o código poderia melhorar)\n\n" +
                "### \uD83D\uDE80 Próximos Passos\n" +
                "(Sugira um pequeno desafio prático para aplicar o conceito aprendido)\n" +
                "</output_format>\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>",
                code
        );
        return new PromptPair(systemPrompt, userPrompt);
    }

    public PromptPair buildGenericPrompt(String question, UserProfileAI profile, Optional<Subject> subject, Optional<Project> project) {
        String systemPrompt = buildBaseSystemPrompt(profile, subject, project);
        String userPrompt = String.format(
                "Responda à pergunta do usuário de forma clara e didática, quebrando a resposta em passos lógicos se for complexa.\n\n" +
                "<user_question>\n" +
                "%s\n" +
                "</user_question>",
                question
        );
        return new PromptPair(systemPrompt, userPrompt);
    }
    
    public PromptPair buildContentModificationPrompt(String currentContent, String instruction) {
        String systemPrompt = "Você é um assistente de edição de texto. Sua única função é transformar o texto de entrada de acordo com a instrução, sem adicionar nenhum conteúdo ou formatação extra.";
        String userPrompt = String.format(
                "Transforme o texto de entrada seguindo estritamente a instrução. " +
                "Retorne APENAS o texto transformado, sem preâmbulos.\n\n" +
                "<instruction>\n" +
                "%s\n" +
                "</instruction>\n\n" +
                "<input_text>\n" +
                "%s\n" +
                "</input_text>",
                instruction,
                currentContent
        );
        return new PromptPair(systemPrompt, userPrompt);
    }

    public PromptPair buildProductThinkerPrompt(String featureDescription) {
        String systemPrompt = "Você é um Gerente de Produto (Product Manager) experiente. Sua especialidade é transformar ideias em especificações claras e acionáveis para o time de desenvolvimento.";
        String userPrompt = String.format(
                "Transforme a ideia bruta abaixo em uma especificação de produto profissional.\n\n" +
                "<raw_idea>\n" +
                "%s\n" +
                "</raw_idea>\n\n" +
                "Gere a saída no seguinte formato:\n" +
                "**User Story**\n" +
                "Como um [persona], eu quero [ação], para que [benefício].\n\n" +
                "**Critérios de Aceite (Gherkin)**\n" +
                "- Dado que...\n" +
                "- Quando...\n" +
                "- Então...\n\n" +
                "**Definição de Pronto (DoD)**\n" +
                "- Listar 3 requisitos técnicos essenciais.",
                featureDescription
        );
        return new PromptPair(systemPrompt, userPrompt);
    }

    public PromptPair buildPortfolioReviewerPrompt(String readmeContent, UserProfileAI userProfile) {
        String systemPrompt = buildBaseSystemPrompt(userProfile, Optional.empty(), Optional.empty()) +
                "\nAdicionalmente, você está atuando como um Tech Recruiter Sênior do Google, avaliando um projeto no GitHub para uma vaga de Engenheiro de Software Pleno.";
        String userPrompt = String.format(
                "Avalie este README de projeto. Seja brutalmente honesto, mas construtivo. Você chamaria esse candidato para uma entrevista com base apenas neste README? Por quê?\n\n" +
                "<evaluation_criteria>\n" +
                "1. **Clareza**: O problema e a solução estão claros em 5 segundos?\n" +
                "2. **Instalação**: É fácil rodar o projeto? As instruções são à prova de falhas?\n" +
                "3. **Qualidade**: A stack está bem definida? O README demonstra profissionalismo?\n" +
                "</evaluation_criteria>\n\n" +
                "<readme_content>\n" +
                "%s\n" +
                "</readme_content>",
                readmeContent
        );
        return new PromptPair(systemPrompt, userPrompt);
    }
    
    // Manter os outros métodos simplificados por enquanto, para focar na correção principal.
    // Eles podem ser expandidos para usar o buildBaseSystemPrompt no futuro.

    public PromptPair buildAnalystPrompt(String code) {
        String systemPrompt = "Você é um analisador de código focado em prontidão para produção. Seja direto e técnico.";
        String userPrompt = String.format(
                "Execute uma análise técnica rigorosa. Foque em: Segurança (OWASP), Performance (Big O), Concorrência e Tratamento de Erros. " +
                "Responda com uma lista de 'Findings' classificados por severidade (ALTA, MÉDIA, BAIXA).\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>",
                code
        );
        return new PromptPair(systemPrompt, userPrompt);
    }

    public PromptPair buildDebugAssistantPrompt(String code) {
        String systemPrompt = "Você é um especialista em debugging. Siga um processo de pensamento lógico para encontrar a causa raiz do erro.";
        String userPrompt = String.format(
                "Encontre a causa raiz do bug no código abaixo, sugira a correção e explique como preveni-lo no futuro.\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>",
                code
        );
        return new PromptPair(systemPrompt, userPrompt);
    }

    public PromptPair buildSocraticTutorPrompt(String code) {
        String systemPrompt = "Você é um tutor socrático. Seu objetivo é guiar o aluno à solução através de perguntas, sem dar a resposta diretamente.";
        String userPrompt = String.format(
                "Gere 3 perguntas reflexivas que foquem na parte mais frágil do código para ajudar o aluno a perceber o erro por conta própria.\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>",
                code
        );
        return new PromptPair(systemPrompt, userPrompt);
    }
}
