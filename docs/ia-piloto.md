# Gateway de IA no piloto

## Objetivo e limites desta configuração

Esta configuração foi preparada para a validação privada da plataforma com aproximadamente 15 a 20 usuários. A aplicação continua no perfil `prod`, com autenticação, HTTPS no ambiente implantado e segredos obrigatórios. O fato de o ambiente ser usado para testes não autoriza enfraquecer essas proteções.

O [FreeLLMAPI](https://github.com/tashfeenahmed/freellmapi) `v0.6.3` funciona como gateway local compatível com a API da OpenAI. O Spring AI chama apenas o gateway, e o gateway escolhe uma rota entre os provedores cujas chaves foram cadastradas.

```text
Usuário autenticado
        |
        v
SistemaAPI PIBIC (perfil prod)
        |
        v
FreeLLMAPI em 127.0.0.1:3001
        |
        v
Provedores gratuitos configurados no painel
```

O FreeLLMAPI não cria cotas gratuitas e não torna pago um modelo gratuito. Cada chave precisa ser obtida diretamente no provedor correspondente. Cotas, catálogo, modelos e termos podem mudar sem aviso. O gateway é adequado para experimentação privada, não oferece SLA e não deve ficar exposto publicamente.

## Preparação dos segredos

1. Crie o arquivo local `.env`, que deve permanecer fora do controle de versão.
2. Configure senhas e segredos exclusivos para o ambiente.
3. Gere 32 bytes aleatórios em formato hexadecimal para `FREELLMAPI_CHAVE_CRIPTOGRAFIA`.

Exemplo no PowerShell:

```powershell
[Convert]::ToHexString(
    [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
).ToLower()
```

Exemplo com OpenSSL:

```bash
openssl rand -hex 32
```

Essa chave protege, em repouso, as credenciais dos provedores armazenadas no volume do gateway. Guarde-a em um gerenciador de segredos. Alterá-la ou perdê-la pode impedir a leitura das chaves já cadastradas.

Não versionar:

- o arquivo `.env`;
- a chave unificada do gateway;
- `FREELLMAPI_CHAVE_CRIPTOGRAFIA`;
- chaves de Groq, Google, OpenRouter ou qualquer outro provedor;
- cópias do volume `freellmapi_data` sem proteção apropriada.

## Inicialização pelo Docker Compose

Na raiz do repositório, execute:

```bash
docker compose --profile ia up -d
docker compose ps
```

O perfil `ia` inicia:

- PostgreSQL 16 em `127.0.0.1:5432`;
- FreeLLMAPI `v0.6.3` em `127.0.0.1:3001`;
- o volume persistente `freellmapi_data`.

O painel e a API do gateway compartilham a porta `3001`. Abra `http://localhost:3001` somente em uma máquina confiável. O teste de saúde pode ser feito por:

```http
GET http://127.0.0.1:3001/api/ping
```

Para encerrar sem apagar os volumes:

```bash
docker compose --profile ia down
```

Não use `down -v` a menos que a intenção seja apagar também o banco local e as chaves cadastradas no gateway.

## Cadastro dos provedores

No painel do FreeLLMAPI:

1. abra a página de chaves;
2. adicione apenas chaves obtidas legitimamente nas contas dos provedores;
3. valide o estado de cada chave;
4. ordene a cadeia de fallback conforme qualidade, velocidade e cota;
5. copie a chave unificada exibida pelo gateway, com prefixo `freellmapi-...`;
6. atribua essa chave a `IA_CHAVE_API` no processo do backend.

Com `IA_MODELO=auto`, o gateway escolhe entre as rotas disponíveis. Para fixar um modelo durante um teste comparativo, use em `IA_MODELO` um identificador exibido pelo próprio painel. Não presuma que todas as opções do catálogo estão disponíveis: uma rota só funciona quando possui uma chave válida, cota restante e suporte ao formato solicitado.

Antes de cadastrar um provedor, revise os termos atuais da respectiva conta. Não crie contas duplicadas para contornar limites, não revenda a cota e não compartilhe credenciais.

## Variáveis de configuração

| Variável | Padrão do piloto | Finalidade |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Mantém as proteções do perfil implantado. |
| `IA_CHAVE_API` | sem valor válido | Chave unificada gerada pelo painel do gateway. |
| `IA_URL_BASE` | `http://127.0.0.1:3001/v1` | Base compatível com OpenAI usada pelo Spring AI. |
| `IA_MODELO` | `auto` | Permite que o gateway selecione a rota. |
| `IA_TEMPERATURA` | `0.2` | Reduz variação e favorece consistência estrutural. |
| `IA_MAXIMO_TOKENS_SAIDA` | `4000` | Limita o tamanho máximo solicitado para a saída da IA. |
| `IA_TEMPO_LIMITE` | `60s` | Tempo máximo da chamada antes de responder `504`. |
| `IA_IDEMPOTENCIA_TTL` | `1h` | Retenção dos metadados de uma tentativa para replay seguro. |
| `IA_MAXIMO_TENTATIVAS_HTTP` | `0` | Evita multiplicar tentativas; o gateway já trata fallback entre provedores. |
| `IA_LIMITE_POR_MINUTO` | `5` | Cota de operações de geração por usuário autenticado a cada minuto. Chamadas internas de reparo e validação da mesma operação idempotente não consomem unidades extras. |
| `IA_LIMITE_POR_DIA` | `20` | Cota de operações de geração por usuário autenticado no dia, no fuso de São Paulo. |
| `IA_MAXIMO_SIMULTANEAS` | `3` | Máximo global de chamadas concorrentes no processo. |
| `HTTP_LIMITE_CORPO_BYTES` | `1048576` | Limite global de 1 MiB para corpos `POST`, `PUT` e `PATCH`. |
| `FREELLMAPI_CHAVE_CRIPTOGRAFIA` | sem valor válido | Chave de criptografia usada pelo contêiner do gateway. |

As variáveis `spring.ai.openai.*` permanecem com o nome exigido pelo Spring AI, mas são preenchidas pelas variáveis `IA_*`.

O arquivo `.env` é uma fonte de interpolação do Docker Compose. Ele não é carregado automaticamente quando o backend é iniciado diretamente pela IDE ou pelo Maven. Nesse caso, configure `DB_*`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `IA_*` e as demais variáveis do perfil `prod` no ambiente do processo.

Se o backend for colocado futuramente no mesmo Compose, `127.0.0.1` passará a apontar para o próprio contêiner. Nesse cenário, ajuste `IA_URL_BASE` para `http://freellmapi:3001/v1` dentro da rede privada do Compose.

## Proteções e respostas da API

| Situação | Resposta | Ação recomendada |
| --- | --- | --- |
| Corpo acima de `HTTP_LIMITE_CORPO_BYTES` | `413 Content Too Large` | Reduza o corpo; não fragmente dados identificáveis em várias requisições. |
| Cota por minuto, por dia ou concorrência atingida | `429 Too Many Requests` | Aguarde o número de segundos do cabeçalho `Retry-After`. |
| Resposta inválida ou falha inesperada do provedor/gateway | `502 Bad Gateway` | Confira saúde, credenciais, compatibilidade e registros operacionais sem conteúdo clínico. |
| Chave/base de IA ausente ou inválida na configuração | `503 Service Unavailable` | Corrija as variáveis do backend. |
| Capacidade gratuita temporariamente esgotada nos provedores | `503 Service Unavailable` | Aguarde o cabeçalho `Retry-After` antes de tentar novamente. |
| Chamada acima de `IA_TEMPO_LIMITE` | `504 Gateway Timeout` | Tente depois; avalie provedor, tamanho do contexto e latência. |
| Âncoras ausentes ou incoerência clínica confirmada | `422 Unprocessable Entity` | Corrija os campos indicados em `campos`; a geração principal não é iniciada e nenhum conteúdo clínico é salvo. |
| Mesma `Idempotency-Key` ainda em processamento | `409 Conflict` + `Retry-After` | Aguarde e repita com a mesma chave; não crie outra tentativa concorrente. |

Os contadores por minuto e por dia e o ledger de idempotência ficam no PostgreSQL. O ledger retém apenas hashes, estado e IDs de resultado; a V19 remove a antiga coluna que duplicava respostas clínicas. A chave expira após `IA_IDEMPOTENCIA_TTL` (1 hora por padrão) e sua reutilização é protegida por bloqueio transacional. O limite de simultaneidade é local ao processo; antes de escalar horizontalmente, coordene esse limite entre réplicas e mantenha métricas e alertas.

O limite de simultaneidade é global para o processo. Quando todas as vagas estão ocupadas, a API responde imediatamente com `429` e `Retry-After: 1`.

## Dados clínicos

Somente casos sintéticos ou previamente desidentificados podem chegar ao gateway. A proteção automática remove alguns padrões óbvios, mas não garante anonimização e não substitui a conferência do professor.

Antes de cada geração:

1. confirme que o caso não descreve uma pessoa real reconhecível;
2. remova identificadores diretos e combinações raras;
3. envie apenas os campos necessários ao objetivo educacional;
4. revise as condições de uso e retenção do provedor que poderá receber a rota;
5. depois da geração, revise correção clínica, coerência, vieses e ausência de dados pessoais.

Antes de gerar ou ajustar um caso, o professor deve informar especialidade, diagnóstico esperado e objetivo de aprendizagem. A API pré-valida semanticamente essas âncoras junto aos demais campos preenchidos e ao perfil do paciente. Uma primeira avaliação `INCOERENTE` é confirmada uma única vez; somente uma incoerência confirmada bloqueia a chamada principal. Na criação, a IA recebe os campos do professor como imutáveis e completa apenas as lacunas. Tanto a criação quanto o ajuste validam novamente a saída antes de persistir. O erro `422` devolve mensagens por campo para que o cliente leve o professor diretamente ao dado que precisa ser corrigido.

Veja [politica-dados-clinicos.md](politica-dados-clinicos.md).

## Verificações do piloto

Com o Docker em execução, valide primeiro o gateway:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:3001/api/ping"
```

Depois, autentique um professor e use os exemplos em [requests.http](requests.http) ou importe [SistemaAPI_PIBIC.postman_collection.json](SistemaAPI_PIBIC.postman_collection.json).

Para testar as migrações em PostgreSQL 16 real com Testcontainers, execute dentro de `Sistema_Crud_API_PIBIC`:

```powershell
$env:RUN_POSTGRES_TESTS = "true"
.\mvnw.cmd test
```

Em Bash:

```bash
RUN_POSTGRES_TESTS=true ./mvnw test
```

O Docker precisa estar disponível. Sem `RUN_POSTGRES_TESTS=true`, esse teste específico permanece desabilitado; a automação de integração contínua já o habilita.

## Transição após a validação

A integração do backend usa o contrato compatível com OpenAI, portanto a troca futura pode ser feita principalmente por configuração:

1. escolha um provedor ou gateway pago com SLA, política de dados e contrato adequados;
2. defina nova `IA_URL_BASE`, `IA_CHAVE_API` e `IA_MODELO`;
3. execute testes de contrato e qualidade dos casos/perguntas;
4. substitua os limites em memória por controle distribuído;
5. mantenha alertas sobre a telemetria já registrada, sem registrar prompts, respostas ou segredos;
6. conclua a avaliação jurídica e de segurança antes de aceitar dados reais.
