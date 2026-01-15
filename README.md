# MindForge API

**MindForge** é uma plataforma pessoal de produtividade e aprendizado, projetada para ser um "segundo cérebro" inteligente. Ela centraliza projetos, estudos, tarefas e uma base de conhecimento em uma API robusta, com uma arquitetura flexível e preparada para integração profunda com Inteligência Artificial.

---

## 💡 Por Que o MindForge? (A Motivação)

Em um mundo de ferramentas de produtividade genéricas, o MindForge nasceu da necessidade de um sistema que entendesse o fluxo de trabalho de quem aprende e constrói tecnologia. A inspiração veio de ferramentas como o Notion, mas com uma pergunta central: *"E se, além de organizar minhas anotações, a plataforma pudesse ativamente me ajudar a aprender, a programar melhor e a planejar minha carreira?"*

O objetivo não é criar um modelo de IA do zero, mas sim aplicar a **Engenharia de Prompt** de forma sofisticada para orquestrar modelos de linguagem existentes (como o Google Gemini) e transformá-los em mentores especializados. O MindForge é a prova de que a verdadeira inovação muitas vezes está na **aplicação inteligente** da tecnologia, e não apenas na sua criação.

---

## 🎯 Estado Atual e Trade-offs

-   **Ambiente:** Atualmente, o projeto está configurado para rodar **localmente** na máquina do desenvolvedor.
-   **Modelo de Usuário (Trade-off Atual):** É um sistema **single-user**. A lógica de autenticação e múltiplos usuários foi **conscientemente adiada** para focar na implementação das funcionalidades de IA. Isso significa que a API é aberta e usa um ID de usuário fixo (`1L`) como placeholder.
-   **Modelo de IA:** A integração com a IA é feita através de um padrão **AI Provider** dentro da aplicação Java. Esta foi uma decisão pragmática para simplificar a infraestrutura, evitando a necessidade de um microserviço Python e acelerando o desenvolvimento.

---

## ✨ Funcionalidades

A plataforma é construída sobre uma base sólida de gerenciamento de produtividade, enriquecida com um poderoso assistente de IA multi-personalidade.

### Módulos Base de Produtividade

-   **🗂️ Gerenciamento de Projetos:** Organize seus objetivos em projetos com marcos (milestones) e vincule-os diretamente a repositórios do **GitHub**.
-   **📋 Kanban Inteligente:** Gerencie seu fluxo de trabalho com um quadro Kanban onde as tarefas podem ser contextualizadas com `Projetos` e `Assuntos de Estudo`.
-   **🎓 Gerenciamento de Estudos:** Monitore seu progresso de aprendizado, registre sessões de estudo e classifique seu domínio em cada assunto com níveis de proficiência (Iniciante, Pleno, Sênior).
-   **🧠 Base de Conhecimento:** Centralize suas anotações, documentos e ideias em um só lugar, com suporte a tags e anexos de qualquer tipo de arquivo.
-   **📄 Gestão de Documentos:** Faça upload e associe qualquer tipo de arquivo (PDFs, imagens, código) a qualquer entidade do sistema.

### 🤖 Assistente de IA Multi-Contexto

O coração do MindForge é um assistente de IA que adota diferentes personalidades para fornecer assistência especializada e contextual.

-   **Análise de Código e Mentoria:**
    -   **Mentor & Analyst:** Fornece feedback de código detalhado, com foco em didática ou em análise técnica direta.
    -   **Debug Assistant:** Ajuda a encontrar a causa raiz de bugs e sugere correções.
    -   **Socratic Tutor:** Guia o aprendizado através de perguntas instigantes, em vez de dar respostas prontas.

-   **Carreira e Portfólio:**
    -   **Portfolio Reviewer:** Atua como um **Tech Recruiter**, analisando seu projeto do GitHub (README, estrutura) e fornecendo feedback para destacar seu trabalho em entrevistas e no LinkedIn.

-   **Estratégia e Planejamento:**
    -   **Study Architect:** Cria roadmaps de estudo personalizados com base em seus objetivos e gaps de conhecimento.
    -   **Agile Coach:** Ajuda a estruturar projetos usando a metodologia Scrum, sugerindo épicos e user stories.
    -   **Product Thinker:** Analisa ideias de funcionalidades e as estrutura do ponto de vista de um gerente de produto, com user stories, sugestões de UX e trade-offs técnicos.

-   **Ferramentas de Conteúdo:**
    -   **Editor de Texto:** Reescreve, resume, traduz e corrige a gramática de suas anotações.
    -   **Transcrição de Imagem (OCR):** Extrai texto de imagens (fotos de anotações, diagramas) e o adiciona à sua base de conhecimento.

-   **Memória e Personalização:** A IA aprende com suas interações, construindo um perfil de conhecimento para fornecer respostas cada vez mais personalizadas e cientes da sua jornada.

---

## 🏗️ Arquitetura e Documentação Técnica

O sistema foi projetado como um **Monólito Modular**, com uma camada de IA desacoplada através do padrão de projeto **Strategy (AI Provider)**. Esta abordagem equilibra a velocidade de desenvolvimento com a flexibilidade e manutenibilidade a longo prazo.

Para uma análise aprofundada da arquitetura, das decisões de design, dos fluxos de dados e da anatomia de cada Bounded Context, consulte a nossa **[Documentação Técnica Completa](TECHNICAL_ARCHITECTURE.md)**.

---

## 🚀 Como Rodar o Projeto

### Pré-requisitos
- **Java 21** instalado.
- **Docker** e **Docker Compose** instalados.
- Uma **API Key do Google AI Studio** (para o Gemini).

### 1. Subindo o Banco de Dados (Docker)
Na raiz do projeto, execute:
```bash
docker-compose up -d
```

### 2. Configurando a API Key
No arquivo `src/main/resources/application.properties`, substitua o placeholder `YOUR_GEMINI_API_KEY` pela sua chave real:
```properties
gemini.api.key=SUA_CHAVE_AQUI
```

### 3. Executando a API Java
Com o banco rodando e a chave configurada, inicie a aplicação Spring Boot:
```bash
./mvnw spring-boot:run
```
A API estará disponível em `http://localhost:8080`.

### 4. Acessando a Documentação da API
A documentação interativa do Swagger UI estará disponível em:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🗺️ Próximas Atualizações (Roadmap)

1.  **Segurança e Multi-usuário (Prioridade Alta):**
    -   Implementar Spring Security com JWT para proteger a API.
    -   Substituir o `userId` fixo por um sistema de contas de usuário, onde cada usuário tem seus próprios dados.

2.  **Workspaces e Colaboração:**
    -   Introduzir o conceito de "Workspaces" para que um usuário possa separar seus contextos (ex: "Trabalho", "Estudos Pessoais").
    -   Evoluir para permitir a colaboração em workspaces, com compartilhamento de projetos, anotações e hierarquia de permissões.

3.  **Construir o Frontend:**
    -   Desenvolver a interface de usuário para consumir a API e proporcionar uma experiência de uso fluida.

4.  **Refinar a Memória da IA:**
    -   Melhorar o ciclo de feedback para criar um perfil de usuário ainda mais detalhado e preciso, permitindo que a IA se lembre de interações passadas entre diferentes sessões.
