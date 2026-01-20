-- Seeding Default Workspaces
-- Using ON CONFLICT to avoid errors on restart if data persists
INSERT INTO workspace (id, name, description, type) VALUES (1, 'Geral', 'Visão geral de tudo', 'GENERIC') ON CONFLICT (id) DO NOTHING;
INSERT INTO workspace (id, name, description, type) VALUES (2, 'Estudos', 'Gestão de estudos e aprendizado', 'STUDY') ON CONFLICT (id) DO NOTHING;
INSERT INTO workspace (id, name, description, type) VALUES (3, 'Projetos', 'Gestão de projetos e tarefas', 'PROJECT') ON CONFLICT (id) DO NOTHING;

-- Reset sequence to avoid collisions with future inserts
-- Assuming sequence name follows standard 'workspace_id_seq'
SELECT setval('workspace_id_seq', (SELECT MAX(id) FROM workspace));
