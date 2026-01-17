# Configuração do Ollama para Embeddings Gratuitos

## O que é Ollama?

Ollama é uma ferramenta **gratuita e open source** que permite rodar modelos de IA localmente no seu computador. Não precisa de API keys ou serviços pagos.

## Instalação

### Windows
1. Baixe o instalador em: https://ollama.ai/download
2. Execute o instalador
3. Ollama será iniciado automaticamente

### Mac
```bash
brew install ollama
```

### Linux
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

## Modelos de Embeddings Recomendados

### ⭐⭐ Recomendação Premium: `nomic-embed-text-v2-moe` (Versão Mais Recente)

```bash
ollama pull nomic-embed-text-v2-moe
```

**Características:**
- ✅ **Dimensão**: 768 (padrão), suporta truncamento até 256
- ✅ **Arquitetura**: MoE (Mixture of Experts) - mais eficiente
- ✅ **Parâmetros**: ~475M total, ~305M ativos (economiza recursos)
- ✅ **Idiomas**: ~100 línguas, incluindo português
- ✅ **Performance**: Melhor qualidade que v1, mantém boa velocidade
- ✅ **Treinamento**: Centenas de milhões de pares de texto
- ✅ **Uso**: Ideal para RAG, documentos técnicos, busca semântica multilíngue
- ✅ **Requisitos**: Funciona bem em CPU, ~4-6GB RAM

**Quando usar**: Versão mais recente e eficiente. ⭐⭐⭐⭐⭐ **MELHOR ESCOLHA**

---

### ⭐ Recomendação Clássica: `nomic-embed-text` (v1)

```bash
ollama pull nomic-embed-text
```

**Características:**
- ✅ **Dimensão**: 768
- ✅ **Tamanho**: ~274 MB (mais leve que v2-moe)
- ✅ **Performance**: Excelente equilíbrio entre qualidade e velocidade
- ✅ **Idiomas**: Multilíngue (inclui português)
- ✅ **Uso**: Ideal para RAG, documentos técnicos, busca semântica
- ✅ **Requisitos**: Funciona bem em CPU, 4GB RAM

**Quando usar**: Se preferir uma versão mais leve ou se v2-moe for pesado demais para seu hardware

---

### 🎯 Alternativa: `mxbai-embed-large` (Para máxima precisão)

```bash
ollama pull mxbai-embed-large
```

**Características:**
- ✅ **Dimensão**: 1024
- ✅ **Tamanho**: ~700 MB (maior)
- ✅ **Performance**: Melhor precisão, especialmente em textos técnicos complexos
- ✅ **Uso**: Quando precisa da máxima qualidade de embeddings
- ⚠️ **Requisitos**: Mais RAM, pode ser mais lento

**Quando usar**: Quando qualidade > velocidade, documentos muito complexos, produção crítica

---

### ⚡ Alternativa: `all-minilm` (Para máxima velocidade)

```bash
ollama pull all-minilm
```

**Características:**
- ✅ **Dimensão**: 384
- ✅ **Tamanho**: ~90 MB (muito leve)
- ✅ **Performance**: Muito rápido, mas um pouco menos preciso
- ✅ **Uso**: Protótipos, desenvolvimento, quando velocidade é crucial
- ✅ **Requisitos**: Funciona até em máquinas modestas

**Quando usar**: Desenvolvimento, protótipos rápidos, hardware limitado

## Verificar se está funcionando

Teste se o Ollama está rodando:

```bash
ollama list
```

Você deve ver `nomic-embed-text` na lista.

## Teste de Embeddings

Teste se os embeddings estão funcionando:

```bash
ollama run nomic-embed-text "Hello world"
```

## Configuração da Aplicação

A aplicação já está configurada para usar Ollama. As configurações estão em `application.properties`:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.options.model=nomic-embed-text
```

## Iniciar Ollama

O Ollama deve estar rodando antes de iniciar a aplicação. Ele inicia automaticamente quando você instala, mas se precisar iniciar manualmente:

**Windows/Mac**: Já inicia automaticamente

**Linux**:
```bash
ollama serve
```

## Comparação de Modelos

| Modelo | Dimensão | Tamanho | Velocidade | Precisão | Uso Recomendado |
|--------|----------|---------|------------|----------|-----------------|
| **nomic-embed-text-v2-moe** ⭐⭐ | 768 | ~500 MB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **Versão mais recente, melhor qualidade** |
| **nomic-embed-text** ⭐ | 768 | 274 MB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | RAG geral, mais leve |
| **mxbai-embed-large** | 1024 | 700 MB | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Produção, máxima precisão |
| **all-minilm** | 384 | 90 MB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Protótipos, desenvolvimento |

## Minha Recomendação para Seu Projeto

Para seu caso (RAG com documentos técnicos como System Design):

**Use `nomic-embed-text-v2-moe`** ⭐⭐
- Versão mais recente e avançada
- Arquitetura MoE = mais eficiente com menos recursos
- Melhor qualidade que v1, mantém boa velocidade
- Multilíngue (~100 idiomas)
- Suporta truncamento até 256 dims se precisar economizar espaço

**Alternativa:** Se preferir algo mais leve, use `nomic-embed-text` (v1).

## Troubleshooting

### Ollama não está rodando
- Verifique se o serviço está ativo: `ollama list`
- Tente iniciar manualmente: `ollama serve`

### Erro de conexão
- Verifique se o Ollama está na porta 11434
- Verifique o firewall

### Modelo não encontrado
- Certifique-se de ter baixado o modelo: `ollama pull nomic-embed-text`

## Vantagens do Ollama

1. ✅ **100% Gratuito** - Sem limites ou custos
2. ✅ **100% Privado** - Dados não saem do seu computador
3. ✅ **Open Source** - Código aberto e auditable
4. ✅ **Sem Internet** - Funciona offline (após baixar o modelo)
5. ✅ **Rápido** - Sem latência de rede

