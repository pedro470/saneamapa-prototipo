# SaneaMapa — Aplicação Java Spring Boot + PostgreSQL

Implementação completa do sistema **SaneaMapa** (*Sistema de Registro e Notificação de Saneamento Básico*), construída a partir do **Documento de Requisitos v6**, do **Protótipo navegável** e dos critérios de avaliação de banco de dados (3AF).

## Estrutura do Projeto

- `database/`: Scripts PostgreSQL puros (DDL com as 4 integridades, Triggers, Índices, Contas/Permissões, Views, Seed e o arquivo `ENTREGA_3AF_SQL.md` para entrega).
- `src/main/java/br/edu/cesmac/saneamapa/`:
  - `domain/model/`: Entidades ricas seguindo os **4 Pilares de POO**:
    - **Abstração**: `EntidadeAuditavel` (`@MappedSuperclass`), `Evidencia` (classe abstrata), `GeometriaCamada` (interface), `CalculadoraPontuacaoFila` (interface).
    - **Encapsulamento**: Regras de negócio protegidas dentro de `Ponto` (ciclo de vida), `Relato` (anonimato LGPD), `Usuario` (validação de CPF e bloqueio após 5 falhas), `Vistoria` (raio <= 100 m e câmera obrigatória) e `LogAuditoria` (imutabilidade).
    - **Herança**: `EvidenciaFoto`, `EvidenciaVideo`, `EvidenciaAudio` estendem `Evidencia` (`SINGLE_TABLE`); hierarquia `SaneaMapaException`.
    - **Polimorfismo**: `validarRegrasCaptura()`, `aplicarTratamentoInicial()`, `descreverTratamento()` em `Evidencia`; `contemPonto()` em `GeometriaPontoRaio` e `GeometriaPoligono`.
  - `service/`: Regras de aplicação (agrupamento em 50 m, fila ponderada, LGPD, importação CSV/GeoJSON do Censo IBGE com rollback, auditoria).
  - `security/`: Autenticação JWT (expiração de 30 minutos) e controle de acesso por perfil (`FISCAL`, `PREFEITURA` e rotas públicas LGPD do `Morador`).
  - `controller/`: Endpoints REST e tratamento padronizado de erros (`RNF03`).
- `src/main/resources/static/`: Interface web (`index.html`, `morador.html`, `fiscal.html`, `painel.html`) usando o Design System (`ds/bundle.css`) do protótipo.

## Como Executar

O caminho recomendado é o Docker, a partir da **raiz do repositório** (`docker compose up --build`); ver o `README.md` da raiz, que traz também os usuários de demonstração.

Sem Docker, com PostgreSQL e Maven instalados:

1. Crie o banco PostgreSQL `saneamapa` e execute os scripts em `database/`:
   - `01_ddl_4_integridades.sql`
   - `02_triggers.sql`
   - `03_indices.sql`
   - `04_contas_permissoes.sql`
   - `05_views.sql`
2. Configure `DB_URL`, `DB_USER` e `DB_PASS` (padrão: `jdbc:postgresql://localhost:5432/saneamapa`, `postgres` / `postgres`).
3. Execute a aplicação Spring Boot:
   ```bash
   mvn spring-boot:run
   ```
   Testes: `mvn test` (usam banco H2 em memória; não exigem PostgreSQL).
4. Acesse http://localhost:8080.
   - Prefeitura: `ana.lima@exemplo.gov.br` / `Sanea2026`
   - Fiscal: `carlos.mendes@exemplo.gov.br` / `Sanea2026`
