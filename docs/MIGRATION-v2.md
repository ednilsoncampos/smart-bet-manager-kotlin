# Migration Guide - v2.0

## Breaking Changes

### ❌ Endpoint Removido: `/api/users/profile`

**Motivo:** Endpoint nunca foi implementado. Requisições eram tratadas como recursos estáticos.

**Impacto:** Aplicações que tentam acessar este endpoint receberão `404 Not Found`.

---

## ✅ Como Migrar

### Antes (❌ Não funciona)
```http
GET /api/users/profile
Authorization: Bearer {token}
```

### Depois (✅ Correto)
```http
GET /api/auth/me
Authorization: Bearer {token}
```

---

## 📋 Response Format

### Endpoint Correto: `GET /api/auth/me`

**Request:**
```bash
curl -X GET https://smartbet.api.br/api/auth/me \
  -H "Authorization: Bearer eyJhbGc..."
```

**Response (200 OK):**
```json
{
  "id": 123,
  "email": "user@example.com",
  "name": "John Doe",
  "role": "USER",
  "createdAt": 1706400000000
}
```

---

## 🔍 Novo Tratamento de Erros

Quando um endpoint não existe, a API agora retorna uma resposta JSON estruturada com sugestão:

### Erro ao acessar `/api/users/profile`:

**Response (404 Not Found):**
```json
{
  "status": 404,
  "error": "ENDPOINT_NOT_FOUND",
  "message": "O endpoint '/api/users/profile' não existe",
  "suggestion": "Use GET /api/auth/me para obter o perfil do usuário autenticado",
  "timestamp": "2026-01-29T12:34:56.789Z"
}
```

---

## 📝 Checklist de Migração Frontend

- [ ] Substituir todas as chamadas de `/api/users/profile` por `/api/auth/me`
- [ ] Verificar se há referências em:
  - [ ] Código JavaScript/TypeScript
  - [ ] Arquivos de configuração
  - [ ] Variáveis de ambiente
  - [ ] Documentação
  - [ ] Testes E2E
- [ ] Testar fluxo completo de autenticação
- [ ] Atualizar README/docs do frontend
- [ ] Deploy e validação

---

## 🛠️ Para Desenvolvedores

### Endpoints de Autenticação Disponíveis

| Método | Endpoint | Descrição | Auth Required |
|--------|----------|-----------|---------------|
| POST | `/api/auth/register` | Criar nova conta | ❌ |
| POST | `/api/auth/login` | Login | ❌ |
| POST | `/api/auth/refresh` | Renovar token | ❌ |
| GET | `/api/auth/me` | Perfil do usuário | ✅ |
| POST | `/api/auth/change-password` | Alterar senha | ✅ |

### Rate Limiting

Os endpoints públicos têm rate limiting por IP:
- `/api/auth/login`: 5 requisições/minuto
- `/api/auth/register`: 3 requisições/hora
- `/api/auth/refresh`: 10 requisições/minuto

---

## 📞 Suporte

Em caso de dúvidas ou problemas na migração:
1. Verifique a documentação completa em `/swagger-ui.html`
2. Consulte o time de backend
3. Abra uma issue no repositório

---

## 🔄 Changelog

### v2.0.0 - 2026-01-29

#### Breaking Changes
- ❌ Removido endpoint inexistente `/api/users/profile`

#### Improvements
- ✅ Melhorado tratamento de erro para endpoints não encontrados
- ✅ Adicionado campo `suggestion` em respostas de erro
- ✅ Implementado rate limiting em endpoints de autenticação
- ✅ Adicionadas validações em DTOs de tickets
- ✅ Corrigida vulnerabilidade CORS
- ✅ Substituído force unwrap (`!!`) por validações explícitas
- ✅ Adicionados índices de performance no banco de dados

#### Fixed
- 🐛 Corrigido CORS inseguro (CVE potencial)
- 🐛 Removido risco de NullPointerException em múltiplos pontos
