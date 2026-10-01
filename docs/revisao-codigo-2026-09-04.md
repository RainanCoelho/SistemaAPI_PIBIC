# Revisão de código — API e front-end

Data: 04/09/2026. Revisão dos dois repositórios locais, abrangendo configuração, autenticação, autorização, CRUD, tentativas e respostas, geração com IA, persistência, migrações, interface, dependências e verificações automatizadas. Código de produção e dados não foram alterados.

**Resultado:** existem problemas relevantes mesmo com as suítes passando. Priorizar configurações seguras, perda de edições, recuperação de respostas interrompidas e timeouts. Não foi identificado bypass de autorização por proprietário nos fluxos examinados; isso não equivale a uma garantia de ausência de vulnerabilidades.

P1 = corrigir prioritariamente, sobretudo antes de disponibilizar externamente. P2 = corrigir no próximo ciclo. “Reproduzido” indica execução isolada; “confirmado por código” indica fluxo rastreado; cenários dependentes de infraestrutura ou concorrência estão explicitamente separados.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| API: `mvnw.cmd -B --no-transfer-progress verify` | BUILD SUCCESS |
| Testes da API | 154 contabilizados: 152 passaram, 2 ignorados |
| JaCoCo | 75,97% das linhas; 59,39% dos branches |
| Checkstyle / SpotBugs | Zero violações / zero achados |
| Front: `npm run check` | Lint, tipos, testes e build passaram |
| Testes do front | 65 passaram |
| `npm audit --json` | Zero vulnerabilidades reportadas pelo registro na consulta |
| Provas isoladas com funções reais do front | Timeout do corpo, JSON interrompido e fallback de UUID confirmados |

Os dois testes ignorados exigem PostgreSQL via Testcontainers; não foram executados nesta máquina, onde Docker não estava disponível no PATH. O restante da integração usou H2. Não houve carga contra produção, chamadas pagas à IA, medição real da latência dos provedores, auditoria de imagens Docker ou varredura completa de CVEs transitivas do Maven. O ambiente implantado, o proxy e as variáveis efetivas não foram inspecionados. Os achados de configuração abaixo se referem aos padrões versionados.

## Achados prioritários

### 1. P1 — Aplicação inicia em desenvolvimento por padrão

**Confirmado por código.** [application.properties:3](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/resources/application.properties:3) define `spring.profiles.default=dev`, contrariando o README. O [DataInitializer:22](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/config/DataInitializer.java:22) cria usuários de demonstração nesse perfil, e [application-dev.properties](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/resources/application-dev.properties) contém credenciais e segredo JWT previsíveis.

**Gatilho/impacto:** iniciar sem perfil explícito, com banco acessível, pode criar contas conhecidas e habilitar a configuração de desenvolvimento em uma implantação. As exigências HTTPS e o validador de produção deixam de ser aplicados.

**Correção:** restaurar padrão seguro ou exigir perfil explícito; manter demonstrações exclusivas de desenvolvimento. Criar teste de inicialização que verifique os padrões efetivos e falhe se produção aceitar configurações de demonstração.

### 2. P1 — Limites de IA estão desativados inclusive no padrão de produção

**Confirmado por código.** [application.properties:26](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/resources/application.properties:26) define `IA_LIMITES_HABILITADOS:false`. Em [ControleUsoIa:140](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/ControleUsoIa.java:140), esse valor pula tanto as cotas quanto a aquisição de vagas simultâneas. O perfil prod não sobrescreve essa opção.

**Impacto:** um usuário autorizado pode abrir várias gerações e esgotar capacidade/cota do gateway; o aumento de concorrência também degrada a velocidade dos demais. Idempotência protege a repetição da mesma chave, mas não limita solicitações com chaves novas.

**Correção:** habilitar por padrão e impedir o desligamento acidental em produção. O README também precisa ser atualizado: quando habilitado, o código atual coordena vagas pelo banco em `ControleUsoIaStore`, não apenas por processo como descrito.

### 3. P1 — Gerar perguntas apaga edições manuais não salvas

**Confirmado por código.** Em [CreateCaseQuestions.jsx:215](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/pages/CreateCaseQuestions.jsx:215), edições ficam no estado local com `dirty: true`. O botão de gerar continua disponível; após o POST, [linhas 308–313](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/pages/CreateCaseQuestions.jsx:308) recarregam a lista do servidor e substituem todo o estado.

**Reprodução funcional:** editar uma pergunta existente ou adicionar uma manual; sem salvar, gerar perguntas pela IA. Se a recarga funcionar, o servidor devolve apenas o que estava persistido e as novas perguntas: as mudanças locais desaparecem. Se a recarga falhar, o caminho alternativo mescla a lista, tornando o comportamento dependente da rede.

**Correção:** persistir/validar mudanças antes da geração ou mesclar os itens gerados preservando perguntas locais e campos alterados. Incluir teste de interface para pergunta nova e pergunta editada.

### 4. P1 — Resposta interrompida pode liberar nova geração com outra chave

**Reproduzido com funções reais.** [api.js:149](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/api.js:149) e a leitura JSON produzem `INVALID_RESPONSE`/`INVALID_JSON`. [shouldKeepAiRequestIdentity:35](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/aiRequestIdentity.js:35) não classifica esses erros como resultado incerto. As telas então removem a chave da tentativa.

**Evidência:** resposta HTTP 200 com corpo JSON truncado resultou em `code=INVALID_JSON`, `status=200`, `keepIdentity=false`.

**Impacto:** a API pode já ter persistido o resultado. O clique seguinte utiliza outra chave, permitindo uma geração adicional e, para perguntas, outro lote. Uma falha 502/504 produzida por proxy também merece tratamento distinto de uma falha definitiva registrada pela API.

**Correção:** preservar a chave quando não é possível determinar se a operação concluiu; consultar/recuperar a tentativa antes de criar outra. Cobrir perda de conexão durante o corpo, JSON truncado e falhas do proxy.

### 5. P2 — Ajustes clínicos expiram cedo; orçamento da geração também está subestimado

**Confirmado por código.** [pibicApi.js:94](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/pibicApi.js:94) dá 75 s para ajustes, apoiado no comentário de que fazem uma chamada. Contudo, [ServicoCasoClinicoIa:239](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/ServicoCasoClinicoIa.java:239) executa pré-validação, geração do ajuste e pós-validação, além de possíveis confirmação e recuperação.

Três chamadas de 30 s já excedem o limite do navegador, embora cada chamada respeite o timeout de 60 s do provedor. A geração inicial tem 330 s no front, calculados para cinco chamadas, mas pode fazer seis: pré-validação + confirmação + geração + reparo + validação + recuperação do status. Não existe um deadline global explícito da operação no serviço.

**Correção:** definir um orçamento total compartilhado entre API, gateway/proxy e front. Para operações longas, considerar job persistido com status/progresso e recuperação por identificador. Aumentar o timeout isoladamente não torna a geração mais rápida.

### 6. P2 — Timeout e cancelamento deixam de valer durante a leitura da resposta

**Reproduzido com `apiRequest`.** [api.js:131](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/api.js:131) limpa o timer e remove o listener de cancelamento assim que `fetch` recebe os cabeçalhos, antes de `response.text()`.

**Evidência:** configurando timeout de 20 ms e corpo com atraso de 80 ms, a função retornou sucesso após aproximadamente 102 ms; `signal.aborted` permaneceu falso.

**Correção:** manter o timer/listener até terminar a leitura do corpo e classificar interrupções nessa fase com a mesma semântica de timeout/cancelamento. Testar cabeçalhos rápidos com corpo lento.

### 7. P2 — Repetir geração na etapa de mídias pode sobrescrever um ajuste já concluído

**Confirmado pelo encadeamento de código; não executado contra banco real.** [CreateCaseReferencesMedia.jsx:102](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/pages/CreateCaseReferencesMedia.jsx:102) chama `persistClinicalDraft` antes de reenviar uma tentativa de ajuste. [caseDraftPersistence.js:74](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/caseDraftPersistence.js:74) faz PUT dos campos locais. O replay da API em [CasoClinicoIAController:148](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/controller/CasoClinicoIAController.java:148) reconstrói o resultado lendo o conteúdo atual, que é mutável.

**Cenário:** caso com conteúdo completo; ajuste conclui no servidor, mas a resposta se perde. Ao repetir na etapa de mídias, o rascunho antigo é salvo por cima do ajuste. A chave anterior é reutilizada e a API responde sucesso lendo o conteúdo que acabou de ser sobrescrito.

**Correção:** resolver a tentativa pendente antes de executar novos PUTs; adotar controle de versão para edições e definir claramente a semântica de replay quando o recurso sofreu alterações. Evitar duplicar conteúdo clínico no ledger apenas para contornar esse problema.

### 8. P2 — Validação final ignora o paciente recém-complementado pela IA

**Confirmado por código.** [montarPromptValidacaoCoerencia:761](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/ServicoCasoClinicoIa.java:761) inclui somente `pacientesAtuais`. Os campos de `gerado.getPaciente()` são aplicados posteriormente em [atualizarPacienteComIa:1133](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/ServicoCasoClinicoIa.java:1133).

**Impacto:** com complemento cadastral habilitado, idade/sexo e demais atributos gerados podem divergir do texto sem que o revisor final receba esses atributos. Valores ausentes ou inválidos também podem ser simplesmente ignorados na persistência.

**Correção:** construir o paciente candidato resultante da mesclagem, validar campos e incluir exatamente esse candidato na pós-validação, antes de persistir. Testar uma resposta fictícia com idade cadastral diferente da descrita no texto.

### 9. P2 — Primeira abertura simultânea do caso pelo aluno pode falhar

**Confirmado como condição de corrida por código; falta reprodução em PostgreSQL.** [TentativaCasoService:32](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/TentativaCasoService.java:32) faz busca sem lock e cria se ausente. Duas aberturas simultâneas podem observar ausência e tentar inserir a mesma combinação aluno/caso. A restrição única impede duplicidade, mas a requisição perdedora recebe conflito genérico.

**Correção:** tornar o início atomicamente idempotente, com lock coerente ou inserção e recuperação de conflito em transação apropriada. Acrescentar teste concorrente em PostgreSQL. Não basta capturar a violação e reler dentro de uma transação já abortada.

### 10. P2 — Ordem inversa de locks permite deadlock entre exclusão e edição

**Risco concreto por ordem de aquisição; não reproduzido em PostgreSQL.** [CasoClinicoService:183](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/CasoClinicoService.java:183) bloqueia o caso e exclui filhos por cascata. [PacienteService:52](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/PacienteService.java:52) e [ConteudoClinicoService:52](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/ConteudoClinicoService.java:52) bloqueiam primeiro o filho e depois o caso.

**Cenário:** exclusão detém o caso e espera o filho; edição detém o filho e espera o caso. O banco aborta uma transação. Os serviços transacionais de IA também precisam entrar na mesma revisão de ordem de locks.

**Correção:** padronizar a ordem entre todos os fluxos e testar exclusão concorrente com edição/geração. Tratar deadlock como conflito transitório quando apropriado, sem repetir cegamente chamadas ao provedor.

### 11. P2 — Fallback da chave de idempotência é incompatível com a API

**Reproduzido.** [aiRequestIdentity.js:62](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/aiRequestIdentity.js:62) retorna `web-...` quando `crypto.randomUUID` está indisponível. A [API exige UUID](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/IdempotenciaGeracaoIaService.java:127).

**Impacto:** nesses ambientes, toda geração com o fallback é rejeitada como chave inválida. O teste isolado desabilitou `crypto` e confirmou formato não UUID.

**Correção:** usar fallback que produza UUID válido com fonte aleatória adequada, ou indicar explicitamente incompatibilidade do ambiente. Adicionar teste do contrato front/API para esse caminho.

## Segurança e manutenção adicionais

- **Armazenamento local:** [CaseDraftContext.jsx:63](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/context/CaseDraftContext.jsx:63) aplica validade de 30 dias ao rascunho, mas lê `savedCase` sem expiração. Essa segunda chave contém uma cópia do caso completo. O prazo de retenção não vale para todas as cópias, e a expiração do rascunho só remove o dado quando ele é lido. Definir retenção uniforme e opção de apagar dados deste dispositivo. O produto declara casos fictícios, mas texto livre e nomes de arquivos ainda podem carregar dados identificáveis.
- **Token acessível a JavaScript:** [api.js:38](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/services/api.js:38) usa localStorage ou sessionStorage. Isso aumenta o impacto de eventual XSS; não foi demonstrado XSS nesta revisão. SessionStorage reduz persistência, mas não impede leitura por scripts. Uma migração para cookies HttpOnly requer revisão conjunta de CSRF/CORS e autenticação, não uma troca isolada no front.
- **Proxy confiável:** `server.forward-headers-strategy=framework` em produção exige que o acesso direto ao backend seja restrito e o proxy normalize os cabeçalhos recebidos. Sem isso, protocolo/IP aparentes podem ser manipulados, afetando HTTPS e limitação de login por IP. É uma condição de implantação a verificar, conforme a [documentação do Spring Security](https://docs.spring.io/spring-security/reference/features/exploits/http.html).
- **Ledger sem limpeza:** a expiração de `SolicitacaoGeracaoIa` permite reusar a mesma chave, mas não elimina linhas de chaves únicas antigas. Como o front cria UUIDs novos, haverá crescimento contínuo. Definir retenção operacional e limpeza em lotes após a janela de recuperação, separada da auditoria necessária.
- **TTL sem validação:** [IdempotenciaGeracaoIaStore:25](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/IdempotenciaGeracaoIaStore.java:25) aceita TTL nulo/inadequado sem regra explícita de duração. Um TTL menor que uma operação permite reinicializar uma tentativa ainda ativa. Validar duração e impedir reaproveitamento enquanto houver execução válida.
- **Auditoria imprecisa:** rejeições de coerência usam sempre `GERAR_CASO`, inclusive quando surgem no ajuste ([ServicoCasoClinicoIa:444](C:/Users/emano/OneDrive/Desktop/Projeto/SistemaAPI_PIBIC/Sistema_Crud_API_PIBIC/src/main/java/com/SistemaApiCrud/SistemaCrud/service/ServicoCasoClinicoIa.java:444)). Erros de rede/formato não têm a mesma cobertura de métricas dos sucessos. Isso dificulta descobrir qual fase falha ou demora.
- **CI do front:** há comando completo de verificação, mas não foi encontrado workflow `.github/workflows` nesse repositório. Automatizar `npm ci` e `npm run check` nos PRs; manter auditoria periódica de dependências. A API já tem workflow e revisão de dependências nos PRs.

## Como aumentar a velocidade da geração

A principal oportunidade está nas chamadas ao modelo, não em pequenos ajustes de Java. O caminho clínico normal faz três chamadas sequenciais; a geração de perguntas faz uma. Não há benchmark real nesta revisão para prometer ganho percentual.

1. **Medir cada fase:** duração e tokens de pré-validação, confirmação, geração, reparo, pós-validação e persistência, incluindo falhas. Registrar p50/p95 e número de chamadas por operação, sem guardar prompt/saída clínica em logs. Hoje a auditoria soma durações e pode esconder a etapa lenta.
2. **Separar o contrato de coerência:** hoje a validação utiliza o DTO do caso completo e pede campos clínicos vazios. Criar DTO pequeno com status e violações, mantendo contratos distintos para geração e validação. Isso reduz instruções conflitantes e saída desnecessária. As opções de timeout e de modelo existem no [Spring AI 2](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html); devem ser configuradas e medidas por finalidade.
3. **Evitar pré-validação repetida do mesmo contexto:** considerar cache curto do resultado por hash dos dados efetivamente enviados, versão do prompt e modelo. Invalidar ao mudar paciente, âncoras ou conteúdo. Preservar a validação final da saída nova.
4. **Reduzir saída desnecessária:** pedir somente lacunas e limitar extensão pedagógica por campo. Calibrar orçamento de tokens por tipo/quantidade de perguntas; não reduzir cegamente o teto atual de 4.000, pois truncamento pode aumentar falhas e reparos.
5. **Tornar recuperação específica:** o reparo de geração recebe o contexto inicial, mas não o restante da saída parcial válida. Incluir o candidato preservado, devidamente protegido, ajuda a completar lacunas em coerência com o texto existente e pode evitar uma rejeição posterior.
6. **Rever política de confirmação com métricas:** confirmações/reparos adicionam rodadas inteiras. Identificar quais são frequentes e corrigir schema/prompt/modelo antes de remover verificações de qualidade. Essas etapas têm dependências; paralelizá-las indiscriminadamente muda o comportamento.
7. **Eliminar requisições redundantes no front:** a geração de perguntas sempre faz GET após receber a lista do POST. Mesclar a resposta já retornada, preservando edições, pode remover uma ida à API. A persistência do rascunho também reenvia caso e paciente mesmo inalterados; comparar revisões/campos evita PUTs e locks desnecessários.
8. **Salvar lote manual em transação:** [CreateCaseQuestions.jsx:266](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/pages/CreateCaseQuestions.jsx:266) salva perguntas uma a uma. Um endpoint de lote reduz latência e permite atomicidade. Concorrência limitada no cliente só deve ser adotada após conferir os locks compartilhados do caso.
9. **Separar velocidade de percepção:** job assíncrono com status/progresso e reconexão melhora recuperação e experiência. Isso não reduz, sozinho, o tempo de inferência. O cancelamento atual interrompe a espera no navegador, sem cancelar de forma confiável o processamento no servidor.
10. **Reduzir trabalho na interface:** aplicar debounce à serialização/gravação do rascunho em [CaseDraftContext.jsx:81](C:/Users/emano/OneDrive/Desktop/Projeto/Front-End-Casos-Clinicos/src/context/CaseDraftContext.jsx:81), tratando falha de armazenamento visivelmente. O resultado de `writeProfessorStorage` hoje é ignorado; storage cheio pode deixar o usuário acreditar que o rascunho sobreviverá à recarga.

## O que pode ser descartado ou simplificado

| Item | Orientação |
|---|---|
| `target/`, `dist/`, caches, relatórios e logs locais de build | Regeneráveis; podem ser limpos quando não estiverem em uso. Já não devem integrar o código versionado. Nenhum foi apagado nesta revisão. |
| `node_modules/` | Regenerável com `npm ci`; remover apenas para liberar espaço ou reinstalação. Não produz ganho de execução da aplicação. |
| `.idea/` e artefatos individuais da IDE | Manter fora do Git salvo configuração compartilhada intencional. |
| Metadados vazios do `pom.xml` | Blocos vazios de nome, licenças, desenvolvedores e SCM podem ser preenchidos ou simplificados. Ganho é de clareza, não desempenho. |
| Clientes Spring AI de caso/pergunta | Há duplicação de tratamento de erros e métricas. Extrair apenas a infraestrutura comum, preservando DTOs/contratos separados. |
| Caminhos de objetivo gerado pela IA | A entrada agora exige objetivo preenchido; trechos de complemento do objetivo merecem revisão de alcançabilidade antes de remover. |
| Telas simuladas de senha e recursos indisponíveis | Simplificar ou ocultar ações sem implementação real, mantendo informação clara. Não remover rotas referenciadas sem ajustar navegação. |
| Referências/mídias | Atualmente o front guarda metadados, descarta o objeto File e não envia conteúdo à IA. Simplificar a etapa enquanto upload não existir. A mensagem de geração que diz analisar “referências” deve ser corrigida. |
| Documentação antiga | Atualizar perfil padrão, limites, coordenação de vagas, número de chamadas, reparos, timeouts e GET após geração de perguntas. Há divergências nos dois READMEs. |

Não identifiquei assets sem referência na busca por nome que justifiquem remoção imediata. As rotas usam carregamento sob demanda, mas as páginas de casos e perguntas importam `casePdf` estaticamente. Mover essa importação para o momento da exportação pode reduzir o custo de abrir essas páginas; o tamanho dos chunks de PDF não deve ser confundido com custo obrigatório do login.

**Preservar:** migrações Flyway V1–V21, wrappers Maven, lockfile npm, testes, contratos OpenAPI e políticas de dados. Migrações antigas são necessárias para histórico/checksums e bancos novos. Volumes PostgreSQL/gateway e arquivos `.env` não são lixo: contêm dados ou configuração que exigem tratamento próprio.

## Ordem sugerida de trabalho

1. Corrigir padrões de produção e limites de IA.
2. Preservar edições de perguntas e recuperação idempotente após falhas incertas.
3. Corrigir timeout integral, orçamento do ajuste e repetição que regrava dados antigos.
4. Validar o paciente candidato e cobrir início de tentativa/deadlocks em PostgreSQL.
5. Instrumentar fases, medir provedores e otimizar chamadas/tokens com comparação de qualidade.
6. Uniformizar retenção local/ledger, automatizar CI do front e atualizar documentação.

Uma revisão ampla e testes aprovados não comprovam ausência de outros bugs. Os achados acima distinguem comportamento demonstrado, problemas diretamente rastreáveis no código e condições que ainda precisam de validação na infraestrutura real.
