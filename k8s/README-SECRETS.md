# Gerenciamento de Secrets no Kubernetes

## Configuração Inicial

Os secrets da aplicação são gerenciados automaticamente pelo CI/CD via GitHub Secrets.

### 1. Configurar GitHub Secrets

Acesse: `Settings → Secrets and variables → Actions → New repository secret`

#### Para Produção:
```
DB_USER_PROD = doadmin
DB_PASSWORD_PROD = <senha do banco em produção>
JWT_SECRET_PROD = <base64 de 32 bytes aleatórios>
```

#### Para Staging:
```
DB_USER_STAGING = <usuário do banco staging>
DB_PASSWORD_STAGING = <senha do banco staging>
JWT_SECRET_STAGING = <base64 de 32 bytes aleatórios>
```

### 2. Gerar JWT Secret

**PowerShell:**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

**Linux/Mac:**
```bash
openssl rand -base64 32
```

## Fluxo Automático

Quando você faz push para `main`, `staging` ou `development`, o CI/CD:

1. Lê os secrets do GitHub
2. Cria/atualiza o secret `smart-bet-secret` no namespace correspondente
3. Faz deploy da aplicação

## Verificação Manual

Para verificar se os secrets foram aplicados corretamente:

```bash
# Ver se o secret existe
kubectl get secret smart-bet-secret -n smartbet-prod

# Ver as chaves (não mostra os valores)
kubectl describe secret smart-bet-secret -n smartbet-prod

# Decodificar valor (CUIDADO: mostra credenciais!)
kubectl get secret smart-bet-secret -n smartbet-prod -o jsonpath='{.data.db-user}' | base64 -d
```

## Atualização Manual (Emergência)

Se precisar atualizar manualmente:

```bash
kubectl create secret generic smart-bet-secret \
  --from-literal=db-user="doadmin" \
  --from-literal=db-password="SUA_SENHA" \
  --from-literal=jwt-secret="SEU_JWT_SECRET" \
  -n smartbet-prod \
  --dry-run=client -o yaml | kubectl apply -f -

# Forçar redeploy
kubectl rollout restart deployment smart-bet-backend -n smartbet-prod
```

## Segurança

- ❌ **NUNCA** commite credenciais reais no Git
- ✅ Os arquivos `smart-bet-secret-*.ps1` estão no `.gitignore`
- ✅ Use GitHub Secrets para armazenar credenciais
- ✅ Rotacione secrets periodicamente
- ✅ Use secrets diferentes para cada ambiente (dev, staging, prod)

## Troubleshooting

### Erro: "password authentication failed"

O secret não foi criado ou está com valor placeholder:

```bash
# Verificar valor atual
kubectl get secret smart-bet-secret -n smartbet-prod -o jsonpath='{.data.db-password}' | base64 -d

# Se mostrar "CHANGE_ME", atualize via CI/CD ou manualmente
```

### Pod não inicia após atualizar secret

O pod precisa ser recriado para pegar novos valores:

```bash
kubectl rollout restart deployment smart-bet-backend -n smartbet-prod
```
