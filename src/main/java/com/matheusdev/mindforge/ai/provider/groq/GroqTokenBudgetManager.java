package com.matheusdev.mindforge.ai.provider.groq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerenciador de orçamento de tokens para a API Groq.
 * Rastreia o consumo de tokens em uma janela deslizante de 1 minuto
 * para garantir que o limite do free tier (6.000 TPM) não seja excedido.
 */
@Component
@Slf4j
public class GroqTokenBudgetManager {

    @Value("${groq.budget.tokens-per-minute:6000}")
    private int tokensPerMinute;

    @Value("${groq.budget.safety-margin:500}")
    private int safetyMargin;

    @Value("${groq.budget.enabled:true}")
    private boolean enabled;

    // Fila de registros de uso (timestamp + tokens)
    private final ConcurrentLinkedQueue<TokenUsageRecord> usageHistory = new ConcurrentLinkedQueue<>();

    // Contador de tokens consumidos na janela atual
    private final AtomicInteger currentUsage = new AtomicInteger(0);

    /**
     * Verifica se é possível consumir a quantidade estimada de tokens
     * sem exceder o limite do orçamento.
     *
     * @param estimatedTokens Quantidade estimada de tokens a consumir
     * @return true se há budget disponível, false caso contrário
     */
    public boolean canConsume(int estimatedTokens) {
        if (!enabled) {
            return true; // Budget desabilitado
        }

        cleanupOldRecords();

        int available = getAvailableBudget();
        boolean canConsume = estimatedTokens <= available;

        if (!canConsume) {
            log.warn("⚠️ Groq budget insuficiente. Necessário: {} tokens, Disponível: {}/{} tokens",
                    estimatedTokens, available, tokensPerMinute);
        } else {
            log.debug("✅ Budget disponível: {}/{} tokens (requisição: {} tokens)",
                    available, tokensPerMinute, estimatedTokens);
        }

        return canConsume;
    }

    /**
     * Registra o uso real de tokens após uma requisição bem-sucedida.
     *
     * @param actualTokens Quantidade real de tokens consumidos
     */
    public void recordUsage(int actualTokens) {
        if (!enabled) {
            return;
        }

        TokenUsageRecord record = new TokenUsageRecord(Instant.now(), actualTokens);
        usageHistory.offer(record);
        currentUsage.addAndGet(actualTokens);

        int remaining = getAvailableBudget();
        log.info("📊 Groq tokens consumidos: {} | Restante: {}/{} tokens",
                actualTokens, remaining, tokensPerMinute);
    }

    /**
     * Retorna o budget disponível (tokens restantes na janela atual).
     *
     * @return Quantidade de tokens disponíveis
     */
    public int getAvailableBudget() {
        if (!enabled) {
            return Integer.MAX_VALUE;
        }

        cleanupOldRecords();
        int effectiveLimit = tokensPerMinute - safetyMargin;
        return Math.max(0, effectiveLimit - currentUsage.get());
    }

    /**
     * Retorna o total de tokens consumidos na janela atual.
     *
     * @return Quantidade de tokens consumidos
     */
    public int getCurrentUsage() {
        cleanupOldRecords();
        return currentUsage.get();
    }

    /**
     * Remove registros mais antigos que 1 minuto e atualiza o contador.
     */
    private void cleanupOldRecords() {
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        int removedTokens = 0;

        while (!usageHistory.isEmpty()) {
            TokenUsageRecord oldest = usageHistory.peek();
            if (oldest != null && oldest.timestamp.isBefore(oneMinuteAgo)) {
                usageHistory.poll();
                removedTokens += oldest.tokens;
            } else {
                break;
            }
        }

        if (removedTokens > 0) {
            currentUsage.addAndGet(-removedTokens);
            log.debug("🔄 Budget resetado: {} tokens removidos da janela", removedTokens);
        }
    }

    /**
     * Reseta completamente o budget (útil para testes).
     */
    public void reset() {
        usageHistory.clear();
        currentUsage.set(0);
        log.info("🔄 Groq budget resetado completamente");
    }

    /**
     * Registro de uso de tokens com timestamp.
     */
    private static class TokenUsageRecord {
        final Instant timestamp;
        final int tokens;

        TokenUsageRecord(Instant timestamp, int tokens) {
            this.timestamp = timestamp;
            this.tokens = tokens;
        }
    }
}
