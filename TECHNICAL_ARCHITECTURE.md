# Arquitetura Técnica Detalhada do MindForge

## 1. Filosofia e Princípios de Arquitetura

O MindForge foi concebido para ser mais do que uma API de CRUD. A visão é criar uma plataforma de produtividade e aprendizado que atua como um "segundo cérebro" proativo e inteligente. Para alcançar essa visão, a arquitetura foi guiada por princípios fundamentais que equilibram a velocidade de desenvolvimento com a manutenibilidade e flexibilidade a longo prazo.

1.  **Separation of Concerns (SoC):** A lógica de negócio (gerenciamento de dados) é mantida estritamente separada da lógica de integração com a IA. Mesmo dentro de um monólito, a camada de IA é desacoplada através de interfaces, garantindo que o código seja modular e fácil de entender.

2.  **Pragmatismo sobre Dogma:** A decisão de integrar a IA diretamente na aplicação Java, em vez de usar um microserviço Python, foi uma escolha pragmática para o estágio atual do projeto. Ela reduz a complexidade de infraestrutura e acelera o desenvolvimento, ao mesmo tempo que o design interno (Padrão Strategy/Provider) mantém a flexibilidade para trocar o modelo de IA no futuro.

3.  **Manutenibilidade e Evolução:** O projeto é um **Monólito Modular**. Os domínios de negócio (`study`, `project`, etc.) são bem definidos em pacotes separados, o que torna o sistema fácil de entender e manter. Esta modularidade é uma base sólida para uma futura extração para microserviços, se a complexidade e a escala justificarem.

4.  **Frontend Desacoplado (API-First):** A API foi projetada para ser consumida por qualquer cliente (web, mobile, desktop). Isso significa que o frontend é uma aplicação separada que se comunica exclusivamente através dos endpoints RESTful da API.

## 2. System Design e Fluxo de Dados Dinâmico

A arquitetura do MindForge é a de um **Monólito Modular** que orquestra chamadas para APIs externas. A melhor forma de entender a interação entre os componentes é através de um Diagrama de Sequência, que ilustra o fluxo de dados mais completo do sistema: a análise de um arquivo do GitHub com o ciclo de memória.

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend
    participant API as MindForge API
    participant GitHub
    participant AIProviders as AI Providers

    User->>+FE: 1. Clica em "Analisar Arquivo do GitHub"
    FE->>+API: 2. POST /api/ai/analyze/github-file
    
    API->>API: 3. Busca Token de Integração e URL no DB
    API->>+GitHub: 4. GET /repos/{owner}/{repo}/contents/{path}
    GitHub-->>-API: 5. Retorna conteúdo do arquivo
    
    API->>API: 6. Coleta contexto (Perfil, Nível, etc.)
    API->>API: 7. Constrói o Prompt de Análise
    
    API->>+AIProviders: 8. POST (Gemini ou Groq)
    Note over API,AIProviders: Lógica de orquestração decide o provedor/modelo
    AIProviders-->>-API: 9. Retorna o texto da análise
    
    API->>API: 10. Salva a conversa no DB
    API-->>-FE: 11. Retorna a resposta da IA
    FE-->>-User: 12. Exibe a análise

    par "Ciclo de Memória Assíncrono"
        API-)+AIProviders: 13. POST (Meta-Prompt e histórico)
        AIProviders-)-API: 14. Retorna Perfil do Usuário em JSON
        API-)+API: 15. Salva o perfil de usuário atualizado no DB
    end
```

## 3. O Padrão "AI Provider" e a Orquestração

Para evitar um acoplamento forte com um único modelo de IA, foi implementado o Padrão Strategy. O `AIService` depende de uma interface `AIProvider`, e o `GeminiProvider` e o `GroqProvider` são implementações concretas.

Isso foi elevado com um **serviço de orquestração** (`GroqOrchestratorService`), que adiciona uma camada de decisão, permitindo ao sistema:
-   Escolher o provedor mais adequado para a tarefa.
-   Selecionar modelos específicos dentro de um provedor (ex: um modelo rápido para tarefas simples, um modelo poderoso para análises complexas).
-   Implementar uma lógica de **fallback**, onde se a chamada para um modelo falhar, o sistema pode tentar novamente com um modelo secundário, aumentando a resiliência.

## 4. Anatomia do Cérebro de IA: Engenharia de Prompt e Memória

A "inteligência" do MindForge não vem do modelo de IA em si, mas da **orquestração e engenharia de prompt** realizadas pelo `AIService` em Java. Ele atua como um "diretor de cena", usando técnicas avançadas para controlar o comportamento da IA:

-   **Atribuição de Persona:** A IA é instruída a assumir diferentes papéis (`"Aja como um Tech Recruiter"`).
-   **Injeção de Contexto:** O prompt é enriquecido com dados do sistema (nível do usuário, perfil de memória).
-   **Instrução de Formato de Saída:** A IA é comandada a retornar a resposta em formatos específicos (Markdown, JSON).

### 4.1. Deep Dive: O Módulo de Memória (`MemoryService`)

O `MemoryService` é o que eleva a IA do MindForge de uma ferramenta de "pergunta e resposta" para um **mentor que aprende e se adapta**. Ele implementa um ciclo de aprendizado contínuo que permite à IA construir um modelo mental do usuário ao longo do tempo.

-   **Objetivo:** Criar uma memória persistente sobre o perfil de aprendizado do usuário (pontos fortes, fracos, interesses).
-   **Impacto na Experiência do Usuário:**
    -   **Personalização:** As respostas da IA se tornam cada vez mais personalizadas e cientes da jornada do usuário.
    -   **Continuidade:** O usuário sente que está continuando uma conversa com um mentor que o conhece.
-   **Componentes:**
    -   **`UserProfileAI` (Entidade):** O "dossiê" da IA sobre o usuário, armazenando um resumo e um JSON com dados estruturados.
    -   **`MemoryService` (Serviço):** Orquestra o ciclo de memória. Seu método `updateUserProfile` é **assíncrono (`@Async`)** para não impactar a latência da resposta principal.
-   **Fluxo do Ciclo de Memória (Meta-Análise):**
    1.  Após uma interação, o `MemoryService` é chamado em background.
    2.  Ele usa o `AIProvider` para fazer uma **"pergunta a si mesmo"**, enviando o histórico da conversa com um **meta-prompt** que instrui a IA a analisar a interação e extrair um perfil atualizado do usuário em formato JSON.
    3.  A IA retorna o JSON, que é salvo na entidade `UserProfileAI`, enriquecendo o contexto para a próxima interação.
-   **Trade-offs:** Esta abordagem gera uma chamada extra à API da IA (mitigada pelo `@Async`) e depende da qualidade do meta-prompt para funcionar bem.

## 5. Anatomia dos Bounded Contexts (Domínios de Negócio)

A API é organizada em "Bounded Contexts", um conceito do Domain-Driven Design (DDD), onde cada domínio tem suas próprias responsabilidades e modelos.

-   **`Study Context`:** Gerencia o progresso de aprendizado (`Subject`, `StudySession`).
-   **`Project Context`:** Gerencia o ciclo de vida de projetos (`Project`, `Milestone`).
-   **`Kanban Context`:** Gerencia o fluxo de trabalho (`KanbanBoard`, `KanbanColumn`, `KanbanTask`).
-   **`Knowledge Context`:** Serve como um "segundo cérebro" para anotações (`KnowledgeItem`).
-   **`Document Context`:** Abstrai o armazenamento de arquivos (`Document`).
-   **`AI & Integration Context`:** Orquestra toda a inteligência e comunicação externa (`ChatSession`, `UserProfileAI`, `UserIntegration`).

## 6. Justificativas Tecnológicas e Trade-offs

### 6.1. Escolha da Stack Java/Spring Boot
-   **Por Quê:** Robustez, ecossistema maduro e produtividade para aplicações backend complexas.
-   **Trade-offs:** Curva de aprendizado e overhead maiores em comparação com frameworks mais leves.

### 6.2. Escolha do Padrão "AI Provider" (Monólito)
-   **Por Quê:** Simplicidade de infraestrutura e foco na Engenharia de Prompt.
-   **Trade-offs:** Menor escalabilidade e perda de acesso ao ecossistema de bibliotecas de IA do Python.

### 6.3. Escolha dos Modelos de IA (Multi-Provider)
-   **Por Quê:**
    -   **Google Gemini:** Altas capacidades multimodais (essencial para OCR), acessibilidade e performance de ponta.
    -   **Groq:** Foco em alta velocidade (tokens/segundo) para modelos de linguagem open-source, ideal para interações que exigem baixa latência.
    -   A arquitetura de provedores permite usar o melhor de cada um, otimizando custo, velocidade e capacidade para diferentes tarefas.
-   **Trade-offs:** Aumento da complexidade na lógica de orquestração e necessidade de gerenciar múltiplas chaves de API e contratos.

### 6.4. Frontend Desacoplado (API-First)
-   **Por Quê:** Flexibilidade para escolher qualquer tecnologia de frontend e reutilização da API.
-   **Trade-offs:** Requer a construção de uma aplicação frontend separada.

## 7. Trade-offs Atuais e Próximas Atualizações

### 7.1. Autenticação e Perfil de Usuário (Trade-offs Atuais)
-   **Estado Atual:** O projeto opera como **single-user** com um `userId` fixo (`1L`). A complexidade de segurança e multi-tenancy foi conscientemente adiada para focar na lógica de IA.
-   **Trade-offs:** A API é aberta, e a personalização é limitada a um único perfil.

### 7.2. Próximas Atualizações (Roadmap)
1.  **Sistema de Autenticação e Autorização (Spring Security + JWT):** Proteger a API e permitir múltiplos usuários.
2.  **Criação de Perfil de Usuário e Workspaces:** Permitir que cada usuário tenha seus próprios dados e contextos.
3.  **Workspaces Colaborativos:** Habilitar a colaboração em projetos e estudos com gerenciamento de permissões.
4.  **Refinamento da Memória da IA:** Aprimorar os meta-prompts do `MemoryService` para um perfil de aprendizado ainda mais detalhado.

Este documento serve como um guia abrangente para a arquitetura do MindForge, detalhando suas escolhas de design, fluxos de dados e o caminho para sua evolução.
