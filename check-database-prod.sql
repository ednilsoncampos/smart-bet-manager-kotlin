-- ============================================
-- Script para verificar estado do banco de dados em produção
-- ============================================

-- 1. Verificar se a tabela do Flyway existe
SELECT EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_name = 'flyway_schema_history'
) AS flyway_table_exists;

-- 2. Se existir, mostrar histórico de migrations
SELECT version, description, type, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- 3. Verificar schemas existentes
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name IN ('core', 'betting', 'log', 'public')
ORDER BY schema_name;

-- 4. Verificar tabelas e seus schemas
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('core', 'betting', 'log', 'public')
AND table_type = 'BASE TABLE'
ORDER BY table_schema, table_name;

-- 5. Verificar permissões do usuário doadmin
SELECT grantee, privilege_type
FROM information_schema.role_table_grants
WHERE grantee = 'doadmin'
LIMIT 20;

-- 6. Ver distribuição de status dos bilhetes
SELECT ticket_status, COUNT(*) as total
FROM betting.bet_tickets
GROUP BY ticket_status
ORDER BY ticket_status;
