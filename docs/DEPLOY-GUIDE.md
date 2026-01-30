# Deploy Guide - Smart Bet Manager

## 📋 Pré-requisitos

- [ ] Cluster Kubernetes configurado (DigitalOcean)
- [ ] `doctl` instalado e configurado
- [ ] Secrets configurados no GitHub
- [ ] Domínios apontados para o Ingress

---

## 🔐 GitHub Secrets Necessários

Configure em **Settings → Secrets and variables → Actions**:

### Secrets Obrigatórios

| Secret | Ambiente | Descrição | Exemplo |
|--------|----------|-----------|---------|
| `DIGITALOCEAN_ACCESS_TOKEN` | Todos | Token de acesso DigitalOcean | `dop_v1_xxx...` |
| `CLUSTER_NAME` | Todos | Nome do cluster K8s | `smartbet-cluster` |
| `DB_USER_PROD` | Production | Usuário do banco (prod) | `smartbet_prod` |
| `DB_PASSWORD_PROD` | Production | Senha do banco (prod) | `senha-segura-xyz` |
| `JWT_SECRET_PROD` | Production | Chave JWT (prod) | `min-256-bits-long...` |
| `DB_USER_STAGING` | Staging | Usuário do banco (staging) | `smartbet_staging` |
| `DB_PASSWORD_STAGING` | Staging | Senha do banco (staging) | `senha-segura-abc` |
| `JWT_SECRET_STAGING` | Staging | Chave JWT (staging) | `min-256-bits-long...` |

**⚠️ IMPORTANTE:**
- JWT secrets devem ter **pelo menos 256 bits** (32+ caracteres)
- Use senhas fortes para banco de dados
- **NUNCA commite secrets no código**

---

## 🗺️ ConfigMaps (Variáveis Não-Sensíveis)

ConfigMaps já estão versionados em `k8s/overlays/{env}/configmap.yaml`:

### Development (`k8s/overlays/dev/configmap.yaml`)
```yaml
cors-allowed-origins: "http://localhost:3000,http://localhost:4200,http://localhost:8081"
db-host: "smartbet-postgres-dev-do-user-32074130-0.f.db.ondigitalocean.com"
spring-profile: "dev"
```

### Staging (`k8s/overlays/staging/configmap.yaml`)
```yaml
cors-allowed-origins: "https://staging.smartbet.api.br,https://app-staging.smartbet.api.br"
db-host: "smartbet-postgres-staging-do-user-xxx.db.ondigitalocean.com"
spring-profile: "staging"
```

### Production (`k8s/overlays/prod/configmap.yaml`)
```yaml
cors-allowed-origins: "https://app.smartbet.api.br,https://smartbet.api.br"
db-host: "private-smartbet-postgres-dev-do-user-32074130-0.f.db.ondigitalocean.com"
spring-profile: "prod"
```

**✏️ Ação necessária:** Atualizar `db-host` e `cors-allowed-origins` com valores reais.

---

## 🚀 Fluxo de Deploy Automático

### Branches e Ambientes

| Branch | Ambiente | Deploy Automático | Image Tag |
|--------|----------|-------------------|-----------|
| `development` | DEV | ❌ Só build | `dev-{sha}` |
| `staging` | STAGING | ✅ Automático | `staging-{sha}` |
| `main` | PROD | ✅ Automático | `{sha}` |
| Tag `v*` | PROD | ✅ Automático | `{sha}` |

### Processo Automático

1. **Push para branch** → GitHub Actions inicia
2. **Build da imagem Docker** → Publicada no GHCR
3. **Deploy no Kubernetes** (staging/prod)
4. **Flyway executa migrations** automaticamente no startup
5. **Health checks** validam aplicação
6. **Rollout** completa quando pod está ready

---

## 🗄️ Migrations de Banco de Dados

### ✅ Executadas AUTOMATICAMENTE

**Quando:** No startup da aplicação (antes de aceitar tráfego)

**Configuração:**
```yaml
# application.yml
flyway:
  enabled: true
  baseline-on-migrate: true
  locations: classpath:db/migration

# deployment.yaml
startupProbe:
  failureThreshold: 60  # Permite até 300s para migrations
```

**Arquivos de migration:**
- `V1__initial_schema.sql`
- `V2__add_bankroll.sql`
- ...
- `V8__add_performance_indexes.sql` ← **Nova migration**

**Validação:**
```bash
# Ver logs de migration no pod
kubectl logs -n smartbet-prod -l app=smart-bet-backend | grep -i flyway

# Exemplo de saída esperada:
# INFO o.f.c.i.s.JdbcTableSchemaHistory : Creating Schema History table
# INFO o.f.core.internal.command.DbMigrate : Migrating schema to version 8
# INFO o.f.core.internal.command.DbMigrate : Successfully applied 1 migration
```

---

## 📝 Checklist de Deploy

### Primeira vez (Setup inicial)

- [ ] **1. Configurar Secrets no GitHub**
  - [ ] `DIGITALOCEAN_ACCESS_TOKEN`
  - [ ] `CLUSTER_NAME`
  - [ ] `DB_USER_PROD`, `DB_PASSWORD_PROD`, `JWT_SECRET_PROD`
  - [ ] `DB_USER_STAGING`, `DB_PASSWORD_STAGING`, `JWT_SECRET_STAGING`

- [ ] **2. Atualizar ConfigMaps**
  - [ ] Editar `k8s/overlays/prod/configmap.yaml`
    - [ ] `db-host` com host real do banco
    - [ ] `cors-allowed-origins` com domínios reais do frontend
  - [ ] Editar `k8s/overlays/staging/configmap.yaml`
    - [ ] Mesmos campos

- [ ] **3. Validar Kustomize**
  ```bash
  # Validar configuração de prod
  kustomize build k8s/overlays/prod

  # Validar configuração de staging
  kustomize build k8s/overlays/staging
  ```

- [ ] **4. Commit e Push**
  ```bash
  git add k8s/
  git commit -m "chore: configure k8s for prod deployment"
  git push origin staging  # Ou main para prod
  ```

- [ ] **5. Monitorar Deploy**
  - [ ] Ir para **Actions** no GitHub
  - [ ] Acompanhar workflow "Deploy to DigitalOcean"
  - [ ] Verificar logs do job "Deploy"

### Deploys subsequentes

- [ ] **1. Fazer merge para branch desejada**
  ```bash
  git checkout staging
  git merge development
  git push origin staging
  ```

- [ ] **2. Acompanhar GitHub Actions**
  - Deploy inicia automaticamente

- [ ] **3. Validar aplicação**
  ```bash
  # Get pods
  kubectl get pods -n smartbet-staging

  # Ver logs
  kubectl logs -n smartbet-staging -l app=smart-bet-backend --tail=100

  # Testar endpoint
  curl https://api-staging.smartbet.api.br/api/health
  ```

---

## 🔍 Troubleshooting

### Pod não inicia (CrashLoopBackOff)

```bash
# Ver logs do pod
kubectl logs -n smartbet-prod -l app=smart-bet-backend

# Descrever pod para ver eventos
kubectl describe pod -n smartbet-prod -l app=smart-bet-backend

# Causas comuns:
# - Secret não configurado → Ver "Error creating bean"
# - Migration falhou → Ver logs Flyway
# - Banco indisponível → Ver "Connection refused"
```

### Migration falha

```bash
# Ver tabela de histórico do Flyway
kubectl exec -n smartbet-prod deployment/smart-bet-backend -- \
  psql $DATABASE_URL -c "SELECT * FROM flyway_schema_history;"

# Fazer rollback manual SE NECESSÁRIO (CUIDADO!)
# Flyway não faz rollback automático - você precisa criar uma nova migration
```

### CORS ainda bloqueando

```bash
# Verificar ConfigMap aplicado
kubectl get configmap smart-bet-config -n smartbet-prod -o yaml

# Se não atualizou, aplicar manualmente
kubectl apply -k k8s/overlays/prod

# Reiniciar pods para pegar nova config
kubectl rollout restart deployment/smart-bet-backend -n smartbet-prod
```

---

## 🎯 Comandos Úteis

```bash
# Ver status do deployment
kubectl get deployment -n smartbet-prod

# Ver pods
kubectl get pods -n smartbet-prod

# Ver logs em tempo real
kubectl logs -f -n smartbet-prod -l app=smart-bet-backend

# Executar comando no pod
kubectl exec -it -n smartbet-prod deployment/smart-bet-backend -- bash

# Ver eventos do namespace
kubectl get events -n smartbet-prod --sort-by='.lastTimestamp'

# Deletar pod (K8s cria novo automaticamente)
kubectl delete pod -n smartbet-prod -l app=smart-bet-backend

# Fazer rollback para versão anterior
kubectl rollout undo deployment/smart-bet-backend -n smartbet-prod

# Ver histórico de deploys
kubectl rollout history deployment/smart-bet-backend -n smartbet-prod
```

---

## 📊 Monitoramento

### Health Checks

| Endpoint | Descrição |
|----------|-----------|
| `/actuator/health` | Health geral |
| `/actuator/health/readiness` | Pronto para tráfego |
| `/actuator/health/liveness` | Aplicação viva |
| `/actuator/prometheus` | Métricas Prometheus |

### Acessar Swagger (apenas dev/staging)

```
https://api-dev.smartbet.api.br/swagger-ui.html
https://api-staging.smartbet.api.br/swagger-ui.html
```

**⚠️ Swagger desabilitado em produção** por segurança.

---

## 🆘 Contatos

- **DevOps:** [seu-email]
- **Backend:** [time-backend]
- **Documentação:** `/docs` no repositório
