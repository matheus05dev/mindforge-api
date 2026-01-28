# 🐳 Setup & Infraestrutura

Esta pasta contém todos os arquivos e scripts necessários para rodar a infraestrutura do **MindForge** (Banco de Dados, IA Local e API).

## 📂 Conteúdo

| Arquivo | Função |
|---------|--------|
| `Makefile` | Atalhos simplificados para todos os comandos (dev, up, test, logs) |
| `docker-compose.yml` | Orquestração dos containers (Postgres, Ollama, API) |
| `setup-ollama.sh` | Script para baixar e configurar modelos de IA automaticamente |
| `init.sql` | Inicialização do Banco de Dados (Extension PGVector) |
| `Dockerfile` | Definição da imagem Docker da aplicação Java |
| `application.properties.example` | Template completo de configuração (Banco, IA, Resiliência, OAuth) |

## 🚀 Como Usar

A partir **desta pasta**, execute:

```bash
# Iniciar ambiente de desenvolvimento (BD + Ollama)
make dev

# Baixar modelos de IA (necessário na 1ª vez)
make setup-ai

# Ver logs
make logs

# Parar tudo
make down
```

Para mais detalhes, consulte o [README principal](../README.md).
