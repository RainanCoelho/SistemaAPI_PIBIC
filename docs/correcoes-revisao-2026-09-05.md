# Correções da revisão — 05/09/2026

Aplicadas no backend e no projeto irmão `Front-End-Casos-Clinicos`. Os itens **1.P1 e 2.P1 foram preservados conforme solicitado**: nenhuma alteração no perfil padrão ou na ativação dos limites de IA. Os READMEs foram corrigidos para descrever a configuração real.

## Achados tratados

| Item da revisão | Alteração |
| --- | --- |
| 3 | Mesclagem de perguntas preserva campos com edição local e perguntas manuais ainda não salvas. |
| 4 | Respostas truncadas, vazias, malformadas ou sem identificadores mantêm a tentativa como incerta. Erros de proxy também preservam a chave quando não há confirmação de falha pelo backend. |
| 5 | Geração e ajuste clínico aguardam 390 s no front. O backend verifica, antes de cada chamada clínica, se o orçamento de seis minutos comporta o timeout do provedor. |
| 6 | Timeout e cancelamento permanecem ativos até terminar a leitura do corpo da resposta. |
| 7 | A etapa de referências recupera um ajuste pendente antes de reenviar o conteúdo; a chave de uma nova tentativa só é criada após a persistência dos dados. |
| 8 | A pós-validação inclui uma cópia do paciente resultante das mesmas regras de complemento utilizadas na persistência. Rejeição não modifica o paciente original. |
| 9 | A primeira abertura do caso pelo aluno é serializada pelo lock do caso antes de buscar/criar a tentativa. |
| 10 | Edição, movimentação e exclusão de filhos adquirem primeiro os casos, ordenados por ID, e depois o filho; revalidam o vínculo após o bloqueio. O fluxo transacional da IA segue a mesma ordem. |
| 11 | Fallback usa UUID v4 com `crypto.getRandomValues`; sem fonte segura, a interface informa a incompatibilidade. |

O ajuste também libera o estado de espera se falhar ao criar/gravar a chave da solicitação. O salvamento em lote preserva novas edições feitas enquanto a requisição estava em andamento e atribui o ID retornado às perguntas recém-criadas, evitando recriá-las no próximo salvamento normal.

## Otimizações aplicadas

- Coerência com DTO compacto (`statusCoerencia`, `violacoes`) e cliente separado da geração clínica.
- Cache local de aprovações: até 256 hashes SHA-256, TTL de cinco minutos, chave por contexto, versão e modelo configurado. Não armazena texto clínico. Contexto alterado invalida o acerto; pós-validação nunca é eliminada.
- Geração concisa e reparo com os campos válidos preservados da saída parcial.
- Eventos por fase com duração, tokens/modelo disponíveis, resultado e ID da solicitação; persistência também é cronometrada. `scripts/resumir-latencia-ia.ps1` calcula p50/p95 dos eventos de um log.
- POST de perguntas aproveitado diretamente; dados sem mudanças não geram PUTs redundantes.
- Endpoint transacional para salvar de 1 a 100 perguntas por lote, com rollback integral, validação de vínculo e ordem de resposta estável; contrato OpenAPI atualizado.
- Gravação de rascunhos com debounce de 350 ms, descarga ao sair da página, tratamento de falha e retenção de 30 dias também para o caso salvo. As gravações pendentes mantêm o professor de origem ao trocar de conta.
- PDF importado somente na exportação. O chunk de aproximadamente 410 kB (134 kB gzip) permanece separado no build.

O acerto do cache reduz o caminho clínico normal de três chamadas para duas; isso foi verificado com provedor simulado. Não há medição de ganho percentual com IA real. Troca de modelo, calibração de tokens, mudança da política de confirmação e jobs assíncronos ficam para uma etapa orientada por medições de qualidade/latência. O orçamento não interrompe uma chamada já iniciada ou uma transação; o proxy/gateway precisa permitir a janela de espera.

## Validação

- Front: 74 testes aprovados, lint, typecheck e build Vite aprovados. Os arquivos aplicados no projeto original foram conferidos por SHA-256.
- Backend: 167 testes, sendo 161 aprovados e seis de PostgreSQL desabilitados sem `RUN_POSTGRES_TESTS=true`. Incluem cache/invalidação, schema compacto, rejeição do paciente candidato, corrida de primeira abertura, ordem de locks e rollback de lote.
- `mvn verify` inclui cobertura JaCoCo, Checkstyle e SpotBugs. Logs locais: `Sistema_Crud_API_PIBIC/target/review-final.log` e relatórios em `target/surefire-reports`.
- Nenhuma chamada a provedor real, migração ou alteração de dados de produção; as novas verificações usam mocks e H2. A suíte equivalente para PostgreSQL foi adicionada, mas precisa de execução em banco de testes isolado.

## Limites restantes

Criar perguntas sem ID no lote ainda não é idempotente após perda da resposta: consulte a lista persistida antes de repetir uma criação incerta. A primeira abertura usa lock do caso, serializando brevemente aberturas de alunos diferentes nesse mesmo caso. Cancelar no navegador encerra a espera local e não garante cancelar o trabalho no servidor.

Esta entrega não remove migrações, volumes, arquivos de ambiente ou dependências necessárias. Os demais apontamentos de manutenção da revisão original não são apresentados como resolvidos por estas correções prioritárias.
