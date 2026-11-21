# ViverBem API (Spring Boot 4 + Hibernate + PostgreSQL)

## Rodar
1. Java 17+ e Maven instalados. Crie o banco: `CREATE DATABASE viverbem;`.
   Defina a senha do banco na variável `DB_PASSWORD` (obrigatória; o usuário `DB_USER` tem padrão `postgres`).
   Ex. no PowerShell: `$env:DB_PASSWORD="sua_senha"` antes de `mvn spring-boot:run`.
2. (Opcional, para o assistente) defina `ANTHROPIC_API_KEY` no ambiente do servidor. Sem ela, o assistente responde que não está configurado.
3. `mvn spring-boot:run`. A API sobe em `http://localhost:8080` e o Hibernate cria as tabelas sozinho (`ddl-auto=update`).
Sem Maven instalado: abra a pasta no IntelliJ ou no VS Code (Extension Pack for Java), ou gere um projeto em start.spring.io para obter o `mvnw` e copie `src/` e `pom.xml` para ele.

## Camadas (MVC)
`model` (entidades JPA) · `repository` (Spring Data) · `service` (regras) · `controller` (REST) · `dto` (JSON) · `config` (CORS, token, erros).

## Modelo de dados (3FN)
usuario(id, nome?, idade?, email único, senha_hash, aceite_lgpd, token, token_expira_em, tentativas_falhas, bloqueado_ate) ·
supervisor(id, usuario_id → usuario, nome, parentesco, online) ·
medicamento(id, usuario_id → usuario, nome, dose, horario, orientacao, ativo) ·
tomada(id, medicamento_id → medicamento, data, tomado_em; único por medicamento+data).
O histórico (dias, sequência e adesão) é calculado a partir de `tomada`, sem dado repetido.

## Ligar o front
No app, `EXPO_PUBLIC_USE_MOCK=false` e `EXPO_PUBLIC_API_URL` apontando para esta API.
Dois ajustes de uma linha no front (a data "de hoje" precisa ir também ao marcar como tomado):
- `src/controllers/MedController.js`: `api.post(\`/api/medicamentos/${id}/tomar?data=${HOJE}\`)`
- `src/services/mock.js`: regex `/^\/api\/medicamentos\/(\d+)\/tomar(\?.*)?$/`
