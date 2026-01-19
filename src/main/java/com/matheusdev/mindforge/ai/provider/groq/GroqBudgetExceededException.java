package com.matheusdev.mindforge.ai.provider.groq;

/**
 * Exceção lançada quando o orçamento de tokens da API Groq é excedido.
 */
public class GroqBudgetExceededException extends RuntimeException {

    private final int remainingBudget;
    private final int requestedTokens;

    public GroqBudgetExceededException(int remainingBudget, int requestedTokens) {
        super(String.format(
                "Groq budget excedido. Disponível: %d tokens, Solicitado: %d tokens. Aguarde reset da janela (1 minuto).",
                remainingBudget, requestedTokens));
        this.remainingBudget = remainingBudget;
        this.requestedTokens = requestedTokens;
    }

    public int getRemainingBudget() {
        return remainingBudget;
    }

    public int getRequestedTokens() {
        return requestedTokens;
    }
}
