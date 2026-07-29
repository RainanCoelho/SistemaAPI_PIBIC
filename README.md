# SistemaAPI PIBIC

Plataforma educacional para criação e resolução de casos clínicos, desenvolvida para apoiar o ensino e a aprendizagem na área da saúde.

O sistema permite que professores construam experiências de estudo baseadas em situações clínicas, reunindo informações do paciente, contexto médico, perguntas e alternativas. Os casos podem ser organizados por área, especialidade e nível de dificuldade.

## Principais funcionalidades

### Para professores

- Criar, editar e publicar casos clínicos.
- Cadastrar pacientes, conteúdos clínicos e perguntas.
- Gerar e ajustar conteúdos clínicos com apoio de IA.
- Gerar e persistir perguntas com IA, tanto para conteúdos clínicos manuais quanto para os gerados por IA.
- Revisar respostas discursivas e de conduta antes de incluí-las no cálculo do desempenho.
- Definir o nível de dificuldade e o tempo limite de cada caso.
- Acompanhar o desempenho dos alunos.

### Para alunos

- Acessar casos clínicos publicados.
- Resolver casos dentro do tempo definido.
- Receber o resultado das respostas.
- Consultar o histórico e acompanhar o próprio desempenho.

### Gestão da plataforma

- Gerenciamento de alunos, professores e usuários.
- Controle de acesso conforme o perfil de cada usuário.
- Proteção das respostas e dos dados utilizados nas avaliações.

## Geração de perguntas com IA

Professores podem gerar perguntas para um caso clínico em rascunho com `POST /casos/{id}/ia/perguntas/gerar`. As perguntas retornadas já ficam persistidas no caso, independentemente de o conteúdo clínico ter sido criado manualmente ou gerado por IA.

```json
{
  "quantidade": 5,
  "tipo": "MULTIPLA_ESCOLHA",
  "quantidadeAlternativas": 4,
  "instrucoesAdicionais": "Priorize raciocínio diagnóstico e conduta inicial."
}
```

Os campos são opcionais. Os valores padrão são 5 perguntas, tipo `MULTIPLA_ESCOLHA` e 4 alternativas; os limites aceitos são de 1 a 10 perguntas, de 2 a 5 alternativas e até 2.000 caracteres nas instruções adicionais. Em caso de sucesso, a API responde com `201 Created` e a lista de perguntas persistidas.

## Revisão humana das respostas

Respostas de múltipla escolha, verdadeiro ou falso e diagnóstico são corrigidas automaticamente. Respostas `DISCURSIVA` e `CONDUTA_CLINICA` permanecem com `correta: null` até a decisão do professor responsável ou de um administrador.

- `GET /casos/{id}/respostas/pendentes-revisao` lista as respostas pendentes de forma paginada.
- `PATCH /casos/{id}/respostas/{idResposta}/revisao`, com `{"correta": true}` ou `{"correta": false}`, registra a decisão humana.

Uma repetição com a mesma decisão é idempotente; tentar trocar uma decisão já registrada responde com `409 Conflict`. Somente respostas já avaliadas entram no denominador da nota e dos indicadores de desempenho.

## Perfis de execução

O perfil padrão é `prod`, que exige as credenciais e os segredos definidos no arquivo local `.env`, que não deve ser versionado. Para desenvolvimento local, ative explicitamente `SPRING_PROFILES_ACTIVE=dev`; isso evita que credenciais previsíveis e usuários de demonstração sejam usados por engano em uma implantação.

## Piloto de IA com FreeLLMAPI

Para o piloto privado, com aproximadamente 15 a 20 usuários, a API pode usar o [FreeLLMAPI](https://github.com/tashfeenahmed/freellmapi) `v0.6.3` como gateway local compatível com a API da OpenAI. O backend é o único cliente do gateway: o painel e a porta `3001` devem continuar restritos à máquina ou à rede privada.

O gateway não fornece créditos próprios. Ele reúne somente os provedores para os quais forem cadastradas chaves válidas e continua sujeito às cotas, aos termos e à disponibilidade de cada serviço gratuito. Essa configuração é destinada a testes e validação, sem SLA; antes do lançamento público, substitua-a por um serviço pago com contrato e garantias adequadas.

### Inicialização

1. Crie o arquivo local `.env`, configure as variáveis descritas em `docs/ia-piloto.md` e gere uma chave de criptografia exclusiva para `FREELLMAPI_CHAVE_CRIPTOGRAFIA`.
2. Inicie PostgreSQL e o gateway:

   ```bash
   docker compose --profile ia up -d
   ```

3. Abra `http://localhost:3001`, cadastre no painel as chaves dos provedores gratuitos que serão usados, ordene a cadeia de fallback e copie a chave unificada `freellmapi-...`.
4. Defina a chave unificada em `IA_CHAVE_API` no ambiente do processo Spring. Use `IA_URL_BASE=http://127.0.0.1:3001/v1` e `IA_MODELO=auto`.
5. Execute a aplicação com `SPRING_PROFILES_ACTIVE=prod`.

O arquivo `.env` é lido pelo Docker Compose, mas não é importado automaticamente por uma execução direta do Maven. Ao iniciar o backend pela IDE, pelo Maven ou por um serviço do sistema, configure as mesmas variáveis no ambiente desse processo.

As opções e o procedimento operacional completo estão em [docs/ia-piloto.md](docs/ia-piloto.md).

## Proteções do piloto

- Cada usuário autenticado pode fazer, por padrão, até 5 chamadas de IA por minuto e 20 por dia; há no máximo 3 chamadas simultâneas no processo.
- Um limite excedido responde com `429 Too Many Requests` e o cabeçalho `Retry-After`, em segundos.
- Quando os provedores gratuitos esgotam temporariamente a capacidade, a API responde com `503 Service Unavailable` e `Retry-After`.
- Uma chamada de IA que exceda 40 segundos responde com `504 Gateway Timeout`.
- Corpos de requisição maiores que 1 MiB respondem com `413 Content Too Large`.
- Os limites podem ser ajustados pelas variáveis `IA_LIMITE_POR_MINUTO`, `IA_LIMITE_POR_DIA`, `IA_MAXIMO_SIMULTANEAS`, `IA_TEMPO_LIMITE` e `HTTP_LIMITE_CORPO_BYTES`.

Os limites por usuário são mantidos em memória e atendem ao piloto em uma única instância. Eles reiniciam com a aplicação e não são compartilhados entre réplicas; uma implantação pública deve usar controle distribuído.

## Dados clínicos e revisão humana

Envie à IA somente casos inteiramente sintéticos ou previamente desidentificados. Não envie nomes, CPF, RG, CNS, número de prontuário, telefone, e-mail, endereço, datas exatas, instituição, profissional identificável, imagens ou qualquer combinação que permita reconhecer uma pessoa real. O filtro da aplicação reduz alguns identificadores óbvios, mas não garante anonimização.

Toda saída deve ser revisada por um professor antes da publicação. O recurso é educacional: não substitui avaliação clínica, diagnóstico, prescrição ou decisão assistencial. Consulte a política completa em [docs/politica-dados-clinicos.md](docs/politica-dados-clinicos.md).

## Teste das migrações no PostgreSQL real

O teste de integração usa Testcontainers e fica desabilitado sem autorização explícita. Com o Docker em execução, rode dentro de `Sistema_Crud_API_PIBIC`:

```powershell
$env:RUN_POSTGRES_TESTS = "true"
.\mvnw.cmd test
```

Em Bash:

```bash
RUN_POSTGRES_TESTS=true ./mvnw test
```

Esse teste aplica as migrações Flyway em PostgreSQL 16 e valida o esquema real. A automação de integração contínua já executa a suíte com `RUN_POSTGRES_TESTS=true`.

## Tecnologias

Java, Spring Boot, Spring Security, Spring Data JPA, Spring AI, PostgreSQL, Flyway e Maven.

## Sobre o projeto

O SistemaAPI PIBIC busca aproximar o aprendizado teórico da tomada de decisão clínica, oferecendo uma estrutura organizada para criação, aplicação e acompanhamento de atividades educacionais.
