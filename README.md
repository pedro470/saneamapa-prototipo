# SaneaMapa

Sistema de Registro e Notificação de Saneamento Básico com backend em **Java Spring Boot**, banco de dados **PostgreSQL** e interface web integrada com **OpenStreetMap / Leaflet**.

## Estrutura

- `saneamapa-app/`: Aplicação Spring Boot (API REST, segurança JWT, regras de domínio em POO, frontend estático em `src/main/resources/static/` e scripts SQL em `database/`).
- `docker-compose.yml`: Orquestração da aplicação junto ao PostgreSQL.
- `Documento de Requisitos - SaneaMapa v6.docx` e `requisitos/`: Documentação de requisitos do sistema.

## Como executar

Na raiz do projeto, execute:

```bash
docker compose up --build
```

Depois, acesse `http://localhost:8080`.

### Usuários de demonstração

Criados automaticamente na primeira vez que o app sobe com o banco vazio. A senha de todos é `Sanea2026`.

| Perfil | Página | E-mail |
|---|---|---|
| Prefeitura | `http://localhost:8080/painel.html` | `ana.lima@exemplo.gov.br` |
| Fiscal | `http://localhost:8080/fiscal.html` | `carlos.mendes@exemplo.gov.br` |
| Morador | `http://localhost:8080/morador.html` | não precisa de login |

### Portas e variáveis

- App: porta `8080`. PostgreSQL: porta `5433` no computador (usuário e senha `postgres`), para abrir o banco num cliente SQL.
- `JWT_SECRET`, `DB_PASS`: opcionais, lidas de um `.env` na raiz (ver Observações).
- Os scripts SQL e a entrega de banco de dados estão em `saneamapa-app/database/` (ver `ENTREGA_3AF_SQL.md`).

### Observações

- **Vistoria e GPS (RS008):** a posição do fiscal é lida pelo GPS no momento de salvar e precisa estar a até 100 m do ponto. Para demonstrar longe de Maceió, simule a localização no navegador (Chrome: DevTools → ⋮ → More tools → Sensors → Location) com a coordenada do ponto.
- **Sessão (JWT):** sem a variável `JWT_SECRET`, o app gera uma chave nova a cada inicialização, e quem estava logado precisa entrar de novo depois de um reinício. Para manter os logins, defina `JWT_SECRET` (64+ caracteres) num arquivo `.env` na raiz, fora do git.
- **Banco já criado antes destas mudanças:** os scripts de `database/` só rodam num volume novo. Para recriar o banco, use `docker compose down -v` e suba de novo (isso apaga os dados locais).
