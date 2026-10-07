# Projeto - Cidades ESG Inteligentes

**EcoTrack** é uma API REST para **agendamento e rastreabilidade de coletas de resíduos corporativos**, apoiando metas de sustentabilidade e governança (ESG). Este repositório reúne a aplicação (Java + Spring Boot) e toda a automação DevOps do seu ciclo de vida: **containerização, pipeline de CI/CD e deploy automatizado em dois ambientes (staging e produção)**.

**Integrantes:** Thiago Andrade Silvano · Isabela dos Santos Pinto · Laura Elvira Naun · Isabelly Romano Tempesta

| Ambiente | URL | Health check |
|---|---|---|
| Staging | https://ecotrack-stg-devbelasp-end5heexh0ejgvdj.eastus-01.azurewebsites.net | [/actuator/health](https://ecotrack-stg-devbelasp-end5heexh0ejgvdj.eastus-01.azurewebsites.net/actuator/health) |
| Produção | https://ecotrack-prd-devbelasp-fqdxeghqhkfkdhbt.eastus-01.azurewebsites.net | [/actuator/health](https://ecotrack-prd-devbelasp-fqdxeghqhkfkdhbt.eastus-01.azurewebsites.net/actuator/health) |

> A primeira requisição após um período sem uso pode demorar alguns segundos: o banco gratuito (Neon) suspende por inatividade e o plano do Azure é de entrada.

Repositório: https://github.com/devbelasp/eco-track · Imagem: https://hub.docker.com/r/devbelasp/eco-track

---

## O que a API faz

Base: `/api/residuos/agendamentos`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/residuos/agendamentos` | Agenda uma coleta (status inicial `AGENDADO`) |
| GET | `/api/residuos/agendamentos?page=0&size=5` | Lista paginada |
| GET | `/api/residuos/agendamentos/{id}` | Busca por ID |
| PUT | `/api/residuos/agendamentos/{id}` | Atualiza os dados (o status é preservado) |
| DELETE | `/api/residuos/agendamentos/{id}` | Cancela o agendamento |
| GET | `/actuator/health` | Saúde da aplicação (inclui a conexão com o banco) |

Exemplo de corpo (a data deve ser de hoje em diante):

```json
{
  "empresaGeradora": "Tech Recicla Eletronicos S/A",
  "categoriaResiduo": "LIXO_ELETRONICO",
  "pesoKg": 230.75,
  "dataAgendamento": "2027-01-15"
}
```

Categorias aceitas: `PLASTICO`, `PAPEL_PAPELAO`, `LIXO_ELETRONICO`, `METAIS`, `VIDRO`. Erros seguem o formato `{"erro": "..."}` (validações incluem também `"campos"`), com status 400, 404 ou 409.

Para testar a API com um cliente HTTP, o repositório inclui uma coleção do [Insomnia](https://insomnia.rest/) com os três ambientes (Local, Staging e Produção): [`docs/insomnia/Insomnia_EcoTrack.yaml`](docs/insomnia/Insomnia_EcoTrack.yaml). Para usar, baixe o arquivo, importe no Insomnia (**Import**) e escolha o ambiente no seletor. Quem preferir pode chamar os endereços da tabela acima diretamente, por exemplo `GET <URL do ambiente>/api/residuos/agendamentos`.

---

## Como executar localmente com Docker

**Pré-requisito:** Docker Desktop em execução.

1. Clone o repositório e entre na pasta:
   ```bash
   git clone https://github.com/devbelasp/eco-track.git
   cd eco-track
   ```
2. Crie o arquivo de variáveis a partir do modelo e **troque a senha**:
   ```bash
   cp .env.example .env        # PowerShell: Copy-Item .env.example .env
   ```
   Edite o `.env` e defina `POSTGRES_PASSWORD` (use só letras e números). O `.env` está no `.gitignore` e nunca vai para o Git.
3. Suba a aplicação e o banco:
   ```bash
   docker compose up --build
   ```
4. Quando os logs mostrarem `Started EcoTrackMsApplication`, teste:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Resposta esperada: `"status":"UP"`. A API está em `http://localhost:8080/api/residuos/agendamentos`.
5. Para parar: `docker compose down` (mantém os dados) ou `docker compose down -v` (apaga também o volume do banco).

**Rodar os testes** (não precisam de Docker nem de banco instalado):

```bash
./mvnw test                 # Windows: .\mvnw.cmd test
```

São 20 testes: 8 unitários do service (Mockito), 11 de integração do controller (MockMvc, com as migrations reais do Flyway) e o `contextLoads`. Localmente, os testes de integração usam H2 em memória (modo PostgreSQL), por isso rodam sem Docker. No CI, o mesmo comando roda contra um **PostgreSQL 16 real** (service container), porque a conexão vem das variáveis `TEST_DATABASE_URL`, `TEST_DATABASE_USER` e `TEST_DATABASE_PWD`.

---

## Pipeline CI/CD

**Ferramenta:** GitHub Actions. Os workflows estão em [`.github/workflows/`](.github/workflows/).

```mermaid
flowchart LR
    A["feature/*"] -->|"Pull Request"| B["develop"]
    B -->|"merge: push em develop"| C["CD: testes, imagem, deploy"]
    C --> D["STAGING"]
    B -->|"Pull Request"| E["main"]
    E -->|"merge: push em main"| F["CD: testes, imagem, deploy"]
    F --> G["PRODUÇÃO"]
```

### CI — `ci.yml`
Roda em todo **Pull Request** para `develop` ou `main` e pode ser reutilizado por outros workflows.

| Job | O que faz |
|---|---|
| Build e testes | Java 21 (Temurin), `mvn -B verify`: compila, roda os 20 testes e empacota o `app.jar`. Sobe um PostgreSQL 16 (service container) usado pelos testes de integração. Anexa o relatório dos testes |
| Build da imagem Docker | Valida o Dockerfile (somente em Pull Request, sem publicar) |

### CD — `cd.yml`
Roda a cada **push** em `develop` (staging) ou `main` (produção), ou seja, quando um Pull Request é mesclado.

| Job | O que faz |
|---|---|
| CI (build e testes) | Reutiliza o `ci.yml`. Se um teste falhar, nada é publicado |
| Publicar imagem no Docker Hub | Constrói a imagem e publica `devbelasp/eco-track` com duas tags: o **SHA do commit** e o **nome da branch** |
| Deploy em staging | Só em `develop`. Publica a imagem no Web App de staging |
| Deploy em producao | Só em `main`. Publica a imagem no Web App de produção |
| Smoke test | Após cada deploy, consulta `/actuator/health` (até 20 tentativas) e exige `"status":"UP"` |

### Ambientes, segredos e variáveis
Nenhum segredo está no código. As informações sensíveis ficam em:

| Onde | Nome | Conteúdo |
|---|---|---|
| GitHub (repositório) | `DOCKERHUB_TOKEN` | Token do Docker Hub para publicar a imagem |
| GitHub Environment `staging` / `production` | `AZURE_WEBAPP_PUBLISH_PROFILE` (secret) | Perfil de publicação do Web App do ambiente |
| GitHub Environment `staging` / `production` | `AZURE_WEBAPP_NAME` (variável) | Nome do Web App do ambiente |
| Azure Web App (cada ambiente) | `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PWD`, `WEBSITES_PORT` | Conexão com o PostgreSQL do ambiente e porta do contêiner |

Staging e produção usam **bancos PostgreSQL separados** (dois projetos no Neon), então os dados nunca se misturam. O Environment `production` aceita regra de aprovação manual antes do deploy.

### Infraestrutura
- **Azure App Service (Web App for Containers, Linux, plano Básico B1):** um plano compartilhado (`plan-eco-track`) com dois Web Apps, `ecotrack-stg-devbelasp` e `ecotrack-prd-devbelasp`, no grupo de recursos `rg-eco-track`.
- **Banco de dados:** PostgreSQL no Neon (fora do Azure, pois a assinatura de estudante restringe as regiões para bancos), um projeto por ambiente. O Flyway cria as tabelas na primeira subida da aplicação.

---

## Containerização

### Dockerfile

```dockerfile
# ---------- Etapa 1: build ----------
# Imagem com Maven + JDK 21 para compilar o projeto e gerar o app.jar
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /opt/app

COPY pom.xml .
COPY src ./src

# Os testes ja rodam no pipeline de CI; aqui so empacotamos
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
# Imagem leve, apenas com o JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app

# Usuario sem privilegios (a aplicacao nao precisa rodar como root)
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /opt/app/target/app.jar app.jar
USER spring

# Porta padrao (pode ser alterada pela variavel SERVER_PORT)
ENV SERVER_PORT=8080
# A JVM respeita o limite de memoria do container
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

# Container "saudavel" = /actuator/health respondendo 200
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO /dev/null http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Estratégias adotadas
- **Multi-stage build:** o Maven e o JDK ficam só na etapa de build. A imagem final leva apenas o JRE, o que a deixa menor e mais segura.
- **Build reproduzível:** o `pom.xml` define o nome fixo `app.jar`; a imagem não depende de nada pré-compilado na máquina.
- **Usuário sem privilégios:** a aplicação não roda como `root`.
- **Configuração por variáveis de ambiente:** nenhuma senha na imagem; a mesma imagem sobe em local, staging e produção.
- **Healthcheck:** o contêiner só é considerado saudável quando `/actuator/health` responde.
- **Limite de memória respeitado:** `MaxRAMPercentage=75` faz a JVM usar o limite do contêiner.
- **`.dockerignore`:** evita copiar `target/`, `.git` e o `.env` para a imagem.

### Docker Compose (`docker-compose.yml`)
- **Dois serviços:** `api` (a aplicação) e `db` (PostgreSQL 16).
- **Volume nomeado** `db_data`: os dados do banco sobrevivem a `docker compose down`.
- **Rede própria** `ecotrack-net`: a API alcança o banco pelo nome do serviço (`db`).
- **Variáveis de ambiente** vindas do `.env` (modelo em [`.env.example`](.env.example)), com falha explícita se faltarem.
- **Ordem de subida:** `depends_on` com `service_healthy`: a API só inicia depois que o banco responde.
- A porta do banco é publicada apenas em `127.0.0.1`.

---

## Prints do funcionamento

### 1. Ambiente local (testes e Docker Compose)

**Execução local (H2): 11 testes de integração do controller**

![Execução local (H2): 11 testes de integração do controller](docs/prints/01a-testes-integracao.png)

**Execução local (H2): 20 testes, 0 falhas, BUILD SUCCESS**

![Execução local (H2): 20 testes, 0 falhas, BUILD SUCCESS](docs/prints/01b-testes-resumo.png)

**Flyway aplicando as migrations V1 e V2 no PostgreSQL**

![Flyway aplicando as migrations V1 e V2 no PostgreSQL](docs/prints/06a-flyway-logs.png)

**Aplicação iniciada no contêiner**

![Aplicação iniciada no contêiner](docs/prints/06b-api-started.png)

**docker compose ps: API e banco saudáveis**

![docker compose ps: API e banco saudáveis](docs/prints/02-compose-ps.png)

**Imagem Docker criada (eco-track-api)**

![Imagem Docker criada (eco-track-api)](docs/prints/03-docker-images.png)

**Health check local**

![Health check local](docs/prints/04-health-local.png)

**POST local: 201 Created**

![POST local: 201 Created](docs/prints/05a-api-post.png)

**GET local: 200 OK (listagem paginada)**

![GET local: 200 OK (listagem paginada)](docs/prints/05b-api-get.png)


### 2. Pipeline de CI/CD

**Pull Request de develop para main com checks verdes**

![Pull Request de develop para main com checks verdes](docs/prints/14c-pr-develop-main.png)

**CI: job Build e testes, com o contêiner do PostgreSQL inicializado (Initialize containers)**

![CI: job Build e testes, com o contêiner do PostgreSQL inicializado (Initialize containers)](docs/prints/07a-ci-job-passos.png)

**CI: variáveis TEST_DATABASE_* apontando os testes para o PostgreSQL (jdbc:postgresql://localhost:5432/ecotrack_test)**

![CI: variáveis TEST_DATABASE_* apontando os testes para o PostgreSQL (jdbc:postgresql://localhost:5432/ecotrack_test)](docs/prints/07d-ci-postgres-real.png)

**CI: 20 testes executados no runner do GitHub, BUILD SUCCESS**

![CI: 20 testes executados no runner do GitHub, BUILD SUCCESS](docs/prints/07b-ci-testes.png)

**CI: build da imagem Docker**

![CI: build da imagem Docker](docs/prints/07c-ci-docker-build.png)

**CD: build, testes e publicação da imagem**

![CD: build, testes e publicação da imagem](docs/prints/08a-cd-publicar-imagem.png)

**Docker Hub: imagem publicada com as tags develop e SHA do commit**

![Docker Hub: imagem publicada com as tags develop e SHA do commit](docs/prints/08b-dockerhub-repositorio.png)


### 3. Infraestrutura (Azure, GitHub e Neon)

**Azure: grupo de recursos com o plano e os dois Web Apps**

![Azure: grupo de recursos com o plano e os dois Web Apps](docs/prints/10a-azure-resource-group.png)

**GitHub: Environments staging e production**

![GitHub: Environments staging e production](docs/prints/13e-github-environments.png)

**Neon: um projeto PostgreSQL por ambiente**

![Neon: um projeto PostgreSQL por ambiente](docs/prints/09-neon-projetos.png)


### 4. Staging

**CD na branch develop: deploy em staging**

![CD na branch develop: deploy em staging](docs/prints/13a-deploy-staging.png)

**Deploy da imagem no Web App de staging**

![Deploy da imagem no Web App de staging](docs/prints/13d-deploy-azure-passo.png)

**Smoke test do staging: status UP**

![Smoke test do staging: status UP](docs/prints/13a2-staging-smoke-test.png)

**Staging: health check no Azure**

![Staging: health check no Azure](docs/prints/11a-staging-health.png)

**Staging: POST 201 Created**

![Staging: POST 201 Created](docs/prints/11b-staging-api-post.png)

**Staging: GET 200 OK**

![Staging: GET 200 OK](docs/prints/11c-staging-api-get.png)

**Banco de staging (Neon): tabela tb_coletas com dados**

![Banco de staging (Neon): tabela tb_coletas com dados](docs/prints/14b-neon-staging-tabelas.png)


### 5. Produção

**CD na branch main: deploy em produção**

![CD na branch main: deploy em produção](docs/prints/13b-deploy-producao.png)

**Deploy da imagem no Web App de produção**

![Deploy da imagem no Web App de produção](docs/prints/13d2-deploy-azure-producao.png)

**Smoke test da produção: status UP**

![Smoke test da produção: status UP](docs/prints/13b2-producao-smoke-test.png)

**Produção: health check no Azure**

![Produção: health check no Azure](docs/prints/12a-producao-health.png)

**Produção: POST 201 Created**

![Produção: POST 201 Created](docs/prints/12b-producao-api-post.png)

**Produção: GET 200 OK**

![Produção: GET 200 OK](docs/prints/12c-producao-api-get.png)

**Produção: DELETE 204 No Content**

![Produção: DELETE 204 No Content](docs/prints/12d-producao-delete.png)

**Produção: listagem após o DELETE**

![Produção: listagem após o DELETE](docs/prints/12e-producao-get-apos-delete.png)

**Banco de produção (Neon): tabela tb_coletas com dados**

![Banco de produção (Neon): tabela tb_coletas com dados](docs/prints/14a-neon-producao-tabelas.png)


---

## Tecnologias utilizadas

- **Linguagem e framework:** Java 21, Spring Boot 4.0.6 (Web MVC, Data JPA, Validation, Actuator)
- **Banco de dados:** PostgreSQL 16 (Docker Compose local e Neon em staging/produção), migrations versionadas com Flyway
- **Testes:** JUnit 5, Mockito, MockMvc; PostgreSQL 16 real no CI (service container) e H2 em memória como padrão local
- **Containers:** Docker (multi-stage build), Docker Compose
- **CI/CD:** GitHub Actions (workflows reutilizáveis, GitHub Environments), Docker Hub
- **Nuvem:** Azure App Service (Web App for Containers), Neon (PostgreSQL gerenciado)
- **Ferramentas:** Maven, Insomnia, Git/GitHub

## Decisões de projeto e limitações

- **Testes em PostgreSQL real no CI:** o workflow sobe um PostgreSQL 16 como service container e os testes de integração rodam contra ele, com as mesmas migrations do Flyway usadas nos ambientes. Sem as variáveis `TEST_DATABASE_*` (execução local), os testes usam H2 em memória, para rodar sem Docker.
- **Banco fora do Azure:** a assinatura Azure for Students restringe as regiões onde bancos podem ser criados. Por isso o PostgreSQL de staging e produção está no Neon. A aplicação só lê variáveis de ambiente, então trocar de banco não exige alterar código.
- **Branch `production` no Neon:** é o nome que o Neon dá à branch principal de todo projeto, inclusive o de staging. Não tem relação com o nosso ambiente de produção.
- **Smoke test:** confirma que o ambiente está saudável após o deploy. A confirmação de qual imagem foi implantada vem do log do passo de deploy, que mostra a tag do commit.
- **Sem autenticação:** o login simulado e o Eureka do projeto original foram removidos para este trabalho, que foca na automação do ciclo de vida. Os endpoints são públicos.
- **Aprovação manual:** o Environment `production` do GitHub suporta regra de aprovação; ela pode ser ativada nas configurações do repositório.

## Estrutura do repositório

```
eco-track/
├── .github/workflows/     # ci.yml e cd.yml
├── docs/
│   ├── insomnia/          # coleção de requisições (Local, Staging, Produção)
│   └── prints/            # evidências usadas neste README
├── src/                   # código-fonte e testes
├── .dockerignore
├── .env.example           # modelo das variáveis de ambiente
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Checklist de entrega

| Item | OK |
|---|---|
| Projeto compactado em .ZIP com estrutura organizada | ☑ |
| Dockerfile funcional | ☑ |
| docker-compose.yml ou arquivos Kubernetes | ☑ |
| Pipeline com etapas de build, teste e deploy | ☑ |
| README.md com instruções e prints | ☑ |
| Documentação técnica com evidências (PDF ou PPT) | ☑ |
| Deploy realizado nos ambientes staging e produção | ☑ |
