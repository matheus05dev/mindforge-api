# Arquitetura do Módulo de Inteligência Artificial

Este documento detalha o funcionamento interno do "cérebro" do MindForge. A inteligência da plataforma não reside no modelo de IA em si, mas na **orquestração sofisticada** e na **engenharia de prompt contextual** realizadas pela API Java. O sistema foi projetado para transformar modelos de linguagem genéricos em um conjunto de **especialistas sob demanda**.

## Fluxo de Decisão e Processamento da IA

O diagrama abaixo ilustra como o `AIService` orquestra cada requisição, desde a seleção da personalidade até o ciclo de aprendizado contínuo.

```mermaid
flowchart TD
    Start([Requisição do Usuário]) --> Router{1. Roteador de Tarefas}

    %% Roteamento de Tarefas
    Router -- "Análise de Código" --> CodeAnalysis[Preparar Análise de Código]
    Router -- "Review de Portfólio" --> PortfolioReview[Preparar Review de Portfólio]
    Router -- "Product Thinking" --> ProductThinking[Preparar Análise de Produto]
    Router -- "Edição de Conteúdo" --> ContentEdit[Preparar Edição de Texto]
    Router -- "OCR / Transcrição" --> OCR[Preparar Transcrição de Imagem]
    Router -- "Pergunta Genérica" --> Generic[Preparar Resposta Genérica]

    %% Coleta de Contexto (Comum a quase todos)
    subgraph ContextEngine [2. Motor de Contexto]
        CodeAnalysis & PortfolioReview & ProductThinking & Generic --> FetchProfile[Buscar Perfil de Aprendizado (Memória)]
        CodeAnalysis & Generic --> FetchSubject[Buscar Nível de Proficiência (Domínio)]
        FetchProfile & FetchSubject --> ContextReady[Contexto Completo]
    end

    %% Seleção de Persona (Engenharia de Prompt)
    subgraph PromptEngine [3. Motor de Prompt]
        ContextReady --> PersonaSelector{Selecionar Persona}
    
        PersonaSelector -- "Modo: MENTOR" --> PromptMentor[Construir Prompt: Mentor Didático]
        PersonaSelector -- "Modo: ANALYST" --> PromptAnalyst[Construir Prompt: Analista Sênior]
        PersonaSelector -- "Modo: DEBUG" --> PromptDebug[Construir Prompt: Debug Assistant]
        PersonaSelector -- "Modo: SOCRATIC" --> PromptSocratic[Construir Prompt: Tutor Socrático]
        
        PortfolioReview --> PromptRecruiter[Construir Prompt: Tech Recruiter]
        ProductThinking --> PromptPM[Construir Prompt: Product Manager]
        ContentEdit --> PromptEditor[Construir Prompt: Editor de Texto]
        OCR --> PromptOCR[Construir Prompt: Transcrição]
    end

    %% Execução
    PromptMentor & PromptAnalyst & PromptDebug & PromptSocratic & PromptRecruiter & PromptPM & PromptEditor & PromptOCR --> AIProvider[4. AI Provider (Abstração)]
    
    subgraph "5. APIs Externas"
        AIProvider --> GeminiAPI[Google Gemini API]
        AIProvider --> GroqAPI[Groq API]
    end

    GeminiAPI --> AIProvider
    GroqAPI --> AIProvider
    
    AIProvider --> ResponseProcessor[6. Processar Resposta]

    %% Ciclo de Memória
    ResponseProcessor --> UserResponse([Retornar ao Usuário])
    ResponseProcessor -.-> AsyncMemory{7. Ciclo de Memória (Async)}
    
    subgraph MemoryCycle [Ciclo de Aprendizado]
        AsyncMemory --> MetaPrompt[Construir Meta-Prompt de Análise]
        MetaPrompt --> AIProviderMemory[AI Provider]
        AIProviderMemory --> GeminiAPIMemory[Google Gemini API]
        GeminiAPIMemory --> UpdateProfile[Atualizar Perfil do Usuário no DB]
    end
```

## Explicação Detalhada dos Componentes

### 1. Roteador de Tarefas
-   **O que é:** O ponto de entrada do `AIService`. Cada endpoint no `AIController` chama um método específico no serviço.
-   **Como atua:** Funciona como um `switch` inteligente. Ele identifica a **intenção** do usuário com base no endpoint que foi chamado (ex: `POST /api/ai/review/portfolio` significa que a intenção é "revisar um portfólio"). Isso permite que o sistema prepare um fluxo de processamento especializado para cada tipo de tarefa.

### 2. Motor de Contexto
-   **O que é:** Antes de construir a pergunta para a IA, o sistema coleta informações cruciais para enriquecer a interação.
-   **Como atua:** Ele busca dados de duas fontes principais:
    1.  **Contexto de Domínio:** Informações sobre o "aqui e agora", como o nível de proficiência (`ProficiencyLevel`) do usuário em um `Subject` específico.
    2.  **Contexto de Memória:** Informações sobre o histórico do usuário, buscando no `MemoryService` o `UserProfileAI` para entender seus pontos fortes e fracos já identificados em interações passadas.
-   **Por que é importante?** Sem contexto, a IA daria uma resposta genérica. Com contexto, ela pode adaptar a complexidade da resposta ao nível do usuário e focar nos seus gaps de conhecimento específicos.

### 3. Motor de Prompt (Engenharia de Prompt)
-   **O que é:** O coração da inteligência do MindForge. Esta camada é responsável por traduzir a intenção do usuário e o contexto coletado em um **prompt detalhado e otimizado** para o modelo de linguagem.
-   **Como atua:** Ele utiliza um conjunto de "receitas de prompt" (os métodos `build...Prompt`), onde cada receita aplica técnicas de engenharia de prompt:
    -   **Atribuição de Persona:** Define o papel que a IA deve assumir (`"Aja como um Tech Recruiter..."`).
    -   **Instrução de Tarefa:** Descreve a tarefa de forma inequívoca (`"Sua tarefa é analisar o README..."`).
    -   **Instrução de Formato:** Comanda o formato da saída (`"Forneça um feedback estruturado em 4 seções, usando Markdown..."`), o que é essencial para que a resposta possa ser processada pela aplicação.
-   **Por que é importante?** É aqui que um modelo de IA genérico é transformado em um conjunto de especialistas (Mentor, Analista, Agile Coach, etc.), garantindo que a resposta seja relevante, estruturada e de alta qualidade.

### 4. AI Provider (Camada de Abstração)
-   **O que é:** Uma implementação do Padrão de Projeto Strategy. O `AIService` não sabe com qual IA está falando; ele apenas se comunica com a interface `AIProvider`.
-   **Como atua:** Existem múltiplas implementações concretas (`GeminiProvider`, `GroqProvider`). Cada uma recebe um `AIProviderRequest` genérico e o traduz para o formato específico que sua respectiva API espera. O sistema pode escolher dinamicamente qual provedor usar.
-   **Por que é importante?** Garante a **flexibilidade e resiliência** do sistema. Se amanhã quisermos usar o modelo da OpenAI para uma tarefa, basta criar um `OpenAiProvider`. Se um provedor estiver indisponível, o sistema pode, em alguns cenários, usar outro.

### 5. APIs Externas (Google Gemini e Groq)
-   **O que são:** Os modelos de linguagem de grande escala (LLM) que executam a inferência.
-   **Provedores Atuais:**
    -   **Google Gemini:** Um modelo robusto e versátil.
    -   **Groq:** Um provedor conhecido por sua alta velocidade de inferência, que disponibiliza diversos modelos open-source (como Llama, Qwen, etc.).
-   **Por que é importante?** São os "motores" brutos. Nossa aplicação fornece o "chassi", o "volante" e o "GPS" (o prompt) para que a potência dos motores seja usada de forma útil e direcionada.

### 6. Orquestração de Provedores e Modelos
-   **O que é:** Uma camada de inteligência adicional que decide qual provedor e qual modelo usar para uma determinada tarefa.
-   **Como atua:** O `GroqOrchestratorService` é um exemplo dessa lógica. Ele permite:
    1.  **Seleção de Modelo:** Escolher um modelo específico da Groq (ex: `VERSATILE` para tarefas complexas, `INSTANT` para tarefas rápidas).
    2.  **Fallback Inteligente:** Tentar uma requisição com um modelo preferencial e, em caso de falha ou resposta insatisfatória, tentar novamente com um modelo de fallback.
-   **Por que é importante?** Isso aumenta a resiliência do sistema e otimiza custos e performance, usando o modelo certo para a tarefa certa.

### 7. Processador de Resposta
-   **O que é:** A lógica que recebe a resposta do `AIProvider`.
-   **Como atua:** Ele tem duas responsabilidades principais:
    1.  **Persistir a Conversa:** Salva a pergunta do usuário e a resposta da IA no banco de dados como `ChatMessage`, criando um histórico auditável.
    2.  **Retornar ao Usuário:** Envia a resposta para o `AIController`, que a devolve ao cliente.

### 8. Deep Dive: O Módulo de Memória (`MemoryService`)
O `MemoryService` eleva a IA de uma ferramenta de "pergunta e resposta" para um **mentor que aprende e se adapta**.

-   **Objetivo:** Criar uma memória persistente sobre o perfil de aprendizado do usuário.
-   **Fluxo do Ciclo de Memória (Meta-Análise):**
    1.  Após uma interação, o `MemoryService` é chamado de forma assíncrona (`@Async`).
    2.  Ele usa o `AIProvider` para fazer uma **"pergunta a si mesmo"**, enviando o histórico da conversa com um **meta-prompt** que instrui a IA a analisar a interação e extrair um perfil atualizado do usuário em formato JSON.
    3.  A IA retorna o JSON, que é salvo na entidade `UserProfileAI`, enriquecendo o contexto para a próxima interação.

### 9. A IA como um Agente Integrado no Ecossistema MindForge
A arquitetura foi projetada para que a IA evolua de uma ferramenta reativa para um **agente proativo e onipresente**.

-   **Análise de Conteúdo Passiva:** A IA pode analisar o conteúdo que o usuário produz para inferir seu nível de conhecimento e sugerir atualizações no seu perfil.
-   **Assistência Ativa e Proativa (Visão de Futuro):** A IA poderá **iniciar ações** com base em eventos do sistema, como criar um item de revisão para o usuário após ele completar várias tarefas sobre um mesmo tema.

### Conclusão

A arquitetura do MindForge permite que a IA atue em três níveis:
1.  **Reativo:** Responde a perguntas diretas com personalidades e contexto.
2.  **Analítico:** Analisa passivamente o conteúdo do usuário para inferir conhecimento e manter o perfil atualizado.
3.  **Proativo (Visão de Futuro):** Inicia ações e sugestões com base em eventos do sistema, agindo como um verdadeiro assistente que antecipa as necessidades do usuário.
    A flexibilidade de provedores e a orquestração de modelos garantem que o sistema possa sempre usar a melhor ferramenta para cada tarefa, de forma resiliente e eficiente.