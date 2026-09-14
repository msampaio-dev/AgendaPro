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

## Funcionalidades

- Cadastro e atualização de usuários
- Autenticação com senha BCrypt e emissão de JWT
- Perfis `CLIENTE`, `PROFISSIONAL` e `ADMIN`
- Cadastro de profissionais e serviços
- Associação entre profissionais e serviços
- Horários semanais de atendimento
- Bloqueios e disponibilidades extras
- Cálculo de horários disponíveis
- Criação, confirmação, cancelamento e conclusão de agendamentos
- Histórico por profissional e por cliente
- Filtros por período e status
- Paginação e ordenação
- Erros HTTP padronizados
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
- Horários inexistentes ou ambíguos por mudança de fuso são rejeitados.

## Arquitetura

O código é organizado por funcionalidade:

```text
com.agendapro
├── auth
├── usuario
├── profissional
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

Cadastre um usuário ou faça login, copie o token retornado e utilize o botão
**Authorize** do Swagger com o JWT.

## Endpoints principais

| Método | Endpoint | Finalidade |
|---|---|---|
| `POST` | `/usuarios` | Cadastrar usuário |
| `POST` | `/auth/login` | Autenticar e gerar JWT |
| `GET` | `/usuarios/{id}` | Consultar usuário |
| `POST` | `/profissionais` | Criar perfil profissional |
| `POST` | `/servicos` | Cadastrar serviço |
| `POST` | `/profissionais-servicos` | Associar profissional e serviço |
| `POST` | `/horarios-atendimento` | Cadastrar horário semanal |
| `POST` | `/excecoes-disponibilidade` | Criar bloqueio ou horário extra |
| `GET` | `/disponibilidades` | Consultar horários disponíveis |
| `POST` | `/agendamentos` | Criar agendamento |
| `GET` | `/agendamentos` | Histórico paginado do profissional |
| `GET` | `/agendamentos/cliente/{id}` | Histórico paginado do cliente |
| `PATCH` | `/agendamentos/{id}/confirmar` | Confirmar agendamento |
| `PATCH` | `/agendamentos/{id}/cancelar` | Cancelar agendamento |
| `PATCH` | `/agendamentos/{id}/concluir` | Concluir atendimento |

A documentação OpenAPI contém todos os parâmetros e contratos disponíveis.

### Filtros e paginação

Exemplo da agenda de um profissional:

```text
GET /agendamentos?profissionalId=1&dataInicio=2030-01-01&dataFim=2030-01-31&status=CONFIRMADO&page=0&size=20&sort=inicio,desc
```

O tamanho máximo é de 100 registros. A ordenação aceita `id`, `inicio`, `fim` e
`status`.

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
PostgreSQL real e teste de concorrência. No fechamento desta versão, os 87 testes
passaram sem falhas.

Todos os testes de integração utilizam Testcontainers e o perfil `test`. Cada
contexto recebe um PostgreSQL descartável, sem acessar o banco local de
desenvolvimento. Os testes unitários permanecem isolados com Mockito e não iniciam
o banco.

O teste de concorrência comprova também que somente uma de duas reservas
simultâneas para o mesmo horário é aceita.

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
