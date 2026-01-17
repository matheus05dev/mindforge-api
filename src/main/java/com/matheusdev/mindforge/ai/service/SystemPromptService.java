package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.memory.model.UserProfileAI;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.study.subject.model.Subject;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemPromptService {

    private static final String BASE_SYSTEM_PROMPT = "Você é um assistente de IA especialista em aprendizado e desenvolvimento de software. " +
            "Seu objetivo é ajudar o usuário a atingir seu potencial máximo, fornecendo respostas claras, educacionais e práticas. " +
            "Adapte sua linguagem e profundidade técnica com base no perfil do usuário fornecido.";

    public String buildSystemPrompt(UserProfileAI userProfile, Optional<Subject> subject, Optional<Project> project) {
        StringBuilder sb = new StringBuilder(BASE_SYSTEM_PROMPT);

        sb.append("\n\n--- CONTEXTO DO USUÁRIO ---\n");
        sb.append("Perfil Resumido: ").append(userProfile.getSummary()).append("\n");

        subject.ifPresent(s -> {
            sb.append("Assunto de Estudo Atual: ").append(s.getName()).append("\n");
            sb.append("Nível de Proficiência no Assunto: ").append(s.getProficiencyLevel()).append("\n");
        });

        project.ifPresent(p -> {
            sb.append("Projeto Atual: ").append(p.getName()).append("\n");
            sb.append("Descrição do Projeto: ").append(p.getDescription()).append("\n");
        });

        return sb.toString();
    }
}
