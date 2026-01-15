# MindForge API

**MindForge** é uma API de produtividade e aprendizado que atua como um "segundo cérebro" inteligente. A plataforma centraliza projetos, estudos e uma base de conhecimento, utilizando uma arquitetura robusta para integrar Inteligência Artificial de forma sofisticada, transformando modelos de linguagem genéricos em mentores especializados.

---

## 🚀 Visão Geral do Projeto

O MindForge foi criado para ir além das ferramentas de produtividade tradicionais. A proposta central é orquestrar modelos de IA (como Google Gemini e modelos open-source via Groq) através de **Engenharia de Prompt avançada** para oferecer assistência contextual e personalizada, ajudando o usuário a aprender, aprimorar seu código e planejar sua carreira.

Este projeto demonstra a aplicação de padrões de design modernos para criar um sistema flexível, manutenível e inteligente.

---

## 🛠️ Arquitetura e Tecnologias Aplicadas

Este projeto foi desenvolvido com foco em boas práticas de engenharia de software, demonstrando competência nas seguintes áreas:

-   **Linguagem e Framework:** **Java 21** com **Spring Boot 3**, aproveitando o ecossistema robusto para construir uma API RESTful segura e performática.
-   **Design de Software:**
    -   **Monólito Modular:** A aplicação é estruturada em domínios de negócio claros (Bounded Contexts), promovendo alta coesão e baixo acoplamento entre os módulos.
    -   **Domain-Driven Design (DDD):** Conceitos de DDD foram aplicados para modelar o domínio de forma rica e alinhada às regras de negócio.
    -   **Padrão Strategy (`AIProvider`):** A integração com a IA é feita através de uma interface que abstrai a implementação, permitindo a coexistência de múltiplos provedores (Gemini, Groq) e facilitando a troca ou adição de novos modelos sem impactar a lógica de negócio.
-   **Inteligência Artificial (Orquestração):**
    -   **Engenharia de Prompt:** O sistema utiliza prompts detalhados para transformar modelos de IA genéricos em especialistas, como mentores de código, recrutadores técnicos e gerentes de produto.
    -   **Multi-Provider e Orquestração:** O sistema pode escolher dinamicamente entre diferentes provedores de IA (Gemini para tarefas complexas, Groq para respostas rápidas) e possui uma lógica de fallback para aumentar a resiliência.
    -   **Memória Assíncrona:** Uma funcionalidade de "memória" permite que a IA aprenda com as interações do usuário de forma assíncrona, personalizando futuras respostas sem impactar a latência.
-   **Banco de Dados e Persistência:** **PostgreSQL** (gerenciado via **Docker Compose**) com **JPA/Hibernate**.
-   **Testes e Documentação:** A API é documentada com **Swagger (OpenAPI)**, facilitando a exploração e o teste dos endpoints.

Para uma análise aprofundada da arquitetura, consulte a **[Documentação Técnica Completa](TECHNICAL_ARCHITECTURE.md)**.

---

## ✨ Funcionalidades Implementadas

### Módulos Base de Produtividade

-   **Gerenciamento de Projetos:** Organização de projetos com marcos e integração com repositórios do **GitHub**.
-   **Kanban Inteligente:** Fluxo de trabalho visual onde tarefas podem ser contextualizadas com projetos e assuntos de estudo.
-   **Gerenciamento de Estudos:** Monitoramento de progresso de aprendizado com níveis de proficiência.
-   **Base de Conhecimento:** Centralização de anotações com suporte a tags e upload de arquivos.

### Assistente de IA Multi-Contexto

-   **Mentoria de Código:** Feedback detalhado com diferentes personas (Mentor, Analista, Tutor Socrático).
-   **Análise de Carreira:** Simula um **Recrutador Técnico** para analisar projetos do GitHub e dar feedback para portfólio.
-   **Planejamento Estratégico:** Ajuda a criar roadmaps de estudo e a estruturar projetos com metodologias ágeis.
-   **Ferramentas de Conteúdo:** Resume, traduz, reescreve textos e extrai conteúdo de imagens (OCR).

---

## 🚀 Como Rodar o Projeto

### Pré-requisitos
- **Java 21**
- **Docker** e **Docker Compose**
- **API Key do Google AI Studio** (para o Gemini)
- **API Key da Groq**

### 1. Subindo o Banco de Dados (Docker)
```bash
docker-compose up -d
```

### 2. Configurando as API Keys
No arquivo `src/main/resources/application.properties`, substitua os placeholders:
```properties
gemini.api.key=SUA_CHAVE_GEMINI_AQUI
groq.api.key=SUA_CHAVE_GROQ_AQUI
```

### 3. Executando a API
```bash
./mvnw spring-boot:run
```
A API estará disponível em `http://localhost:8080`.

### 4. Acessando a Documentação da API
A documentação interativa do Swagger UI está disponível em:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
