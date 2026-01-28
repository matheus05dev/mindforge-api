-- Cria extensão pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Verifica se criou
SELECT * FROM pg_extension WHERE extname = 'vector';

-- Opcional: cria schema separado
-- CREATE SCHEMA IF NOT EXISTS mindforge;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE mindforge TO mindforge_user;

-- Mensagem de sucesso
DO $$
BEGIN
    RAISE NOTICE '✅ pgvector extension installed successfully!';
    RAISE NOTICE '✅ Database mindforge ready for MindForge API';
END $$;
