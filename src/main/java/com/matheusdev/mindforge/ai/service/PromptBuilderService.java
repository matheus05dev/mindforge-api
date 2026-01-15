package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.study.subject.model.Subject;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    // TÉCNICA: Role-Prompting + Structured Output + Context Injection
    public String buildMentorPrompt(String code, Subject subject, String profileSummary) {
        return String.format(
                "Realize uma revisão de código educacional focada no crescimento do aluno.\n\n" +
                "<student_context>\n" +
                " - Perfil: %s\n" +
                " - Assunto Estudado: %s\n" +
                " - Nível Atual: %s\n" +
                " - Nível Profissional: %s\n" +
                "</student_context>\n\n" +
                "<task>\n" +
                "1. Analise o código abaixo buscando oportunidades de ensino.\n" +
                "2. Não apenas corrija; explique o conceito por trás da correção.\n" +
                "3. Estruture sua resposta EXATAMENTE nas seções abaixo.\n" +
                "</task>\n\n" +
                "<output_format>\n" +
                "### \uD83D\uDD2C Visão Técnica (Code Review)\n" +
                "(Feedback objetivo sobre Clean Code, SOLID e Performance)\n\n" +
                "### \uD83C\uDF93 Momento Didático\n" +
                "(Explique UM conceito chave que o aluno parece não ter dominado completamente. Use analogias se o perfil for 'ANALOGICAL')\n\n" +
                "### \uD83D\uDE80 Próximos Passos\n" +
                "(Sugira um pequeno desafio prático para aplicar o conceito aprendido)\n" +
                "</output_format>\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>",
                profileSummary,
                subject.getName(),
                subject.getProficiencyLevel(),
                subject.getProfessionalLevel(),
                code
        );
    }

    // TÉCNICA: Constraint-Based Prompting + Security Focus
    public String buildAnalystPrompt(String code, Subject subject, String profileSummary) {
        return String.format(
                "Execute uma análise técnica rigorosa visando prontidão para produção (Production Readiness).\n\n" +
                "<constraints>\n" +
                " - Ignore aspectos subjetivos de estilo.\n" +
                " - Foque em: Segurança (OWASP), Performance (Big O), Concorrência e Tratamento de Erros.\n" +
                " - Seja direto. Use bullet points.\n" +
                "</constraints>\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>\n\n" +
                "Responda com uma lista de 'Findings' classificados por severidade (ALTA, MÉDIA, BAIXA).",
                code
        );
    }

    // TÉCNICA: Zero-Shot Transformation
    public String buildContentModificationPrompt(String currentContent, String instruction) {
        return String.format(
                "Sua tarefa é transformar o texto de entrada seguindo estritamente a instrução fornecida.\n\n" +
                "<instruction>\n" +
                "%s\n" +
                "</instruction>\n\n" +
                "<input_text>\n" +
                "%s\n" +
                "</input_text>\n\n" +
                "Retorne APENAS o texto transformado. Não inclua preâmbulos como 'Aqui está o texto modificado'.",
                instruction,
                currentContent
        );
    }

    // TÉCNICA: Contextual Question Answering
    public String buildGenericPrompt(String question, String contextInfo, String profileSummary) {
        return String.format(
                "Responda à pergunta do usuário considerando o contexto fornecido.\n\n" +
                "<context>\n" +
                " - Situação: %s\n" +
                " - Perfil do Usuário: %s\n" +
                "</context>\n\n" +
                "<user_question>\n" +
                "%s\n" +
                "</user_question>\n\n" +
                "Dica: Se a pergunta for complexa, quebre a resposta em passos lógicos.",
                contextInfo,
                profileSummary,
                question
        );
    }

    // TÉCNICA: Chain of Thought (CoT) para Debugging
    public String buildDebugAssistantPrompt(String code) {
        return String.format(
                "Você é um especialista em debugging. Siga este processo de pensamento para encontrar o erro:\n\n" +
                "1. **Análise Estática**: Leia o código e entenda a intenção.\n" +
                "2. **Rastreamento**: Simule mentalmente a execução do código passo a passo.\n" +
                "3. **Hipótese**: Identifique onde o estado do programa diverge do esperado.\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>\n\n" +
                "Saída esperada:\n" +
                "1. **Causa Raiz**: O que exatamente está causando o bug.\n" +
                "2. **Correção**: O código corrigido.\n" +
                "3. **Prevenção**: Como evitar isso no futuro (ex: testes unitários, tipagem mais forte).",
                code
        );
    }

    // TÉCNICA: Socratic Method (Maieutics)
    public String buildSocraticTutorPrompt(String code) {
        return String.format(
                "Analise o código abaixo, mas NÃO forneça a solução ou aponte os erros diretamente.\n\n" +
                "<goal>\n" +
                "Seu objetivo é fazer o aluno perceber o erro por conta própria através de perguntas guiadas.\n" +
                "</goal>\n\n" +
                "<source_code>\n" +
                "%s\n" +
                "</source_code>\n\n" +
                "Gere 3 perguntas reflexivas que foquem na parte mais frágil do código. Exemplo: 'O que acontece com essa variável se a lista for nula?'",
                code
        );
    }

    // TÉCNICA: Persona Simulation (Google Recruiter)
    public String buildPortfolioReviewerPrompt(String readmeContent) {
        return String.format(
                "Atue como um Tech Recruiter Sênior avaliando este README de projeto no GitHub.\n\n" +
                "<evaluation_criteria>\n" +
                "1. **Clareza**: O problema e a solução estão claros em 5 segundos?\n" +
                "2. **Instalação**: É fácil rodar o projeto?\n" +
                "3. **Tecnologias**: A stack está bem definida?\n" +
                "</evaluation_criteria>\n\n" +
                "<readme_content>\n" +
                "%s\n" +
                "</readme_content>\n\n" +
                "Forneça um feedback honesto: Você chamaria esse candidato para uma entrevista baseando-se apenas neste README? Por que?",
                readmeContent
        );
    }

    // TÉCNICA: Framework Application (User Story Format)
    public String buildProductThinkerPrompt(String featureDescription) {
        return String.format(
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
    }
}
