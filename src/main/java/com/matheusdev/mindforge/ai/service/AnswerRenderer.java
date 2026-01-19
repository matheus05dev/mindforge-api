package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.service.model.AuditedAnswer;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnswerRenderer {

    public String renderToMarkdown(AuditedAnswer auditedAnswer, List<Evidence> evidences) {
        if (auditedAnswer == null || auditedAnswer.answer() == null || !StringUtils.hasText(auditedAnswer.answer().markdown())) {
            return "Não foi possível renderizar a resposta.";
        }

        StringBuilder markdownBuilder = new StringBuilder();
        markdownBuilder.append(auditedAnswer.answer().markdown()).append("\n\n");

        if (auditedAnswer.references() != null && !auditedAnswer.references().isEmpty() && evidences != null) {
            markdownBuilder.append("**Fontes:**\n");
            String references = auditedAnswer.references().stream()
                    .map(ref -> {
                        if (ref.evidenceId() > 0 && ref.evidenceId() <= evidences.size()) {
                            Evidence evidence = evidences.get(ref.evidenceId() - 1);
                            return String.format("- **Seção %s (p. %d):** *%s* (Score: %.2f)",
                                    evidence.section() != null ? evidence.section() : "N/A",
                                    evidence.page() != null ? evidence.page() : 0,
                                    evidence.excerpt(),
                                    evidence.score());
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            
            if (StringUtils.hasText(references)) {
                markdownBuilder.append(references);
            }
        }

        return markdownBuilder.toString();
    }
}
