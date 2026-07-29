# Política de dados clínicos enviados à IA

## Escopo

Esta política vale para toda geração ou ajuste de conteúdo clínico e de perguntas realizado por IA no piloto privado do SistemaAPI PIBIC. Ela se aplica aos campos estruturados, textos livres, instruções adicionais, arquivos copiados manualmente e qualquer outro dado que possa integrar um prompt.

O piloto permite exclusivamente dados:

- inteiramente sintéticos, criados para fins educacionais; ou
- previamente desidentificados, sem possibilidade razoável de associação a uma pessoa real.

Dados clínicos reais identificados ou identificáveis estão fora do escopo, mesmo quando houver consentimento. O uso assistencial também está fora do escopo.

## Dados que não podem ser enviados

Não inclua:

- nome, iniciais, apelido, assinatura ou nome social de uma pessoa real;
- CPF, RG, CNS, passaporte, matrícula, número de prontuário ou outro identificador;
- telefone, e-mail, perfil em rede social ou endereço;
- data de nascimento, admissão, alta, consulta ou procedimento em precisão identificável;
- instituição, setor, cidade pequena, profissional ou equipe que permita reconhecimento;
- fotografia, gravação, voz, imagem de exame com metadados, documento ou captura de tela;
- texto copiado de prontuário, laudo, prescrição ou mensagem sem desidentificação prévia;
- doença rara, evento público ou combinação de idade, local, profissão, datas e circunstâncias que identifique indiretamente a pessoa;
- credenciais, tokens, chaves de API ou outros segredos.

Trocar apenas o nome por “Paciente X” não torna um caso anônimo. Identificação indireta também deve ser removida.

## Preparação permitida

Ao adaptar material clínico para o ensino:

1. prefira criar um caso completamente fictício;
2. mantenha somente informações necessárias ao objetivo de aprendizagem;
3. use faixa etária quando a idade exata não for clinicamente essencial;
4. use períodos relativos, como “há três dias”, no lugar de datas de calendário;
5. generalize local, instituição, profissão e circunstâncias não essenciais;
6. altere detalhes raros que não afetem o raciocínio clínico;
7. faça uma segunda revisão antes de acionar a IA.

Não use a IA para realizar a própria desidentificação de um prontuário real. A desidentificação deve ocorrer antes que qualquer conteúdo chegue à aplicação ou ao gateway.

## Controles técnicos e suas limitações

A aplicação:

- evita enviar o nome do paciente nos prompts;
- minimiza e delimita os dados usados como contexto;
- remove alguns padrões evidentes de contato e documentos;
- limita tamanho, frequência, concorrência e duração das requisições;
- mantém as credenciais dos provedores fora do código.

Essas medidas reduzem risco, mas não detectam todas as formas de identificação direta ou indireta. Elas não substituem a decisão humana nem comprovam anonimização.

O FreeLLMAPI armazena localmente as chaves dos provedores de forma criptografada, porém os prompts são encaminhados ao provedor selecionado. Cada provedor pode ter regras próprias de processamento, registro, retenção e uso de dados. O responsável pelo piloto deve revisar essas regras antes de cadastrar uma chave.

## Revisão humana obrigatória

Um professor deve revisar toda saída antes da publicação, verificando:

- correção e atualidade clínica;
- coerência entre história, exame, hipótese, perguntas e gabaritos;
- adequação ao nível de dificuldade e ao objetivo de aprendizagem;
- inexistência de dados pessoais ou detalhes reconhecíveis;
- ausência de conduta perigosa, viés indevido ou falsa certeza;
- clareza das alternativas e existência de uma resposta inequivocamente correta quando aplicável.

Conteúdo gerado não deve ser publicado automaticamente. A IA pode inventar fatos, referências, resultados, diagnósticos e justificativas plausíveis, porém incorretos.

## Finalidade e aviso de uso

As saídas destinam-se exclusivamente ao ensino e à avaliação acadêmica. Elas:

- não constituem diagnóstico, prescrição ou orientação individual;
- não substituem profissional habilitado;
- não devem orientar atendimento de emergência ou decisão clínica real;
- não devem ser apresentadas como fonte científica sem conferência independente.

## Acesso, registros e incidentes

- Restrinja o painel do gateway a administradores e à rede privada.
- Não registre prompts, respostas completas, cabeçalhos de autorização ou chaves nos logs da aplicação.
- Dê a cada pessoa apenas o acesso necessário à sua função.
- Revogue imediatamente uma chave exposta e substitua também a chave unificada comprometida.
- Se houver envio indevido de dado identificável, interrompa as chamadas, preserve somente os registros necessários para investigar, identifique o provedor destinatário e acione os responsáveis por privacidade e segurança.
- Documente o incidente e avalie as obrigações aplicáveis antes de retomar o uso.

## Responsabilidades

O professor que solicita a geração é responsável por conferir o caso antes do envio e revisar a saída. A administração da plataforma é responsável por controlar acessos, segredos, provedores e configurações. A equipe técnica é responsável por manter os controles e tratar falhas sem expor conteúdo clínico nos logs.

Antes do lançamento público ou de qualquer uso com dados reais, a organização deve realizar avaliação jurídica e de segurança, definir base legal e retenção, revisar contratos dos fornecedores, avaliar a necessidade de relatório de impacto e estabelecer processo formal de atendimento a titulares e incidentes. Esta política operacional não substitui orientação jurídica sobre a LGPD.
