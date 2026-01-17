# Kubernetes Deployment - Smart Bet Manager

Este diretório contém as configurações Kubernetes usando **Kustomize** para deploy em diferentes ambientes.

## Estrutura

```
k8s/
├── base/                    # Recursos compartilhados
│   ├── deployment.yaml      # Deployment base
│   ├── service.yaml         # Service ClusterIP
│   ├── ingress.yaml         # Ingress com TLS
│   └── kustomization.yaml   # Kustomize base
├── overlays/
│   ├── dev/                 # Ambiente DEV
│   ├── staging/             # Ambiente STAGING
│   └── prod/                # Ambiente PROD
└── README.md
```

## Pré-requisitos

1. **Cluster Kubernetes** (DigitalOcean DOKS)
2. **kubectl** configurado
3. **Kustomize** (incluído no kubectl v1.14+)
4. **Ingress Controller** (nginx-ingress)
5. **Cert-Manager** (para TLS automático)

## Configuração Inicial

### 1. Criar Secret para GitHub Container Registry

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --docker-email=YOUR_EMAIL \
  -n smartbet-dev  # ou staging/prod
```

### 2. Atualizar ConfigMaps

Edite os arquivos `configmap.yaml` em cada overlay com os dados do seu banco:

```yaml
data:
  db-host: "seu-host-real.db.ondigitalocean.com"
  db-port: "25060"
  db-name: "smartbet_dev"
```

### 3. Atualizar Secrets

**Opção A:** Editar `secret.yaml` (não recomendado para produção)

**Opção B:** Criar via kubectl (recomendado):
```bash
kubectl create secret generic smart-bet-secret \
  --from-literal=db-user=seu_usuario \
  --from-literal=db-password=sua_senha \
  --from-literal=jwt-secret=seu_jwt_secret \
  -n smartbet-dev
```

### 4. Atualizar Ingress

Edite o host no `kustomization.yaml` de cada overlay:
```yaml
patches:
  - patch: |-
      - op: replace
        path: /spec/rules/0/host
        value: api-dev.seudominio.com
```

## Deploy

### Ambiente DEV
```bash
kubectl apply -k k8s/overlays/dev
```

### Ambiente STAGING
```bash
kubectl apply -k k8s/overlays/staging
```

### Ambiente PROD
```bash
kubectl apply -k k8s/overlays/prod
```

## Verificar Deploy

```bash
# Ver pods
kubectl get pods -n smartbet-dev

# Ver logs
kubectl logs -f deployment/smart-bet-backend -n smartbet-dev

# Ver serviços
kubectl get svc -n smartbet-dev

# Ver ingress
kubectl get ingress -n smartbet-dev
```

## Diferenças entre Ambientes

| Config | DEV | STAGING | PROD |
|--------|-----|---------|------|
| Replicas | 1 | 2 | 2 |
| CPU Request | 100m | 250m | 250m |
| CPU Limit | 250m | 500m | 500m |
| Memory Request | 256Mi | 512Mi | 512Mi |
| Memory Limit | 512Mi | 1Gi | 1Gi |
| Spring Profile | dev | staging | prod |
| Log Level | DEBUG | INFO | WARN |
| Swagger | ✅ | ✅ | ❌ |

## Troubleshooting

### Pod não inicia
```bash
kubectl describe pod <pod-name> -n smartbet-dev
kubectl logs <pod-name> -n smartbet-dev --previous
```

### Verificar conectividade com banco
```bash
kubectl exec -it <pod-name> -n smartbet-dev -- sh
wget -qO- http://localhost:8080/actuator/health
```

### Reiniciar deployment
```bash
kubectl rollout restart deployment/smart-bet-backend -n smartbet-dev
```

## CI/CD com GitHub Actions

O workflow `.github/workflows/deploy.yml` automatiza:
1. Build da imagem Docker
2. Push para GitHub Container Registry
3. Deploy no cluster DigitalOcean

Secrets necessários no GitHub:
- `DIGITALOCEAN_ACCESS_TOKEN`
- `CLUSTER_NAME`
