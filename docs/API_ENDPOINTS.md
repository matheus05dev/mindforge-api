# Documentação de Endpoints - MindForge API

Este documento lista todos os endpoints da API com exemplos de entrada e saída.

**Base URL:** `http://localhost:8080`

---

## 📁 Workspaces

### GET `/api/workspaces`
Lista todos os workspaces.

**Resposta:**
```json
[
  {
    "id": 1,
    "name": "Meu Workspace",
    "description": "Descrição do workspace",
    "type": "PROJECT"
  }
]
```

**Tipos de Workspace:** `PROJECT`, `STUDY`, `GENERIC`

---

### POST `/api/workspaces`
Cria um novo workspace.

**Entrada:**
```json
{
  "name": "Meu Workspace",
  "description": "Descrição do workspace",
  "type": "PROJECT"
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "Meu Workspace",
  "description": "Descrição do workspace",
  "type": "PROJECT"
}
```

---

### GET `/api/workspaces/{id}`
Busca um workspace por ID.

**Resposta:**
```json
{
  "id": 1,
  "name": "Meu Workspace",
  "description": "Descrição do workspace",
  "type": "PROJECT"
}
```

---

## 🚀 Projects

### GET `/api/projects`
Lista todos os projetos.

**Resposta:**
```json
[
  {
    "id": 1,
    "name": "Meu Projeto",
    "description": "Descrição do projeto",
    "documents": []
  }
]
```

---

### GET `/api/projects/{projectId}`
Busca um projeto por ID.

**Resposta:**
```json
{
  "id": 1,
  "name": "Meu Projeto",
  "description": "Descrição do projeto",
  "documents": []
}
```

---

### POST `/api/projects`
Cria um novo projeto.

**Entrada:**
```json
{
  "workspaceId": 1,
  "name": "Meu Projeto",
  "description": "Descrição do projeto"
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "Meu Projeto",
  "description": "Descrição do projeto",
  "documents": []
}
```

---

### PUT `/api/projects/{projectId}`
Atualiza um projeto existente.

**Entrada:**
```json
{
  "workspaceId": 1,
  "name": "Projeto Atualizado",
  "description": "Nova descrição"
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "Projeto Atualizado",
  "description": "Nova descrição",
  "documents": []
}
```

---

### DELETE `/api/projects/{projectId}`
Deleta um projeto.

**Resposta:** `204 No Content`

---

### POST `/api/projects/{projectId}/link`
Vincula um repositório do GitHub a um projeto.

**Entrada:**
```json
{
  "repoUrl": "https://github.com/usuario/repositorio"
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "Meu Projeto",
  "description": "Descrição do projeto",
  "documents": []
}
```

---

## 🎯 Milestones

### POST `/api/projects/{projectId}/milestones`
Adiciona um milestone a um projeto.

**Entrada:**
```json
{
  "title": "Sprint 1",
  "description": "Primeira sprint do projeto",
  "dueDate": "2024-12-31",
  "completed": false
}
```

**Resposta:**
```json
{
  "id": 1,
  "projectId": 1,
  "title": "Sprint 1",
  "description": "Primeira sprint do projeto",
  "dueDate": "2024-12-31",
  "completed": false
}
```

---

### PUT `/api/projects/milestones/{milestoneId}`
Atualiza um milestone.

**Entrada:**
```json
{
  "title": "Sprint 1 - Atualizado",
  "description": "Descrição atualizada",
  "dueDate": "2024-12-31",
  "completed": true
}
```

**Resposta:**
```json
{
  "id": 1,
  "projectId": 1,
  "title": "Sprint 1 - Atualizado",
  "description": "Descrição atualizada",
  "dueDate": "2024-12-31",
  "completed": true
}
```

---

### DELETE `/api/projects/milestones/{milestoneId}`
Deleta um milestone.

**Resposta:** `204 No Content`

---

## 📚 Studies

### GET `/api/studies/subjects`
Lista todos os assuntos de estudo.

**Resposta:**
```json
[
  {
    "id": 1,
    "name": "Java",
    "description": "Programação em Java",
    "proficiencyLevel": "INTERMEDIATE",
    "professionalLevel": "PLENO",
    "studySessions": [
      {
        "id": 1,
        "subjectId": 1,
        "subjectName": "Java",
        "startTime": "2024-01-15T10:00:00",
        "durationMinutes": 120,
        "notes": "Estudei sobre streams e lambdas",
        "documents": []
      }
    ]
  }
]
```

**Campos:**
- `id` (Long): ID único do assunto
- `name` (String): Nome do assunto
- `description` (String, opcional): Descrição do assunto
- `proficiencyLevel` (Enum, opcional): Nível de proficiência
- `professionalLevel` (Enum, opcional): Nível profissional
- `studySessions` (List): Lista de sessões de estudo relacionadas

**Níveis de Proficiência:** `BEGINNER`, `INTERMEDIATE`, `ADVANCED`  
**Níveis Profissionais:** `JUNIOR`, `PLENO`, `SENIOR`

---

### GET `/api/studies/subjects/{subjectId}`
Busca um assunto por ID.

**Parâmetros:**
- `subjectId` (path): ID do assunto

**Resposta:**
```json
{
  "id": 1,
  "name": "Java",
  "description": "Programação em Java",
  "proficiencyLevel": "INTERMEDIATE",
  "professionalLevel": "PLENO",
  "studySessions": [
    {
      "id": 1,
      "subjectId": 1,
      "subjectName": "Java",
      "startTime": "2024-01-15T10:00:00",
      "durationMinutes": 120,
      "notes": "Estudei sobre streams e lambdas",
      "documents": []
    }
  ]
}
```

**Códigos de Status:**
- `200`: Sucesso
- `404`: Assunto não encontrado

---

### POST `/api/studies/subjects`
Cria um novo assunto de estudo.

**Entrada:**
```json
{
  "name": "Java",
  "description": "Programação em Java",
  "proficiencyLevel": "INTERMEDIATE",
  "professionalLevel": "PLENO"
}
```

**Campos Obrigatórios:**
- `name` (String): Nome do assunto

**Campos Opcionais:**
- `description` (String): Descrição do assunto
- `proficiencyLevel` (Enum): Nível de proficiência (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`)
- `professionalLevel` (Enum): Nível profissional (`JUNIOR`, `PLENO`, `SENIOR`)

**Resposta:**
```json
{
  "id": 1,
  "name": "Java",
  "description": "Programação em Java",
  "proficiencyLevel": "INTERMEDIATE",
  "professionalLevel": "PLENO",
  "studySessions": []
}
```

**Códigos de Status:**
- `200`: Assunto criado com sucesso
- `400`: Dados inválidos

---

### PUT `/api/studies/subjects/{subjectId}`
Atualiza um assunto de estudo.

**Parâmetros:**
- `subjectId` (path): ID do assunto a ser atualizado

**Entrada:**
```json
{
  "name": "Java Avançado",
  "description": "Programação avançada em Java",
  "proficiencyLevel": "ADVANCED",
  "professionalLevel": "SENIOR"
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "Java Avançado",
  "description": "Programação avançada em Java",
  "proficiencyLevel": "ADVANCED",
  "professionalLevel": "SENIOR",
  "studySessions": []
}
```

**Códigos de Status:**
- `200`: Assunto atualizado com sucesso
- `404`: Assunto não encontrado
- `400`: Dados inválidos

---

### DELETE `/api/studies/subjects/{subjectId}`
Deleta um assunto de estudo.

**Parâmetros:**
- `subjectId` (path): ID do assunto a ser deletado

**Resposta:** `204 No Content`

**Códigos de Status:**
- `204`: Assunto deletado com sucesso
- `404`: Assunto não encontrado

---

## 📖 Study Sessions

### POST `/api/studies/subjects/{subjectId}/sessions`
Registra uma nova sessão de estudo.

**Parâmetros:**
- `subjectId` (path): ID do assunto ao qual a sessão pertence

**Entrada:**
```json
{
  "startTime": "2024-01-15T10:00:00",
  "durationMinutes": 120,
  "notes": "Estudei sobre streams e lambdas"
}
```

**Campos Obrigatórios:**
- `startTime` (LocalDateTime): Data e hora de início da sessão (formato ISO: `YYYY-MM-DDTHH:mm:ss`)
- `durationMinutes` (Integer): Duração da sessão em minutos (deve ser positivo)

**Campos Opcionais:**
- `notes` (String): Notas sobre a sessão de estudo

**Resposta:**
```json
{
  "id": 1,
  "subjectId": 1,
  "subjectName": "Java",
  "startTime": "2024-01-15T10:00:00",
  "durationMinutes": 120,
  "notes": "Estudei sobre streams e lambdas",
  "documents": []
}
```

**Campos da Resposta:**
- `id` (Long): ID único da sessão
- `subjectId` (Long): ID do assunto
- `subjectName` (String): Nome do assunto
- `startTime` (LocalDateTime): Data e hora de início
- `durationMinutes` (Integer): Duração em minutos
- `notes` (String, opcional): Notas da sessão
- `documents` (List): Lista de documentos associados

**Códigos de Status:**
- `200`: Sessão registrada com sucesso
- `400`: Dados inválidos
- `404`: Assunto não encontrado

---

### PUT `/api/studies/sessions/{sessionId}`
Atualiza uma sessão de estudo.

**Parâmetros:**
- `sessionId` (path): ID da sessão a ser atualizada

**Entrada:**
```json
{
  "startTime": "2024-01-15T10:00:00",
  "durationMinutes": 150,
  "notes": "Estudei sobre streams, lambdas e optional"
}
```

**Resposta:**
```json
{
  "id": 1,
  "subjectId": 1,
  "subjectName": "Java",
  "startTime": "2024-01-15T10:00:00",
  "durationMinutes": 150,
  "notes": "Estudei sobre streams, lambdas e optional",
  "documents": []
}
```

**Códigos de Status:**
- `200`: Sessão atualizada com sucesso
- `404`: Sessão não encontrada
- `400`: Dados inválidos

---

### DELETE `/api/studies/sessions/{sessionId}`
Deleta uma sessão de estudo.

**Parâmetros:**
- `sessionId` (path): ID da sessão a ser deletada

**Resposta:** `204 No Content`

**Códigos de Status:**
- `204`: Sessão deletada com sucesso
- `404`: Sessão não encontrada

---

## 📝 Knowledge Base

### GET `/api/knowledge`
Lista todos os itens de conhecimento.

**Resposta:**
```json
[
  {
    "id": 1,
    "title": "Java Best Practices",
    "content": "Use streams para manipular coleções",
    "tags": ["java", "best-practices"],
    "documents": []
  }
]
```

---

### GET `/api/knowledge/{id}`
Busca um item de conhecimento por ID.

**Resposta:**
```json
{
  "id": 1,
  "title": "Java Best Practices",
  "content": "Use streams para manipular coleções",
  "tags": ["java", "best-practices"],
  "documents": []
}
```

---

### POST `/api/knowledge`
Cria um novo item de conhecimento.

**Entrada:**
```json
{
  "title": "Java Best Practices",
  "content": "Use streams para manipular coleções",
  "tags": ["java", "best-practices"]
}
```

**Resposta:**
```json
{
  "id": 1,
  "title": "Java Best Practices",
  "content": "Use streams para manipular coleções",
  "tags": ["java", "best-practices"],
  "documents": []
}
```

---

### PUT `/api/knowledge/{id}`
Atualiza um item de conhecimento.

**Entrada:**
```json
{
  "title": "Java Best Practices - Atualizado",
  "content": "Use streams e optional para manipular coleções",
  "tags": ["java", "best-practices", "optional"]
}
```

**Resposta:**
```json
{
  "id": 1,
  "title": "Java Best Practices - Atualizado",
  "content": "Use streams e optional para manipular coleções",
  "tags": ["java", "best-practices", "optional"],
  "documents": []
}
```

---

### DELETE `/api/knowledge/{id}`
Deleta um item de conhecimento.

**Resposta:** `204 No Content`

---

### GET `/api/knowledge/search?tag={tag}`
Busca itens de conhecimento por tag.

**Exemplo:** `GET /api/knowledge/search?tag=java`

**Resposta:**
```json
[
  {
    "id": 1,
    "title": "Java Best Practices",
    "content": "Use streams para manipular coleções",
    "tags": ["java", "best-practices"],
    "documents": []
  }
]
```

---

## 📋 Kanban

### GET `/api/kanban/board`
Retorna o quadro Kanban completo (todas as colunas e tarefas).

**Resposta:**
```json
[
  {
    "id": 1,
    "name": "To Do",
    "position": 0,
    "tasks": [
      {
        "id": 1,
        "title": "Implementar feature X",
        "description": "Descrição da tarefa",
        "position": 0,
        "columnId": 1,
        "subjectId": 1,
        "subjectName": "Java",
        "projectId": 1,
        "projectName": "Meu Projeto",
        "documents": []
      }
    ]
  }
]
```

---

### POST `/api/kanban/columns`
Cria uma nova coluna no Kanban.

**Entrada:**
```json
{
  "name": "To Do",
  "position": 0
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "To Do",
  "position": 0,
  "tasks": []
}
```

---

### PUT `/api/kanban/columns/{columnId}`
Atualiza uma coluna.

**Entrada:**
```json
{
  "name": "A Fazer",
  "position": 0
}
```

**Resposta:**
```json
{
  "id": 1,
  "name": "A Fazer",
  "position": 0,
  "tasks": []
}
```

---

### DELETE `/api/kanban/columns/{columnId}`
Deleta uma coluna.

**Resposta:** `204 No Content`

---

### POST `/api/kanban/columns/{columnId}/tasks`
Cria uma nova tarefa em uma coluna.

**Entrada:**
```json
{
  "title": "Implementar feature X",
  "description": "Descrição da tarefa",
  "position": 0,
  "subjectId": 1,
  "projectId": 1
}
```

**Resposta:**
```json
{
  "id": 1,
  "title": "Implementar feature X",
  "description": "Descrição da tarefa",
  "position": 0,
  "columnId": 1,
  "subjectId": 1,
  "subjectName": "Java",
  "projectId": 1,
  "projectName": "Meu Projeto",
  "documents": []
}
```

---

### PUT `/api/kanban/tasks/{taskId}`
Atualiza uma tarefa.

**Entrada:**
```json
{
  "title": "Implementar feature X - Atualizado",
  "description": "Nova descrição",
  "position": 0,
  "subjectId": 1,
  "projectId": 1
}
```

**Resposta:**
```json
{
  "id": 1,
  "title": "Implementar feature X - Atualizado",
  "description": "Nova descrição",
  "position": 0,
  "columnId": 1,
  "subjectId": 1,
  "subjectName": "Java",
  "projectId": 1,
  "projectName": "Meu Projeto",
  "documents": []
}
```

---

### PUT `/api/kanban/tasks/{taskId}/move/{targetColumnId}`
Move uma tarefa para outra coluna.

**Resposta:**
```json
{
  "id": 1,
  "title": "Implementar feature X",
  "description": "Descrição da tarefa",
  "position": 0,
  "columnId": 2,
  "subjectId": 1,
  "subjectName": "Java",
  "projectId": 1,
  "projectName": "Meu Projeto",
  "documents": []
}
```

---

### DELETE `/api/kanban/tasks/{taskId}`
Deleta uma tarefa.

**Resposta:** `204 No Content`

---

## 📄 Documents

### POST `/api/documents/upload`
Faz upload de um documento.

**Formato:** `multipart/form-data`

**Parâmetros:**
- `file` (obrigatório): Arquivo a ser enviado
- `projectId` (opcional): ID do projeto
- `kanbanTaskId` (opcional): ID da tarefa do Kanban
- `knowledgeItemId` (opcional): ID do item de conhecimento
- `studySessionId` (opcional): ID da sessão de estudo

**Resposta:**
```json
{
  "id": 1,
  "fileName": "documento.pdf",
  "fileType": "application/pdf",
  "downloadUri": "/api/documents/download/documento.pdf",
  "uploadDate": "2024-01-15T10:00:00"
}
```

---

### GET `/api/documents/download/{fileName}`
Baixa um documento pelo nome do arquivo.

**Resposta:** Arquivo binário (download)

---

## 🤖 AI Assistant

### POST `/api/ai/analyze/code`
Analisa código enviado diretamente.

**Entrada:**
```json
{
  "subjectId": 1,
  "codeToAnalyze": "public class Test { ... }",
  "documentId": null,
  "mode": "MENTOR"
}
```

**Modos de Análise:**
- `MENTOR`: Modo didático e guiado (padrão)
- `ANALYST`: Modo direto e sincero
- `DEBUG_ASSISTANT`: Focado em encontrar e corrigir bugs
- `SOCRATIC_TUTOR`: Focado em fazer perguntas para guiar o aprendizado

**Resposta:**
```json
{
  "id": 1,
  "role": "assistant",
  "content": "Análise do código...",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### POST `/api/ai/analyze/github-file`
Analisa um arquivo do GitHub vinculado a um projeto.

**Entrada:**
```json
{
  "projectId": 1,
  "filePath": "src/main/java/com/example/Service.java",
  "mode": "MENTOR"
}
```

**Resposta:**
```json
{
  "id": 1,
  "role": "assistant",
  "content": "Análise do arquivo...",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### POST `/api/ai/analyze/generic`
Análise genérica de conhecimento (qualquer área).

**Entrada:**
```json
{
  "question": "Como funciona o algoritmo de ordenação quicksort?",
  "subjectId": 1,
  "projectId": null,
  "provider": "ollama"
}
```

**Provedores:** `ollama`, `groq` ou `null` (usa "mindforge" - padrão)

**Resposta:**
```json
{
  "id": 1,
  "role": "assistant",
  "content": "Resposta da IA...",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### POST `/api/ai/edit/knowledge-item/{itemId}`
Modifica o conteúdo de um item de conhecimento usando IA.

**Entrada:**
```json
{
  "instruction": "Resuma este texto"
}
```

**Exemplos de instruções:**
- "Resuma este texto"
- "Corrija a gramática"
- "Traduza para inglês"
- "Reescreva de forma mais clara"

**Resposta:**
```json
{
  "id": 1,
  "title": "Java Best Practices",
  "content": "Texto modificado pela IA...",
  "tags": ["java", "best-practices"],
  "documents": []
}
```

---

### POST `/api/ai/transcribe/document/{documentId}/to-item/{itemId}`
Transcreve texto de uma imagem (OCR) e anexa a um item de conhecimento.

**Resposta:**
```json
{
  "id": 1,
  "title": "Java Best Practices",
  "content": "Texto extraído da imagem...",
  "tags": ["java", "best-practices"],
  "documents": []
}
```

---

### POST `/api/ai/review/portfolio`
Revisa um portfólio do GitHub (atua como Tech Recruiter).

**Entrada:**
```json
{
  "githubRepoUrl": "https://github.com/usuario/repositorio"
}
```

**Resposta:**
```json
{
  "id": 1,
  "role": "assistant",
  "content": "Análise do portfólio como Tech Recruiter...",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### POST `/api/ai/think/product`
Pensa como um Gerente de Produto (análise de funcionalidade).

**Entrada:**
```json
{
  "featureDescription": "Adicionar sistema de notificações em tempo real"
}
```

**Resposta:**
```json
{
  "id": 1,
  "role": "assistant",
  "content": "Análise de produto (User Story, UX, Trade-offs)...",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### POST `/v1/ai/document/analyze`
Analisa um documento (PDF, DOCX, TXT, imagens) com um prompt de texto usando IA.

**Formato:** `multipart/form-data`

**Parâmetros:**
- `file` (obrigatório): Arquivo do documento a ser analisado
- `prompt` (obrigatório): Prompt de texto para guiar a análise da IA
- `provider` (opcional): Provedor de IA a ser usado. Opções: `ollamaProvider` (padrão), `groqProvider`

**Comportamento:**
- **Documentos de texto**: Aplica padrão Map-Reduce para processar documentos grandes
  - Divide o documento em chunks menores
  - Processa cada chunk em paralelo (Map)
  - Consolida as respostas em um relatório final (Reduce)
- **Imagens**: Processa diretamente com modelo multimodal
- **Salvamento automático**: Todas as interações são salvas no banco de dados para RAG
  - Cria uma sessão de chat
  - Salva o prompt do usuário
  - Salva a resposta do assistente
  - Atualiza o perfil do usuário

**Exemplo de requisição (cURL):**
```bash
curl -X POST "http://localhost:8080/v1/ai/document/analyze" \
  -F "file=@documento.pdf" \
  -F "prompt=Faça uma auditoria técnica deste documento" \
  -F "provider=ollamaProvider"
```

**Resposta:**
```json
{
  "content": "Análise completa do documento...",
  "error": null
}
```

**Notas:**
- O processamento pode levar vários segundos (especialmente na primeira chamada)
- Timeout configurado para 180-200 segundos para dar tempo ao Ollama processar
- Se o Ollama falhar, o sistema faz fallback automático para Groq
- Todas as mensagens são salvas em `chat_session` e `chat_message` para RAG

---

## 🔗 Integrations

### GET `/api/integrations/github/connect`
Inicia o processo de conexão com o GitHub (OAuth).

**Resposta:** Redireciona para a página de autorização do GitHub.

---

### GET `/api/integrations/github/callback`
Callback do OAuth do GitHub.

**Parâmetros de Query:**
- `code`: Código de autorização (quando sucesso)
- `error`: Mensagem de erro (quando falha)

**Resposta (sucesso):**
```
Conta do GitHub conectada e token salvo com sucesso!
```

---

## 📌 Notas Importantes

### Autenticação
Atualmente não há sistema de autenticação implementado. O `userId` está hardcoded como `1L` na integração do GitHub.

### Formato de Data
- **Datas simples:** `"2024-12-31"` (formato ISO: YYYY-MM-DD)
- **Datas com hora:** `"2024-01-15T10:00:00"` (formato ISO: YYYY-MM-DDTHH:mm:ss)
- **Timezone:** As datas são armazenadas em LocalDateTime (sem timezone)

### Upload de Arquivos
Use `multipart/form-data` para uploads. O campo `file` é obrigatório.

### Códigos de Status HTTP
- `200 OK`: Requisição bem-sucedida
- `201 Created`: Recurso criado com sucesso (não usado atualmente, todos retornam 200)
- `204 No Content`: Sucesso sem conteúdo (usado em DELETE)
- `400 Bad Request`: Dados inválidos ou faltando campos obrigatórios
- `404 Not Found`: Recurso não encontrado
- `500 Internal Server Error`: Erro interno do servidor

### Validação
- Campos marcados como obrigatórios retornam erro `400 Bad Request` se não forem fornecidos
- Validações são feitas usando Bean Validation (Jakarta Validation)
- Mensagens de erro são retornadas no formato padrão do Spring Boot

### Enums
**ProficiencyLevel:**
- `BEGINNER`: Iniciante
- `INTERMEDIATE`: Intermediário
- `ADVANCED`: Avançado

**ProfessionalLevel:**
- `JUNIOR`: Júnior
- `PLENO`: Pleno
- `SENIOR`: Sênior

**WorkspaceType:**
- `PROJECT`: Workspace focado em projetos
- `STUDY`: Workspace focado em estudos
- `GENERIC`: Workspace genérico/híbrido

**AnalysisMode (IA):**
- `MENTOR`: Modo didático e guiado (padrão)
- `ANALYST`: Modo direto e sincero
- `DEBUG_ASSISTANT`: Focado em encontrar e corrigir bugs
- `SOCRATIC_TUTOR`: Focado em fazer perguntas para guiar o aprendizado

