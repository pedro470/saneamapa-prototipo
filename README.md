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
