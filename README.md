# AgendaPro API

[![Backend CI](https://github.com/msampaio-dev/AgendaPro/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/msampaio-dev/AgendaPro/actions/workflows/backend-ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

![AgendaPro — agendamento para barbearias](https://raw.githubusercontent.com/msampaio-dev/AgendaPro-Web/main/public/og-agendapro.png)

API de agendamento para barbearias, em Java 21 e Spring Boot. Cada unidade tem equipe, catálogo e horário de funcionamento próprios; o cliente escolhe onde e com quem quer ser atendido; e duas reservas para o mesmo horário, chegando no mesmo instante, nunca são aceitas juntas — garantia do banco, não só do código.

> **Experimente o produto:** [agenda-pro-web-agendapro2.vercel.app](https://agenda-pro-web-agendapro2.vercel.app/)
> **Frontend:** [msampaio-dev/AgendaPro-Web](https://github.com/msampaio-dev/AgendaPro-Web)

## O que torna este projeto interessante

- **Reserva dupla impossível:** duas pessoas nunca ficam com o mesmo horário, nem quando clicam no mesmo instante — a aplicação verifica e o PostgreSQL garante.
- **Disponibilidade calculada:** horários livres consideram jornada do profissional, funcionamento da unidade, almoço, bloqueios, horários extras e reservas existentes.
- **Múltiplas barbearias:** profissionais, serviços, preços, equipe e histórico pertencem à unidade correta.
- **Tempo modelado corretamente:** horários locais usam `LocalDate` e `LocalTime`; reservas são persistidas como `Instant`, respeitando o `ZoneId` do profissional.
- **Autorização por contexto:** cliente, profissional, proprietário da barbearia e administrador possuem permissões diferentes.
- **Banco versionado:** o Hibernate valida o schema, enquanto o Flyway controla todas as mudanças.
- **Testes isolados:** integrações executam em PostgreSQL descartável com Testcontainers e nunca acessam o banco local.

## O produto

O cliente escolhe a barbearia, o profissional e o serviço — com barba como adicional — e vê apenas horários livres. O profissional cuida da própria agenda, da jornada, do almoço e dos bloqueios. O proprietário administra a unidade, a equipe e o catálogo, com preços próprios. E o administrador acompanha a plataforma inteira.

A lista completa de funcionalidades por perfil está no [README do frontend](https://github.com/msampaio-dev/AgendaPro-Web#experiência-por-perfil).

## A regra mais importante

Um profissional nunca pode possuir dois agendamentos que ocupem o mesmo intervalo.

```text
Reserva existente:  14:00 ───────── 14:30
Nova tentativa:            14:15 ───────── 14:45  → rejeitada
Intervalo adjacente:                     14:30 ─── 15:00  → permitido
```

A proteção acontece em duas camadas:

1. o service verifica previamente se existe sobreposição;
2. o PostgreSQL aplica uma exclusion constraint sobre `tstzrange`.

Mesmo que duas requisições sejam processadas praticamente ao mesmo tempo, apenas uma consegue reservar o intervalo. Essa garantia é comprovada por teste de concorrência com PostgreSQL real.

## Arquitetura

O AgendaPro é um **monólito modular organizado por funcionalidade**. Essa escolha mantém o deploy simples sem transformar o código em um pacote único e acoplado.

```text
com.agendapro
├── auth
├── usuario
├── profissional
├── barbearia
├── profissionalservico
├── servico
├── disponibilidade
├── agendamento
├── security
└── shared
```

```text
HTTP/JSON
   │
   ▼
Controller ── DTOs + validação
   │
   ▼
Service ───── regras de negócio + transações
   │
   ▼
Repository ── Spring Data JPA
   │
   ▼
PostgreSQL ── constraints + integridade + concorrência
```

Os controllers lidam com o contrato HTTP. Os services coordenam casos de uso e transações. As entidades protegem seu próprio ciclo de vida. Os repositories isolam a persistência.

## Decisões técnicas

| Decisão | Por quê |
|---|---|
| Monólito modular | Um único deploy, com cada funcionalidade no seu pacote. Dividir em microsserviços só compensaria com várias equipes ou partes que precisem escalar separadas. |
| DTOs na entrada e na saída | A API conversa com o cliente por contratos próprios. Mudar uma tabela não quebra quem consome. |
| `ddl-auto=validate` | O Hibernate só confere o banco, nunca altera. O schema muda apenas por migration. |
| Flyway | Todo ambiente chega ao mesmo banco pelo mesmo caminho, e cada mudança fica versionada como código. |
| JWT stateless | O servidor não guarda sessão: o frontend envia o token a cada requisição, sem depender de cookie. |
| BCrypt | É lento de propósito. Mesmo que o banco vaze, descobrir as senhas por tentativa fica caro demais. |
| `Instant` no banco | Guarda o momento exato, sem ambiguidade de fuso. O horário local é calculado com o fuso de cada profissional. |
| Constraint de exclusão | O próprio PostgreSQL recusa dois horários sobrepostos, mesmo que a aplicação falhe. |
| Testcontainers | A regra mais importante depende de recursos exclusivos do PostgreSQL. Testar num banco em memória não provaria que ela funciona. |
| Desativação lógica | Registros são desativados, não apagados: o histórico de agendamentos continua íntegro. |

## Stack

- Java 21
- Spring Boot 3.5
- Spring Web, Validation e Data JPA
- Spring Security e JWT
- PostgreSQL e Flyway
- JUnit 5, Mockito e Testcontainers
- Swagger/OpenAPI
- Maven Wrapper
- Docker e Docker Compose
- GitHub Actions

## Executando localmente

### Pré-requisitos

- JDK 21;
- PostgreSQL;
- Docker Desktop para os testes de integração.

Crie o banco `agendapro` e o usuário `agendapro_user`. Depois configure:

| Variável | Uso |
|---|---|
| `DB_PASSWORD` | Senha do PostgreSQL local |
| `JWT_SECRET` | Chave Base64 com pelo menos 32 bytes |

Uma chave de desenvolvimento pode ser gerada no PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Inicie a API:

```powershell
.\mvnw.cmd spring-boot:run
```

O perfil padrão é `dev`, com PostgreSQL em `localhost:5432`. A API estará em `http://localhost:8080/api/v1`.

### Docker Compose

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Preencha `DB_PASSWORD` e `JWT_SECRET` no `.env`. O arquivo é ignorado pelo Git e não deve ser versionado.

Para encerrar sem apagar os dados:

```powershell
docker compose down
```

## Documentação da API

Com o projeto local em execução:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Health check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

Principais grupos de endpoints:

| Recurso | Exemplos |
|---|---|
| Autenticação | `POST /auth/login`, `GET /auth/me` |
| Usuários | cadastro, atualização, desativação e reativação |
| Barbearias | gestão da unidade, proprietário, equipe, foto e funcionamento |
| Serviços | catálogo por barbearia e associação com profissionais |
| Disponibilidade | jornada, almoço, bloqueios, horários extras e consulta de vagas |
| Agendamentos | criação, filtros, confirmação, cancelamento e conclusão |

Todos os endpoints da aplicação utilizam o prefixo `/api/v1`.

## Testes e qualidade

```powershell
.\mvnw.cmd verify
```

Na última verificação local, **164 testes** passaram sem falhas. A suíte inclui:

- testes unitários de services e entidades;
- Mockito para colaboradores isolados;
- testes HTTP de autenticação, validação, CORS e autorização;
- migrations Flyway aplicadas do zero;
- PostgreSQL real com Testcontainers;
- teste de concorrência para reservas simultâneas;
- persistência e validação de imagens;
- bootstrap seguro do primeiro administrador.

O GitHub Actions executa `mvn verify` com Java 21 em todo push e pull request para `main`.

## Segurança

- senhas armazenadas exclusivamente com BCrypt;
- JWT assinado, com emissor e expiração validados;
- autorização por perfil, proprietário e titular do recurso;
- mensagens de login que não revelam se um e-mail existe;
- CORS restrito às origens configuradas;
- segredos fornecidos por variáveis de ambiente;
- bootstrap administrativo opcional e de uso único;
- upload limitado a JPEG/PNG, com tamanho e conteúdo validados;
- container executado com usuário sem privilégios administrativos.

### Primeiro administrador

Não existe endpoint público para transformar uma conta em `ADMIN`. O primeiro administrador é criado por bootstrap controlado por ambiente:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED="true"
$env:BOOTSTRAP_ADMIN_NAME="Administrador"
$env:BOOTSTRAP_ADMIN_EMAIL="admin@agendapro.com"
$env:BOOTSTRAP_ADMIN_PASSWORD="defina-uma-senha-forte"
.\mvnw.cmd spring-boot:run
```

Depois da criação, desative `BOOTSTRAP_ADMIN_ENABLED` e remova as variáveis sensíveis.

## Banco e migrations

As migrations ficam em `src/main/resources/db/migration`. O Hibernate está configurado com `ddl-auto=validate`: ele verifica a compatibilidade das entidades, mas não cria nem modifica tabelas.

Uma migration já aplicada não deve ser editada. Toda evolução de schema recebe uma nova versão, preservando ambientes existentes e o histórico do banco.

## Demonstração online

O frontend está publicado na Vercel e a API em infraestrutura gratuita. Quando o backend está hibernando, a primeira requisição pode levar até cerca de um minuto enquanto o serviço sobe — a tela de login avisa que isso está acontecendo.

A conta demonstrativa exibida na tela de login permite explorar também os fluxos do profissional. Os dados desse ambiente são fictícios e compartilhados; credenciais administrativas não são publicadas.

**Acesse:** [https://agenda-pro-web-agendapro2.vercel.app](https://agenda-pro-web-agendapro2.vercel.app/)

## Limitações conhecidas

Decisões tomadas com consciência do custo, registradas aqui para quem avalia o projeto:

- **Arranque a frio de até um minuto.** O plano gratuito hiberna o serviço após cerca de quinze minutos sem tráfego. A aplicação aquece o caminho crítico assim que sobe, e o frontend avisa quem espera em vez de parecer travado, mas o tempo de despertar só desaparece em plano pago.
- **Imagens no banco, não em object storage.** O disco do plano gratuito é efêmero e apagava as fotos a cada implantação. Guardá-las em `BYTEA` resolveu com a infraestrutura que já existia; em outro volume, a escolha correta seria S3 ou equivalente. A troca está contida em `ArmazenamentoImagemService`.
- **Fotos gravadas no tamanho original.** Ainda não há redimensionamento no upload, então uma capa pode ocupar alguns megabytes.
- **Observabilidade mínima.** Existe health check, mas não há métricas nem rastreamento distribuído.
- **Ambiente demonstrativo compartilhado.** Os dados são fictícios e qualquer visitante pode alterá-los.

## Autor

Desenvolvido por **Marcelo Sampaio** como projeto de portfólio e estudo prático de engenharia de software com Java, Spring Boot, PostgreSQL e React.

- GitHub: [@msampaio-dev](https://github.com/msampaio-dev)
- Frontend: [AgendaPro-Web](https://github.com/msampaio-dev/AgendaPro-Web)

---

Se você chegou até aqui, obrigado pelo tempo. O raciocínio por trás de cada decisão está nas mensagens de commit: o `git log` conta a história do projeto, inclusive os erros e como foram corrigidos.
