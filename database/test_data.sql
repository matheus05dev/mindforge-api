-- testes do database

-- Inserções para a tabela 'projects'
INSERT INTO projects (name, description) VALUES
                                             ('MindForge API', 'API para o projeto MindForge'),
                                             ('Outro Projeto', 'Descrição de outro projeto');

-- Inserções para a tabela 'milestones'
INSERT INTO milestones (project_id, title, description, due_date, completed) VALUES
                                                                                 (1, 'Entrega 1', 'Primeira entrega da API', '2024-12-31', false),
                                                                                 (1, 'Entrega 2', 'Segunda entrega da API', '2025-01-31', false),
                                                                                 (2, 'Fase 1', 'Primeira fase do outro projeto', '2024-11-30', true);

-- Inserções para a tabela 'subjects'
INSERT INTO subjects (name, description) VALUES
                                             ('Java', 'Estudo da linguagem Java'),
                                             ('Spring Boot', 'Estudo do framework Spring Boot');

-- Inserções para a tabela 'study_sessions'
INSERT INTO study_sessions (subject_id, start_time, duration_minutes, notes) VALUES
                                                                                 (1, '2024-07-22 14:00:00', 60, 'Revisão de Generics'),
                                                                                 (2, '2024-07-22 15:00:00', 90, 'Estudo de injeção de dependências');

-- Inserções para a tabela 'kanban_columns'
INSERT INTO kanban_columns (name, position) VALUES
                                                ('A Fazer', 1),
                                                ('Em Andamento', 2),
                                                ('Feito', 3);

-- Inserções para a tabela 'kanban_tasks'
INSERT INTO kanban_tasks (title, description, column_id, project_id, subject_id, position) VALUES
                                                                                               ('Implementar autenticação', 'Usar Spring Security', 1, 1, 2, 1),
                                                                                               ('Estudar JPA', 'Focar em relacionamentos', 2, null, 1, 1),
                                                                                               ('Corrigir bug X', 'Bug na listagem de projetos', 1, 1, null, 2);

-- Inserções para a tabela 'knowledge_items'
INSERT INTO knowledge_items (title, content, type, file_path) VALUES
                                                                  ('Anotação sobre REST', 'REST é um estilo arquitetural...', 'NOTE', null),
                                                                  ('Diagrama da API', null, 'IMAGE', '/path/to/diagram.png');

-- QUERIES DE BUSCA --

-- 1. Listar todas as tarefas do Kanban com o nome da coluna correspondente
SELECT
    t.title AS "Tarefa",
    t.description AS "Descrição",
    c.name AS "Coluna"
FROM
    kanban_tasks t
        JOIN
    kanban_columns c ON t.column_id = c.id;

-- 2. Encontrar todos os marcos (milestones) que ainda não foram concluídos
SELECT
    p.name AS "Projeto",
    m.title AS "Marco",
    m.due_date AS "Prazo"
FROM
    milestones m
        JOIN
    projects p ON m.project_id = p.id
WHERE
    m.completed = false;

-- 3. Listar todas as sessões de estudo sobre "Java"
SELECT
    ss.start_time AS "Início",
    ss.duration_minutes AS "Duração (min)",
    ss.notes AS "Anotações"
FROM
    study_sessions ss
        JOIN
    subjects s ON ss.subject_id = s.id
WHERE
    s.name = 'Java';

-- 4. Contar quantas tarefas existem em cada coluna do Kanban
SELECT
    c.name AS "Coluna",
    COUNT(t.id) AS "Total de Tarefas"
FROM
    kanban_columns c
        LEFT JOIN
    kanban_tasks t ON c.id = t.column_id
GROUP BY
    c.name, c.position
ORDER BY
    c.position;
