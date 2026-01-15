package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.memory.model.CommunicationTone;
import com.matheusdev.mindforge.ai.memory.model.LearningStyle;
import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import org.springframework.stereotype.Service;

@Service
public class AIContextService {

    public String buildSystemMessage(UserProfileAI profile, String baseRole) {
        StringBuilder systemMessage = new StringBuilder();
        
        // 1. Define o Papel Base
        systemMessage.append("Você está atuando como ").append(baseRole).append(".\n\n");

        // 2. Adapta ao Tom de Comunicação
        systemMessage.append("### Diretrizes de Comunicação:\n");
        if (profile.getCommunicationTone() != null) {
            switch (profile.getCommunicationTone()) {
                case SOCRATIC:
                    systemMessage.append("- Não dê respostas prontas imediatamente. Faça perguntas que guiem o usuário à solução.\n");
                    systemMessage.append("- Estimule o pensamento crítico.\n");
                    break;
                case DIRECT:
                    systemMessage.append("- Seja extremamente conciso e objetivo.\n");
                    systemMessage.append("- Evite introduções longas ou 'fluff'. Vá direto ao ponto técnico.\n");
                    break;
                case ACADEMIC:
                    systemMessage.append("- Use linguagem formal e precisa.\n");
                    systemMessage.append("- Cite conceitos teóricos fundamentais sempre que possível.\n");
                    break;
                case ENCOURAGING:
                default:
                    systemMessage.append("- Seja positivo, paciente e motivador.\n");
                    systemMessage.append("- Celebre pequenas vitórias e explique erros de forma construtiva.\n");
                    break;
            }
        }

        // 3. Adapta ao Estilo de Aprendizado
        systemMessage.append("\n### Estilo de Explicação Preferido:\n");
        if (profile.getLearningStyle() != null) {
            switch (profile.getLearningStyle()) {
                case VISUAL:
                    systemMessage.append("- Use diagramas ASCII, Mermaid ou descrições visuais sempre que explicar arquitetura ou fluxo.\n");
                    systemMessage.append("- Use bullet points e formatação clara.\n");
                    break;
                case THEORETICAL:
                    systemMessage.append("- Comece pelos princípios fundamentais (First Principles).\n");
                    systemMessage.append("- Explique o 'porquê' antes do 'como'.\n");
                    break;
                case ANALOGICAL:
                    systemMessage.append("- Use metáforas do mundo real para explicar conceitos abstratos de programação.\n");
                    break;
                case PRACTICAL:
                default:
                    systemMessage.append("- Priorize exemplos de código práticos e executáveis.\n");
                    systemMessage.append("- Menos teoria, mais prática.\n");
                    break;
            }
        }

        // 4. Contexto do Usuário
        if (profile.getSummary() != null && !profile.getSummary().isEmpty()) {
            systemMessage.append("\n### Sobre o Usuário:\n").append(profile.getSummary()).append("\n");
        }

        return systemMessage.toString();
    }

    public String selectModel(UserProfileAI profile, boolean isComplexTask) {
        // Se o usuário tem uma preferência explícita, respeite-a
        if (profile.getPreferredModel() != null && !profile.getPreferredModel().isEmpty()) {
            return profile.getPreferredModel();
        }

        // Lógica de seleção automática (exemplo simplificado)
        // Em um cenário real, isso mapearia para nomes de modelos do provedor (ex: OpenAI, Anthropic)
        if (isComplexTask) {
            return "gpt-4-turbo"; // Ou equivalente de alta capacidade
        } else {
            return "gpt-3.5-turbo"; // Ou modelo mais rápido/barato
        }
    }
}
