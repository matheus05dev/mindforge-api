#!/bin/bash

echo "🤖 MindForge - Setting up Ollama models..."
echo ""

# Espera Ollama estar pronto
echo "⏳ Waiting for Ollama to be ready..."
until curl -s http://localhost:11434/api/tags > /dev/null 2>&1; do
  echo "   Ollama not ready yet, waiting 2s..."
  sleep 2
done

echo "✅ Ollama is ready!"
echo ""

# Modelo de embeddings (OBRIGATÓRIO pro RAG)
echo "📥 Pulling embedding model: nomic-embed-text (274MB)..."
docker exec mindforge-ollama ollama pull nomic-embed-text
echo "✅ Embedding model ready!"
echo ""

# Modelo de chat (ESCOLHA UM)
echo "📥 Which chat model do you want?"
echo "1) llama3.1:8b (4.7GB) - Recommended (best quality)"
echo "2) phi3:mini (2.3GB) - Fast (good for low-end PCs)"
echo "3) mistral:7b (4.1GB) - Alternative (good quality)"
echo "4) Skip chat model (only embeddings)"
echo ""
read -p "Choose (1-4): " choice

case $choice in
  1)
    echo "📥 Pulling llama3.1:8b (this may take 5-10 minutes)..."
    docker exec mindforge-ollama ollama pull llama3.1:8b
    echo "✅ llama3.1:8b ready!"
    ;;
  2)
    echo "📥 Pulling phi3:mini..."
    docker exec mindforge-ollama ollama pull phi3:mini
    echo "✅ phi3:mini ready!"
    ;;
  3)
    echo "📥 Pulling mistral:7b..."
    docker exec mindforge-ollama ollama pull mistral:7b
    echo "✅ mistral:7b ready!"
    ;;
  4)
    echo "⏭️  Skipping chat model"
    ;;
  *)
    echo "❌ Invalid choice. Run script again."
    exit 1
    ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Ollama setup complete!"
echo ""
echo "📋 Installed models:"
docker exec mindforge-ollama ollama list
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
