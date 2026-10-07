# EcoTrack MS - Monitoramento e Gestão de Coletas de Resíduos (ESG)

O **EcoTrack MS** é um microsserviço desenvolvido para o ecossistema corporativo focado em metas de sustentabilidade e governança (ESG). A aplicação tem como objetivo central automatizar o rastreamento, agendamento e a destinação correta de resíduos industriais e comerciais, garantindo que as empresas geradoras cumpram as legislações ambientais vigentes.

---

## 🛠️ O que o projeto faz? (Regras de Negócio)

A API gerencia o ciclo de vida completo de um descarte ecológico através das seguintes funcionalidades:

* **Agendamento de Coletas:** Permite que empresas parceiras registrem solicitações de descarte informando a categoria do resíduo, o peso exato e a data programada, contando com validações estritas de entrada de dados.
* **Triagem por Categoria:** Classifica automaticamente os materiais coletados de acordo com as normas ambientais em categorias específicas: `LIXO_ELETRONICO`, `PLASTICO`, `PAPEL_PAPELAO`, `METAIS` e `VIDRO`.
* **Rastreabilidade (Status de Destinação):** Cada coleta nasce com o status `AGENDADO` e está pronta para ser integrada a painéis de monitoramento que acompanham o material até a sua reciclagem ou descarte final homologado.
* **Histórico Ambiental:** Centraliza todos os dados de descarte para a geração de relatórios de auditoria verde e balanços de sustentabilidade corporativa.

---

## 🏗️ Arquitetura e Tecnologias Utilizadas

Este microsserviço foi desenhado seguindo os padrões de sistemas distribuídos e alta escalabilidade:

* **Java 21 & Spring Boot 4.0.6:** Base robusta e moderna para o desenvolvimento da API Restful.
* **Spring Data JPA & Oracle Database:** Persistência de dados com foco em integridade corporativa.
* **Flyway Database Migrations:** Controle de versão evolutivo do esquema do banco de dados da FIAP (Tabela `tb_coletas` e `seq_coletas`).
* **Spring Security & JWT (Json Web Token):** Validação de tokens via Filtro customizado (`VerificacaoTokenFilter`) para garantir a segurança descentralizada (Stateless).
* **Tratamento Avançado de Exceções:** Implementação de um manipulador global (`RestControllerAdvice`) para interceptar erros de validação (Bean Validation) e regras de negócio (como IDs não encontrados), retornando payloads limpos e padronizados.
* **Paginação de Dados:** Listagem de agendamentos otimizada utilizando `Pageable` do Spring Data, mitigando gargalos de infraestrutura no banco de dados Oracle.
* **Netflix Eureka Client:** Registro automatizado no servidor de Service Discovery.
* **Spring Cloud Gateway:** Roteamento unificado das requisições na porta `5051`.
* **Docker:** Conteinerização da aplicação através de `Dockerfile` para deploy facilitado em ambientes de nuvem.

---

## 🔐 Arquitetura de Segurança e Testes

Como este projeto adota a arquitetura de **Microsserviços**, a responsabilidade de persistência e cadastro de usuários pertence ao serviço de Autenticação global da infraestrutura.

Para que este microsserviço de negócio pudesse ser testado de forma **100% independente e isolada**, o filtro de segurança intercepta a assinatura digital do JWT emitida pelo ecossistema (Issuer: `fiap`) e constrói o contexto de segurança (`UserDetails`) dinamicamente em memória, respeitando o princípio do desacoplamento de dados. Uma rota de login simulada (`AuthController`) foi incluída para facilitar a geração de tokens legítimos durante a avaliação.

### 🚀 Como testar a API no Insomnia (Passo a Passo)

Para validar o funcionamento do CRUD completo passando pelo API Gateway, siga a ordem das pastas incluídas na Collection do Insomnia:

1.  **Pasta Autenticação -> Requisição `POST Efetuar Login`:**
    * Dispare a requisição. Ela aceita qualquer e-mail fictício no formato JSON e retornará um Token JWT legítimo assinado com a nossa Secret Key corporativa (`fiap`).
    * Copie o Token gerado (o texto contido dentro do campo `"token"`).

2.  **Pasta Coleta -> Demais requisições (`POST`, `GET`, `PUT`, `DELETE`):**
    * Vá até a aba **Auth** da requisição do CRUD que deseja testar, selecione **Bearer Token** e cole o token copiado no campo correspondente.
    * No endpoint de listagem (`GET /agendamentos`), é possível testar o recurso de paginação passando os parâmetros na URL, ex: `?size=5&page=0`.
    * Dispare as requisições para interagir com o banco de dados Oracle através do API Gateway na porta `5051`.