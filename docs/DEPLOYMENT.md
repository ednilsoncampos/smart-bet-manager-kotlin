# Guia de Deploy - Smart Bet Manager

Este documento descreve o processo de deploy do Smart Bet Manager na DigitalOcean.

## Pré-requisitos

Antes de iniciar o deploy, você precisará de uma conta DigitalOcean com os seguintes recursos provisionados:

| Recurso | Descrição |
|---------|-----------|
| DOKS Cluster | Kubernetes gerenciado (mínimo 2 nodes) |
| Managed Database | PostgreSQL 16 |
| Container Registry | Para armazenar imagens Docker |
| Domain | Domínio configurado no DigitalOcean DNS |

## Opção 1: DigitalOcean Kubernetes (DOKS)

### 1. Configurar kubectl

```bash
# Instalar doctl
brew install doctl  # macOS
# ou
snap install doctl  # Linux

# Autenticar
doctl auth init

# Configurar kubeconfig
doctl kubernetes cluster kubeconfig save <cluster-name>
```

### 2. Criar namespace

```bash
kubectl create namespace smartbet
```

### 3. Configurar secrets

Edite o arquivo `k8s/secret.yaml` com os valores reais:

```yaml
stringData:
  db-user: "seu_usuario"
  db-password: "sua_senha_segura"
  jwt-secret: "chave_jwt_256_bits_minimo"
```

Aplique os secrets:

```bash
kubectl apply -f k8s/secret.yaml -n smartbet
```

### 4. Configurar ConfigMap

Edite o arquivo `k8s/configmap.yaml` com os valores do seu ambiente:

```yaml
data:
  db-host: "seu-db.db.ondigitalocean.com"
  db-port: "25060"
  db-name: "smartbet"
```

Aplique o ConfigMap:

```bash
kubectl apply -f k8s/configmap.yaml -n smartbet
```

### 5. Build e push da imagem

```bash
# Login no registry
doctl registry login

# Build da imagem
docker build -t registry.digitalocean.com/<seu-registry>/smart-bet-backend:latest .

# Push
docker push registry.digitalocean.com/<seu-registry>/smart-bet-backend:latest
```

### 6. Deploy da aplicação

Atualize a imagem no `k8s/deployment.yaml`:

```yaml
image: registry.digitalocean.com/<seu-registry>/smart-bet-backend:latest
```

Aplique os manifests:

```bash
kubectl apply -f k8s/deployment.yaml -n smartbet
kubectl apply -f k8s/service.yaml -n smartbet
kubectl apply -f k8s/hpa.yaml -n smartbet
```

### 7. Configurar Ingress

Instale o NGINX Ingress Controller:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/do/deploy.yaml
```

Instale o cert-manager para TLS:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
```

Crie o ClusterIssuer para Let's Encrypt:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: seu-email@exemplo.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

Aplique o Ingress:

```bash
kubectl apply -f k8s/ingress.yaml -n smartbet
```

### 8. Verificar deploy

```bash
# Status dos pods
kubectl get pods -n smartbet

# Logs
kubectl logs -f deployment/smart-bet-backend -n smartbet

# Health check
curl https://api.seudominio.com/actuator/health
```

## Opção 2: DigitalOcean App Platform

Para deploys mais simples, você pode usar o App Platform:

### 1. Criar App

1. Acesse o painel DigitalOcean
2. Vá em "Apps" → "Create App"
3. Conecte seu repositório GitHub
4. Selecione o branch `main`

### 2. Configurar Build

O App Platform detectará automaticamente o Dockerfile. Configure:

| Configuração | Valor |
|--------------|-------|
| Build Command | (automático via Dockerfile) |
| Run Command | (automático via Dockerfile) |
| HTTP Port | 8080 |

### 3. Configurar Variáveis de Ambiente

Adicione as variáveis:

| Variável | Valor |
|----------|-------|
| `DB_HOST` | Host do Managed Database |
| `DB_PORT` | 25060 |
| `DB_NAME` | smartbet |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha (marcar como secret) |
| `JWT_SECRET` | Chave JWT (marcar como secret) |
| `SPRING_PROFILES_ACTIVE` | docker |

### 4. Configurar Database

1. Vá em "Components" → "Add Component" → "Database"
2. Selecione seu Managed PostgreSQL
3. O App Platform configurará automaticamente a conexão

### 5. Deploy

Clique em "Deploy" e aguarde. O App Platform irá:
1. Clonar o repositório
2. Build da imagem Docker
3. Deploy da aplicação
4. Configurar SSL automático

## Variáveis de Ambiente

| Variável | Descrição | Obrigatório |
|----------|-----------|-------------|
| `DB_HOST` | Host do PostgreSQL | Sim |
| `DB_PORT` | Porta do PostgreSQL | Sim |
| `DB_NAME` | Nome do banco | Sim |
| `DB_USER` | Usuário do banco | Sim |
| `DB_PASSWORD` | Senha do banco | Sim |
| `JWT_SECRET` | Chave para assinar tokens | Sim |
| `SPRING_PROFILES_ACTIVE` | Profile ativo (docker/prod) | Sim |

## Health Checks

A aplicação expõe os seguintes endpoints de saúde:

| Endpoint | Descrição |
|----------|-----------|
| `/actuator/health` | Status geral |
| `/actuator/health/liveness` | Probe de liveness |
| `/actuator/health/readiness` | Probe de readiness |
| `/actuator/prometheus` | Métricas Prometheus |

## Troubleshooting

### Pod não inicia

```bash
# Ver eventos
kubectl describe pod <pod-name> -n smartbet

# Ver logs
kubectl logs <pod-name> -n smartbet
```

### Conexão com banco falha

1. Verifique se o IP do cluster está na whitelist do Managed Database
2. Confirme que SSL está habilitado (`sslmode=require`)
3. Teste conexão manualmente:

```bash
kubectl run pg-test --rm -it --image=postgres:16 -- psql "postgresql://user:pass@host:port/db?sslmode=require"
```

### Certificado SSL não emitido

```bash
# Ver status do certificado
kubectl describe certificate smartbet-tls -n smartbet

# Ver logs do cert-manager
kubectl logs -n cert-manager deployment/cert-manager
```
