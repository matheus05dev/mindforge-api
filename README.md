# MindForge API

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql)
![License](https://img.shields.io/badge/license-Private-red)
![Maven](https://img.shields.io/badge/Maven-3.8+-blue?logo=apache-maven)

**MindForge** é uma plataforma de produtividade e aprendizado de classe empresarial que atua como um "segundo cérebro" inteligente. A solução centraliza projetos, estudos e uma base de conhecimento estruturada, utilizando arquitetura moderna e integração sofisticada com Inteligência Artificial para transformar modelos de linguagem genéricos em mentores especializados e contextualmente conscientes.

---

## 🎯 Visão Geral do Projeto

O MindForge foi arquitetado para transcender as limitações das ferramentas de produtividade tradicionais. A proposta central é orquestrar modelos de IA de múltiplos provedores (Google Gemini e modelos open-source via Groq) através de **Engenharia de Prompt avançada** e **orquestração inteligente**, oferecendo assistência contextual e personalizada que evolui com o tempo.

O sistema foi projetado seguindo princípios de **Domain-Driven Design (DDD)** e padrões arquiteturais modernos, resultando em um monólito modular altamente manutenível, testável e preparado para escalar.

---

## 🛠️ Stack Tecnológica

### Core Framework e Linguagem

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=spring-boot) ![Spring Framework](https://img.shields.io/badge/Spring%20Framework-6.1.x-brightgreen?logo=spring)

- **Java 21** - Aproveitando recursos modernos como Records, Pattern Matching e Virtual Threads
- **Spring Boot 3.3.5** - Framework enterprise com suporte completo para Java 21
- **Spring Framework 6.1.x** - Ecossistema robusto para aplicações web modernas

### Persistência e Banco de Dados

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql&logoColor=white) ![JPA](https://img.shields.io/badge/JPA/Hibernate-6.0+-blue?logo=hibernate) ![Docker](https://img.shields.io/badge/Docker%20Compose-Latest-blue?logo=docker&logoColor=white)

- **PostgreSQL** - Sistema de gerenciamento de banco de dados relacional
- **JPA/Hibernate** - ORM para mapeamento objeto-relacional
- **Docker Compose** - Orquestração de containers para ambiente de desenvolvimento

### Integração e APIs

![Swagger](https://img.shields.io/badge/SpringDoc%20OpenAPI-2.6.0-green?logo=swagger) ![Gemini](https://img.shields.io/badge/Google%20Gemini-API-orange?logo=google) ![Groq](https://img.shields.io/badge/Groq%20API-Multiple%20Models-purple)

- **SpringDoc OpenAPI 2.6.0** - Documentação automática de API (Swagger UI)
- **RestTemplate/WebClient** - Clientes HTTP para integração com APIs externas
- **Google Gemini API** - Modelos de linguagem multimodal
- **Groq API** - Infraestrutura de IA de alta performance (6 modelos/agentes)

### Resiliência e Performance

![Resilience4j](https://img.shields.io/badge/Resilience4j-2.1.0-yellow) ![Caffeine](https://img.shields.io/badge/Caffeine%20Cache-High%20Performance-red) ![AOP](https://img.shields.io/badge/Spring%20AOP-Enabled-brightgreen)

- **Resilience4j 2.1.0** - Padrões de resiliência (Circuit Breaker, Retry, Rate Limiting)
- **Caffeine Cache** - Cache em memória de alta performance
- **Spring AOP** - Programação orientada a aspectos para concerns transversais

### Qualidade e Manutenibilidade

![MapStruct](https://img.shields.io/badge/MapStruct-1.5.5.Final-blue) ![Lombok](https://img.shields.io/badge/Lombok-1.18.30-pink) ![JaCoCo](https://img.shields.io/badge/JaCoCo-Code%20Coverage-yellow) ![JUnit](https://img.shields.io/badge/JUnit-5-green)

- **MapStruct 1.5.5.Final** - Mapper type-safe para DTOs
- **Lombok 1.18.30** - Redução de boilerplate
- **JaCoCo** - Cobertura de testes
- **JUnit 5** - Framework de testes

---

## 🏗️ Arquitetura e Padrões de Design

### Arquitetura Modular
O MindForge segue uma arquitetura de **Monólito Modular**, organizada em Bounded Contexts claramente definidos:
- Alta coesão e baixo acoplamento entre módulos
- Facilita manutenção e evolução incremental
- Base sólida para futura migração para microserviços, se necessário

### Padrões de Design Implementados

1. **Strategy Pattern (`AIProvider`)**
   - Abstração completa de provedores de IA
   - Permite múltiplos provedores coexistirem (Gemini, Groq)
   - Facilita adição de novos provedores sem impacto na lógica de negócio

2. **Domain-Driven Design (DDD)**
   - Bounded Contexts bem definidos
   - Modelagem rica de domínios
   - Separação clara de responsabilidades

3. **Repository Pattern**
   - Abstração de acesso a dados
   - Facilita testes e mudanças de persistência

4. **Service Layer Pattern**
   - Lógica de negócio isolada
   - Transações gerenciadas
   - Orquestração de operações complexas

5. **DTO Pattern com MapStruct**
   - Separação entre entidades de domínio e representação
   - Mapeamento type-safe e performático
   - Redução de vazamento de detalhes de implementação

### Resiliência e Tolerância a Falhas

O sistema incorpora múltiplos padrões de resiliência:
- **Circuit Breaker** - Proteção contra falhas em cascata
- **Retry** - Recuperação automática de falhas transitórias
- **Rate Limiting** - Controle de throughput para APIs externas
- **Time Limiter** - Proteção contra timeouts indefinidos
- **Fallback Strategy** - Alternativas automáticas em caso de falha

---

## ✨ Funcionalidades Principais

### Módulos de Produtividade

#### Gerenciamento de Projetos
- Criação e organização de projetos com estrutura hierárquica
- Sistema de marcos (milestones) para acompanhamento de progresso
- Integração nativa com repositórios do GitHub
- Análise automatizada de código via IA

#### Sistema Kanban
- Quadros visuais customizáveis
- Tarefas contextualizadas com projetos e estudos
- Sistema de colunas configurável
- Rastreamento de progresso visual

#### Gestão de Estudos
- Organização por assuntos (subjects)
- Níveis de proficiência por tópico
- Sessões de estudo rastreáveis
- Progresso personalizado e mensurável

#### Base de Conhecimento
- Centralização de anotações e documentos
- Sistema de tags para organização
- Upload e armazenamento de arquivos
- Busca e categorização inteligente

### Assistente de IA Multi-Contexto

#### Mentoria de Código
- Análise detalhada de código com feedback estruturado
- Múltiplas personas especializadas:
  - **Mentor**: Orientação didática e pedagógica
  - **Analista**: Análise técnica profunda
  - **Tutor Socrático**: Aprendizado guiado por perguntas
  - **Debug Assistant**: Identificação e resolução de problemas

#### Análise de Carreira
- Persona de **Recrutador Técnico** especializado
- Análise de projetos do GitHub para portfólio
- Feedback profissional sobre apresentação técnica
- Sugestões de melhorias e destacáveis

#### Planejamento Estratégico
- Criação de roadmaps de estudo personalizados
- Estruturação de projetos com metodologias ágeis
- Planejamento de sprint e milestones
- Análise de viabilidade e estimativas

#### Ferramentas de Conteúdo
- Resumo e síntese de textos
- Tradução entre idiomas
- Reescrita e otimização de conteúdo
- Extração de texto de imagens (OCR)
- Análise multimodal de conteúdo

### Memória e Personalização

O sistema implementa um **ciclo de memória assíncrono** que permite:
- Construção progressiva de perfil de aprendizado do usuário
- Personalização automática de respostas baseada em histórico
- Adaptação contínua a preferências e estilo do usuário
- Consistência eventual sem impacto na latência

---

## 🚀 Guia de Instalação e Execução

### Pré-requisitos

- **Java Development Kit (JDK) 21** ou superior
- **Docker Desktop** ou Docker Engine com Docker Compose
- **Maven 3.8+** (ou use o Maven Wrapper incluído)
- **API Key do Google AI Studio** (para integração com Gemini)
- **API Key da Groq** (para integração com Groq)

### Passo 1: Clone o Repositório

```bash
git clone <repository-url>
cd mindforge-api
```

### Passo 2: Configurar o Banco de Dados

Inicie o PostgreSQL usando Docker Compose:

```bash
docker-compose up -d
```

Isso irá iniciar um container PostgreSQL na porta `5432` com as seguintes credenciais padrão:
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `mindforge`
- **Username**: `mindforge`
- **Password**: `mindforge`

### Passo 3: Configurar API Keys

Edite o arquivo `src/main/resources/application.properties` e configure suas chaves de API:

```properties
# Google Gemini API
gemini.api.key=SUA_CHAVE_GEMINI_AQUI

# Groq API
groq.api.key=SUA_CHAVE_GROQ_AQUI
```

**Onde obter as chaves:**
- **Gemini**: [Google AI Studio](https://makersuite.google.com/app/apikey)
- **Groq**: [Groq Console](https://console.groq.com/)

### Passo 4: Compilar o Projeto

```bash
# Usando Maven Wrapper (recomendado)
./mvnw clean install

# Ou usando Maven instalado localmente
mvn clean install
```

### Passo 5: Executar a Aplicação

```bash
# Usando Maven Wrapper
./mvnw spring-boot:run

# Ou usando Maven instalado
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

### Passo 6: Acessar a Documentação

A documentação interativa da API está disponível através do Swagger UI:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **OpenAPI YAML**: [http://localhost:8080/v3/api-docs.yaml](http://localhost:8080/v3/api-docs.yaml)

---

## 📚 Documentação Adicional

Para uma compreensão mais profunda da arquitetura e decisões de design:

- **[Arquitetura Técnica Completa](TECHNICAL_ARCHITECTURE.md)** - Análise detalhada da arquitetura, padrões de design e fluxos de dados
- **[Arquitetura do Módulo de IA](AI_ARCHITECTURE.md)** - Deep dive na orquestração de IA, engenharia de prompt e ciclo de memória

---

## 🧪 Executando Testes

```bash
# Executar todos os testes
./mvnw test

# Executar testes com relatório de cobertura
./mvnw clean test jacoco:report

# Ver relatório de cobertura
open target/site/jacoco/index.html  # macOS/Linux
start target/site/jacoco/index.html # Windows
```

---

## 📦 Estrutura do Projeto

```
mindforge-api/
├── src/
│   ├── main/
│   │   ├── java/com/matheusdev/mindforge/
│   │   │   ├── ai/              # Módulo de IA e orquestração
│   │   │   ├── project/         # Contexto: Projetos
│   │   │   ├── study/           # Contexto: Estudos
│   │   │   ├── kanban/          # Contexto: Kanban
│   │   │   ├── knowledgeltem/   # Contexto: Base de Conhecimento
│   │   │   ├── workspace/       # Contexto: Workspaces
│   │   │   ├── document/        # Contexto: Documentos
│   │   │   ├── integration/     # Integrações externas
│   │   │   ├── exception/       # Tratamento de exceções
│   │   │   └── core/            # Configurações centrais
│   │   └── resources/
│   │       └── application.properties
│   └── test/                    # Testes unitários e de integração
├── docker-compose.yml           # Configuração do PostgreSQL
├── pom.xml                      # Dependências Maven
└── README.md                    # Este arquivo
```

---

## 🔒 Segurança e Boas Práticas

- **Validação de Entrada**: Validação em todas as camadas usando Bean Validation
- **Tratamento de Exceções**: Handler global centralizado com respostas padronizadas
- **Logging Estruturado**: Logging adequado para debugging e auditoria
- **Gestão de Dependências**: Versões atualizadas e monitoramento de vulnerabilidades

---

## 🛣️ Roadmap

- [ ] Sistema de autenticação e autorização (Spring Security + JWT)
- [ ] Suporte multi-usuário com workspaces
- [ ] Workspaces colaborativos com controle de permissões
- [ ] Refinamento avançado do ciclo de memória da IA
- [ ] Métricas e observabilidade (Micrometer, Prometheus)
- [ ] Testes de carga e otimização de performance
- [ ] CI/CD completo com deploy automatizado

---

## 📄 Licença

Este projeto é privado e de uso pessoal.

---

## 👤 Autor

**Matheus Dev**

---

## 🙏 Agradecimentos

Este projeto demonstra a aplicação de padrões modernos de engenharia de software e integração inteligente com IA, servindo como portfólio técnico e plataforma de aprendizado contínuo.
