# MindForge - Arquitetura Técnica Profunda

## 1. Visão e Princípios de Arquitetura

O MindForge foi concebido para ser mais do que uma API de CRUD. A visão é criar uma plataforma de produtividade e aprendizado que atua como um "segundo cérebro" proativo e inteligente. Para alcançar essa visão, a arquitetura foi guiada por princípios fundamentais que equilibram a velocidade de desenvolvimento com a manutenibilidade e flexibilidade a longo prazo.

1.  **Separation of Concerns (SoC):** A lógica de negócio (gerenciamento de dados) é mantida estritamente separada da lógica de integração com a IA. Mesmo dentro de um monólito, a camada de IA é desacoplada através de interfaces, garantindo que o código seja modular e fácil de entender.

2.  **Pragmatismo sobre Dogma:** A decisão de integrar a IA diretamente na aplicação Java, em vez de usar um microserviço Python, foi uma escolha pragmática para o estágio atual do projeto. Ela reduz a complexidade de infraestrutura (menos serviços para gerenciar, menos deploy) e acelera o desenvolvimento, ao mesmo tempo que o design interno (Padrão Strategy/Provider) mantém a flexibilidade para trocar o modelo de IA no futuro.

3.  **Manutenibilidade e Evolução:** O projeto é um **Monólito Modular**. Os domínios de negócio (`study`, `project`, etc.) são bem definidos em pacotes separados, o que torna o sistema fácil de entender e manter. Esta modularidade é uma base sólida para uma futura extração para microserviços, se a complexidade e a escala justificarem.

4.  **Frontend Desacoplado (API-First):** A API foi projetada para ser consumida por qualquer cliente (web, mobile, desktop). Isso significa que o frontend é uma aplicação separada que se comunica exclusivamente através dos endpoints RESTful da API. Essa abordagem garante flexibilidade na escolha da tecnologia do frontend e permite que a API seja reutilizada por múltiplos clientes.

## 2. Arquitetura de Sistema: Monólito Modular com AI Provider

A arquitetura atual é a de um monólito modular. A comunicação com a Inteligência Artificial externa é abstraída através do **Padrão de Projeto Strategy**, implementado como um "AI Provider".

```mermaid
graph TD
    subgraph "Usuário Final"
        A[Cliente (Frontend/Mobile)]
    end

    subgraph "MindForge API (Java/Spring Boot)"
        B[API Layer (Controllers)]
        C[Business Logic Layer (Services)]
        D[Data Access Layer (Repositories)]
        E[Banco de Dados (PostgreSQL)]
        
        subgraph "AI Module"
            F[AIService]
            G[AIProvider (Interface)]
            H[GeminiProvider (Implementação)]
        end

        I[API Externa (Google Gemini)]
    end

    A -- "Requisições REST" --> B
    B --> C
    C --> D
    D --> E
    C -- "Usa o" --> F
    
    F -- "Depende da abstração" --> G
    G -- "É implementado por" --> H
    H -- "Chama via HTTP" --> I
```

### 2.1. O Padrão "AI Provider"

Para evitar um acoplamento forte com um único provedor de IA (como o Google Gemini), foi implementado um padrão de design que permite trocar o modelo de IA com impacto mínimo no resto da aplicação.

-   **`AIProvider` (Interface):** Define um contrato genérico que qualquer provedor de IA deve seguir (ex: `executeTask(AIProviderRequest)`).
-   **`GeminiProvider` (Implementação Concreta):** Uma classe que implementa a interface `AIProvider` e contém toda a lógica específica para se comunicar com a API do Google Gemini, incluindo a montagem dos DTOs (`GeminiRequest`, `GeminiResponse`) e o tratamento da resposta.
-   **`AIService` (O Consumidor):** O serviço principal de IA não depende diretamente do `GeminiProvider`. Ele depende da **interface** `AIProvider`. Isso significa que, no futuro, para trocar o Gemini pelo OpenAI, bastaria criar uma classe `OpenAiProvider` e alterar uma única linha na configuração do Spring, sem tocar em nenhuma linha de código do `AIService`.

Essa decisão de design foi crucial para manter a **flexibilidade arquitetural** mesmo dentro de um monólito.

## 3. Fluxo de Dados Completo da API

O MindForge opera como uma API RESTful, onde o cliente (frontend) inicia todas as interações.

1.  **Requisição do Cliente:** O frontend envia uma requisição HTTP (GET, POST, PUT, DELETE) para um endpoint específico da API (ex: `POST /api/ai/analyze/code`).
2.  **Camada de API (Controllers):** O `AIRestController` (ou outro Controller de domínio) recebe a requisição. Ele valida os dados de entrada usando anotações `@Valid` e DTOs específicos.
3.  **Camada de Lógica de Negócio (Services):** O Controller chama o método apropriado no `AIService` (ou outro Service de domínio). É aqui que a lógica de negócio é executada, incluindo:
    -   Busca de dados no banco de dados.
    -   Aplicação de regras de negócio.
    -   Orquestração de chamadas para outros serviços (ex: `AIService` chamando `MemoryService`).
    -   **Para requisições de IA:** O `AIService` constrói o prompt dinamicamente, injetando contexto do usuário e do sistema.
4.  **Camada de Acesso a Dados (Repositories):** Os Services interagem com os Repositórios (Spring Data JPA) para persistir ou recuperar dados do banco de dados.
5.  **Banco de Dados (PostgreSQL):** Armazena todos os dados da aplicação.
6.  **Resposta da API:** Após o processamento, o Service retorna um DTO para o Controller, que o encapsula em um `ResponseEntity` (com status HTTP apropriado) e o envia de volta ao cliente.

## 4. Fluxo Específico de Dados na IA: O Cérebro do MindForge

A inteligência do MindForge reside na orquestração e engenharia de prompt realizadas pelo `AIService` em Java, que utiliza o `AIProvider` para interagir com o modelo de IA.

1.  **Início da Interação (Ex: Análise de Código):**
    -   O `AIRestController` recebe uma requisição (ex: `POST /api/ai/analyze/code`) com o código a ser analisado e o `subjectId`.
    -   Ele chama `AIService.analyzeCodeForProficiency()`.
2.  **Coleta de Contexto (Java):**
    -   O `AIService` busca informações relevantes do banco de dados:
        -   `Subject`: Para obter o nome do assunto, `ProficiencyLevel` e `ProfessionalLevel`.
        -   `UserProfileAI`: Para obter o perfil de aprendizado do usuário (pontos fortes, fracos, etc.).
3.  **Engenharia de Prompt (Java):**
    -   Com base no `AnalysisMode` solicitado (Mentor, Analyst, Debug Assistant, Socratic Tutor) e no contexto coletado, o `AIService` constrói um **prompt altamente específico e detalhado**. Este prompt instrui a IA sobre sua persona, o formato da resposta esperada e injeta todo o contexto relevante.
4.  **Chamada ao AI Provider (Java):**
    -   O `AIService` cria um `AIProviderRequest` (contendo o prompt e, se for OCR, os dados da imagem em Base64).
    -   Ele chama `aiProvider.executeTask(request)`.
5.  **Comunicação com a API Externa (GeminiProvider):**
    -   O `GeminiProvider` recebe o `AIProviderRequest`.
    -   Ele traduz o `AIProviderRequest` para o formato específico da API do Google Gemini (`GeminiRequest`).
    -   Faz uma requisição HTTP POST para a API do Google Gemini (ex: `https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent`) com a API Key.
6.  **Processamento pelo Modelo de IA (Google Gemini):**
    -   O modelo Gemini recebe o prompt e gera uma resposta com base em seu treinamento e nas instruções do prompt.
7.  **Resposta do AI Provider (GeminiProvider):**
    -   O `GeminiProvider` recebe a `GeminiResponse` da API do Google.
    -   Ele extrai o texto gerado e o encapsula em um `AIProviderResponse` genérico.
8.  **Processamento da Resposta (Java):**
    -   O `AIService` recebe o `AIProviderResponse`.
    -   Salva a interação (prompt do usuário e resposta da IA) como `ChatMessage` em uma `ChatSession` no banco de dados.
    -   Retorna a `ChatMessage` com a resposta da IA para o `AIRestController`, que a envia de volta ao cliente.
9.  **Ciclo de Memória (Assíncrono):**
    -   Em paralelo, o `AIService` dispara uma chamada assíncrona para o `MemoryService`.
    -   O `MemoryService` usa o `AIProvider` novamente, mas com um **meta-prompt** que instrui a IA a analisar a conversa recente e atualizar o `UserProfileAI` do usuário no banco de dados.

## 5. Justificativas Tecnológicas e Trade-offs

### 5.1. Escolha da Stack Java/Spring Boot

-   **Por Quê:**
    -   **Robustez e Ecossistema:** Java e Spring Boot são líderes de mercado para aplicações corporativas, oferecendo um ecossistema maduro, ferramentas robustas e alta performance para o backend.
    -   **Produtividade:** Spring Boot acelera o desenvolvimento com auto-configuração e convenções.
    -   **Manutenibilidade:** A tipagem forte do Java e as convenções do Spring promovem código mais legível e fácil de manter.
-   **Trade-offs:**
    -   **Curva de Aprendizagem:** Pode ser mais íngreme para iniciantes comparado a frameworks mais leves.
    -   **Overhead:** Para aplicações muito pequenas, o Spring Boot pode ter um overhead de memória e inicialização maior.

### 5.2. Escolha do Padrão "AI Provider" (Monólito)

-   **Por Quê:**
    -   **Simplicidade de Infraestrutura:** Elimina a complexidade de gerenciar um segundo serviço (Python), dois deploys, comunicação inter-serviços, etc. Ideal para um projeto de portfólio onde o foco é a funcionalidade, não a complexidade de DevOps.
    -   **Flexibilidade Interna:** O padrão Strategy (`AIProvider` interface) permite trocar o provedor de IA (Gemini, OpenAI, Anthropic) com uma mudança mínima de código (apenas a implementação do provedor e a configuração do Spring).
    -   **Foco na Engenharia de Prompt:** Permite concentrar o esforço na criação de prompts inteligentes e no `AIService` em Java, que detém todo o contexto de negócio.
-   **Trade-offs:**
    -   **Escalabilidade:** O processamento da IA (que pode ser intensivo) escala junto com toda a aplicação Java. Se a IA se tornar um gargalo, será necessário escalar todo o monólito, o que é menos eficiente do que escalar um microserviço de IA dedicado.
    -   **Ecossistema de IA:** Perde-se o acesso direto ao rico ecossistema de bibliotecas Python para IA (LangChain, PyTorch, etc.), que poderiam simplificar certas tarefas de pré/pós-processamento de texto ou integração com modelos mais complexos.

### 5.3. Escolha do Modelo de IA (Google Gemini)

-   **Por Quê:**
    -   **Capacidades Multimodais:** O Gemini é um modelo multimodal, o que é crucial para funcionalidades como a transcrição de imagens (OCR).
    -   **Acessibilidade:** O Google AI Studio oferece uma plataforma acessível para experimentação e obtenção de API Keys.
    -   **Performance:** Modelos de ponta como o Gemini oferecem alta qualidade de resposta para as tarefas de Engenharia de Prompt definidas.
-   **Trade-offs:**
    -   **Custo:** O uso da API pode gerar custos, dependendo do volume de requisições.
    -   **Dependência Externa:** A aplicação depende da disponibilidade e das políticas de uso da API do Google.

### 5.4. Frontend Desacoplado (API-First)

-   **Por Quê:**
    -   **Flexibilidade:** Permite a escolha de qualquer tecnologia de frontend (React, Vue, Angular, mobile nativo) sem impactar o backend.
    -   **Reutilização:** A mesma API pode servir a múltiplos clientes (web, mobile).
    -   **Separação de Equipes:** Facilita o trabalho de equipes de backend e frontend de forma independente.
-   **Trade-offs:**
    -   **Complexidade Inicial:** Requer a construção de uma aplicação frontend separada, o que adiciona um projeto ao escopo.
    -   **Comunicação:** Exige um bom design de API para garantir que o frontend tenha todas as informações necessárias.

## 6. Trade-offs Atuais e Próximas Atualizações

### 6.1. Autenticação e Perfil de Usuário (Trade-offs Atuais)

-   **Estado Atual:** O projeto opera como **single-user** com um `userId` fixo (`1L`). Não há sistema de autenticação ou criação de perfil de usuário.
-   **Por Quê:** Esta decisão foi tomada para simplificar o desenvolvimento inicial e focar na lógica de negócio e IA. A complexidade de segurança e multi-tenancy foi conscientemente adiada.
-   **Trade-offs:**
    -   **Segurança:** A API é completamente aberta, o que é inaceitável para um ambiente de produção.
    -   **Personalização Limitada:** Embora a IA tenha um perfil de aprendizado, ele é associado a um ID fixo, não a um usuário real.
    -   **Experiência do Usuário:** Não há como usuários diferentes terem seus próprios dados ou perfis de IA.

### 6.2. Próximas Atualizações (Roadmap)

1.  **Sistema de Autenticação e Autorização (Spring Security + JWT):**
    -   **Objetivo:** Proteger a API e permitir múltiplos usuários.
    -   **Impacto:** Cada requisição será associada a um usuário autenticado, e o `userId` fixo será substituído pelo ID do usuário logado.

2.  **Criação de Perfil de Usuário e Workspaces:**
    -   **Objetivo:** Permitir que cada usuário tenha seus próprios dados e contextos.
    -   **Impacto:** Entidades como `Project`, `Subject`, `KnowledgeItem` serão associadas a um `User`. A IA terá um `UserProfileAI` distinto para cada usuário.

3.  **Workspaces Colaborativos (Projetos e Estudos em Conjunto):**
    -   **Objetivo:** Habilitar a colaboração em projetos e estudos.
    -   **Impacto:** Introdução de modelos de `Team` ou `Workspace` e gerenciamento de permissões (hierarquia de acesso). Isso permitiria o compartilhamento de anotações e a colaboração em projetos, com a IA atuando como um assistente para o grupo.

4.  **Refinamento da Memória da IA:**
    -   **Objetivo:** Tornar o perfil de aprendizado da IA ainda mais detalhado e preciso.
    -   **Impacto:** Aprimorar os meta-prompts do `MemoryService` e talvez introduzir um sistema de "tags de memória" para a IA registrar marcos de aprendizado específicos do usuário.

Este documento serve como um guia abrangente para a arquitetura do MindForge, detalhando suas escolhas de design, fluxos de dados e o caminho para sua evolução.
