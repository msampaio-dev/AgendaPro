# AgendaPro — Sistema de Agendamento de Serviços

API REST para clínicas, barbearias, consultórios e profissionais autônomos
administrarem serviços, horários, disponibilidade e reservas.

O projeto foi construído como um monólito modular, com foco em regras de negócio,
segurança, consistência transacional e evolução gradual para um produto completo.

## Tecnologias

- Java 21
- Spring Boot 3.5
- Maven Wrapper
- Spring Web e Validation
- Spring Data JPA e Hibernate
- Spring Security e JWT
- PostgreSQL e Flyway
- JUnit 5, Mockito e Testcontainers
- Swagger/OpenAPI
- Docker e Docker Compose
- GitHub Actions

## Funcionalidades

- Cadastro e atualização de usuários
- Autenticação com senha BCrypt e emissão de JWT
- Perfis `CLIENTE`, `PROFISSIONAL` e `ADMIN`
- Cadastro de profissionais e serviços
- Barbearias com proprietário, endereço, foto e horários de funcionamento
- Administração da equipe e dos serviços pelo proprietário da unidade
- Associação entre profissionais e serviços
- Horários semanais de atendimento
- Bloqueios e disponibilidades extras
- Cálculo de horários disponíveis
- Criação, confirmação, cancelamento e conclusão de agendamentos
- Histórico por profissional e por cliente
- Filtros por período e status
- Paginação e ordenação
- Erros HTTP padronizados
- API versionada em `/api/v1`
- Bootstrap seguro e opcional do primeiro administrador
- CORS configurável para o frontend
- Documentação interativa com Swagger UI

## Regras de negócio importantes

Um profissional nunca pode possuir dois agendamentos que ocupem o mesmo intervalo.
A regra é protegida em duas camadas:

1. A aplicação verifica previamente a existência de sobreposição.
2. O PostgreSQL utiliza uma constraint de exclusão sobre um `tstzrange`.

A proteção do banco evita reservas duplicadas mesmo quando duas requisições chegam
praticamente ao mesmo tempo. Intervalos adjacentes continuam permitidos.

Outras regras:

- Somente profissionais e serviços ativos participam de novos agendamentos.
- Um horário precisa pertencer à disponibilidade calculada.
- Agendamentos no passado são rejeitados.
- Um atendimento só pode ser concluído depois do horário final.
- Horários são convertidos para `Instant` e armazenados como instante absoluto.
- Cada profissional possui seu próprio `ZoneId`.
- A agenda do profissional é limitada pelo funcionamento da barbearia, inclusive intervalos de almoço.
- Cada barbearia ativa possui um proprietário profissional e cada profissional pode possuir uma unidade ativa.
- A propriedade deve ser transferida para outro membro ativo antes de desativar ou transferir o proprietário.
- Agendamentos guardam a barbearia original para preservar corretamente o histórico após transferências.
- Horários inexistentes ou ambíguos por mudança de fuso são rejeitados.

## Arquitetura

O código é organizado por funcionalidade:

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

Cada módulo contém apenas os pacotes necessários, como `controller`, `service`,
`repository`, `entity`, `dto` e `exception`.

Fluxo principal:

```text
requisição HTTP → controller → service → repository → PostgreSQL
                         ↓
                    regras de negócio
```

Os controllers recebem e devolvem DTOs. As regras ficam nos services ou nas
entidades quando pertencem diretamente ao ciclo de vida do domínio.

## Executando localmente

Pré-requisitos:

- JDK 21
- PostgreSQL
- Docker Desktop para os testes com Testcontainers

Sem perfil explícito, a aplicação utiliza `dev`, com PostgreSQL em
`localhost:5432`. O perfil `test` é ativado pelos testes e sempre usa
Testcontainers. O perfil `prod` exige banco, JWT e origens CORS definidos por
variáveis de ambiente, sem valores locais como fallback.

Crie o banco `agendapro` e o usuário `agendapro_user`. Depois defina as variáveis
de ambiente `DB_PASSWORD` e `JWT_SECRET`.

O segredo JWT precisa estar em Base64 e representar pelo menos 32 bytes. No
PowerShell, uma chave de desenvolvimento pode ser gerada assim:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Execute a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

### Criando o primeiro administrador

Não existe endpoint público para promover usuários a `ADMIN`. Para criar o
primeiro administrador, defina temporariamente estas variáveis antes de iniciar a
aplicação:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED="true"
$env:BOOTSTRAP_ADMIN_NAME="Administrador"
$env:BOOTSTRAP_ADMIN_EMAIL="admin@agendapro.com"
$env:BOOTSTRAP_ADMIN_PASSWORD="troque-por-uma-senha-forte"
.\mvnw.cmd spring-boot:run
```

O bootstrap só cria o usuário quando ainda não existe nenhum `ADMIN`. A senha é
validada e armazenada com BCrypt. Se o e-mail já pertencer a outro usuário, a
aplicação falha sem promover essa conta silenciosamente.

Depois da primeira inicialização bem-sucedida, pare a aplicação, desative
`BOOTSTRAP_ADMIN_ENABLED` e remova nome, e-mail e senha das variáveis de ambiente.

## Executando com Docker

Crie seu arquivo local de variáveis a partir do exemplo:

```powershell
Copy-Item .env.example .env
```

Substitua os valores de `DB_PASSWORD` e `JWT_SECRET` no `.env`. Esse arquivo está
no `.gitignore` e não deve ser enviado ao GitHub.

Suba a API e o PostgreSQL:

```powershell
docker compose up --build
```

O Compose ativa o perfil `prod`. Para uma implantação real, defina
`SWAGGER_ENABLED=false`; no ambiente Docker local ele permanece habilitado para
facilitar os testes manuais.

Para encerrar:

```powershell
docker compose down
```

O volume `postgres_data` mantém os dados entre reinicializações. Use
`docker compose down --volumes` somente quando quiser apagar o banco do ambiente
Docker.

## Documentação da API

Com a aplicação iniciada:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- URL base da API: `http://localhost:8080/api/v1`
- Health check: `http://localhost:8080/actuator/health`

Cadastre um usuário ou faça login, copie o token retornado e utilize o botão
**Authorize** do Swagger com o JWT.

## Endpoints principais

| Método | Endpoint | Finalidade |
|---|---|---|
| `POST` | `/api/v1/usuarios` | Cadastrar usuário |
| `POST` | `/api/v1/auth/login` | Autenticar e gerar JWT |
| `GET` | `/api/v1/auth/me` | Consultar a sessão autenticada |
| `GET` | `/api/v1/usuarios/{id}` | Consultar usuário |
| `POST` | `/api/v1/profissionais` | Criar perfil profissional |
| `PATCH` | `/api/v1/profissionais/{id}/barbearia` | Transferir profissional entre unidades |
| `GET` | `/api/v1/barbearias` | Listar unidades ativas disponíveis ao cliente |
| `POST` | `/api/v1/barbearias` | Profissional criar sua própria unidade |
| `GET` | `/api/v1/barbearias/minhas` | Listar unidades administradas pelo profissional |
| `PATCH` | `/api/v1/barbearias/{id}/proprietario` | Transferir propriedade a um membro da unidade |
| `POST` | `/api/v1/barbearias/{id}/horarios` | Adicionar período de funcionamento |
| `POST` | `/api/v1/barbearias/{id}/foto` | Enviar foto da unidade |
| `POST` | `/api/v1/servicos` | Cadastrar serviço |
| `POST` | `/api/v1/profissionais-servicos` | Associar profissional e serviço |
| `GET` | `/api/v1/profissionais-servicos/por-servico` | Listar profissionais associados ao serviço |
| `POST` | `/api/v1/horarios-atendimento` | Cadastrar horário semanal |
| `POST` | `/api/v1/excecoes-disponibilidade` | Criar bloqueio ou horário extra |
| `GET` | `/api/v1/disponibilidades` | Consultar horários disponíveis |
| `POST` | `/api/v1/agendamentos` | Criar agendamento |
| `GET` | `/api/v1/agendamentos` | Histórico paginado do profissional |
| `GET` | `/api/v1/agendamentos/cliente/{id}` | Histórico paginado do cliente |
| `PATCH` | `/api/v1/agendamentos/{id}/confirmar` | Confirmar agendamento |
| `PATCH` | `/api/v1/agendamentos/{id}/cancelar` | Cancelar agendamento |
| `PATCH` | `/api/v1/agendamentos/{id}/concluir` | Concluir atendimento |

A documentação OpenAPI contém todos os parâmetros e contratos disponíveis.

### Filtros e paginação

Exemplo da agenda de um profissional:

```text
GET /api/v1/agendamentos?profissionalId=1&dataInicio=2030-01-01&dataFim=2030-01-31&status=CONFIRMADO&page=0&size=20&sort=inicio,desc
```

O tamanho máximo é de 100 registros. A ordenação aceita `id`, `inicio`, `fim` e
`status`.

## Integração com o frontend

Por padrão, o navegador permite chamadas originadas de
`http://localhost:5173`, porta padrão do Vite. Para autorizar outras origens,
configure uma lista separada por vírgulas:

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://app.exemplo.com
```

São aceitos apenas os métodos e cabeçalhos necessários à API. Como o JWT é enviado
no cabeçalho `Authorization`, credenciais baseadas em cookies permanecem
desabilitadas no CORS.

## Banco de dados e migrations

O Hibernate está configurado com `ddl-auto=validate`: ele confere se as entidades
correspondem ao banco, mas não cria ou altera tabelas.

O schema é controlado exclusivamente por migrations Flyway em:

```text
src/main/resources/db/migration
```

Nunca altere uma migration que já foi executada em um ambiente compartilhado.
Crie uma nova versão para cada mudança de schema.

## Testes

Execute toda a suíte:

```powershell
.\mvnw.cmd test
```

O projeto possui testes unitários, testes HTTP, validação do contexto, testes com
PostgreSQL real e teste de concorrência. No fechamento desta versão, os 94 testes
passaram sem falhas.

Todos os testes de integração utilizam Testcontainers e o perfil `test`. Cada
contexto recebe um PostgreSQL descartável, sem acessar o banco local de
desenvolvimento. Os testes unitários permanecem isolados com Mockito e não iniciam
o banco.

O teste de concorrência comprova também que somente uma de duas reservas
simultâneas para o mesmo horário é aceita.

## Integração contínua

O workflow `.github/workflows/backend-ci.yml` executa `mvn verify` com Java 21 em
todo push e pull request para `main`. O Docker disponível no runner é usado pelos
testes de integração com Testcontainers; nenhuma credencial do banco de
desenvolvimento é necessária.

## Segurança

- Senhas nunca são armazenadas em texto puro.
- O login devolve uma mensagem genérica para credenciais incorretas.
- JWTs possuem emissor, expiração e identificador único.
- Endpoints verificam perfil e propriedade do recurso.
- Segredos são recebidos por variáveis de ambiente.
- A aplicação Docker executa com usuário sem privilégios administrativos.

## Próxima evolução

O backend está preparado para receber um frontend em JavaScript/React. O frontend
poderá consumir os contratos documentados no Swagger para oferecer uma interface
visual de barbearia, clínica ou consultório.

## Autor

Projeto desenvolvido por Marcelo como estudo prático de backend profissional com
Java e Spring Boot.
