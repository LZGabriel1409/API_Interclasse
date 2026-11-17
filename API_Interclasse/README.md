# API Interclasse ETEC

API REST para gerenciamento do interclasse, construída com Java 17, Servlets (`javax.servlet`), Gson e SQLite.

## 1. Executar

**Requisitos:** JDK 17+ e Maven 3.8+.

```bash
mvn clean package
mvn tomcat7:run
```

Base URL configurada no `pom.xml`: `http://localhost:8082/api`.
O SQLite (`interclasse.db`) é criado automaticamente na primeira conexão. Para escolher outro arquivo, defina a variável de ambiente `INTERCLASSE_DB`.

> O primeiro organizador pode ser cadastrado com a chave mestra padrão `ETEC-INTERCLASSE-2026`. Em ambiente real, altere/remova essa chave padrão antes de publicar a API.

## 2. Autenticação e formato

A autenticação usa sessão HTTP (`JSESSIONID`). Envie `Content-Type: application/json` nas requisições JSON e mantenha o cookie nas próximas chamadas (`curl -c cookies.txt` e `curl -b cookies.txt`).

Erros são retornados em JSON no formato:

```json
{"erro":"email invalido: informe um endereco no formato nome@dominio.com"}
```

Códigos comuns: `400` dados inválidos/obrigatórios; `401` sem login; `403` sem permissão; `404` recurso/rota inexistente; `409` conflito; `500` erro interno.

## 3. Rotas

Todas as rotas abaixo são relativas à Base URL (`http://localhost:8082/api`).

| Método | Rota | Acesso | Finalidade |
|---|---|---|---|
| GET | `/` | Livre | Resumo da API |
| POST | `/auth/registrar` | Livre | Cadastrar participante |
| POST | `/auth/registrar-organizador` | Chave mestra | Cadastrar organizador |
| POST | `/auth/login` | Livre | Iniciar sessão |
| POST | `/auth/logout` | Logado | Encerrar sessão |
| GET | `/auth/me` | Logado | Consultar usuário da sessão |
| GET | `/times` | Livre | Listar times |
| GET | `/times?id=1` | Livre | Consultar time |
| POST/PUT/DELETE | `/times` | Organizador | Criar/alterar/remover time (`id` em query para PUT/DELETE) |
| GET | `/turmas` | Logado | Listar/consultar turmas |
| POST/PUT/DELETE | `/turmas` | Organizador | Criar/alterar/remover turma (`id` em query) |
| GET | `/modalidades` | Logado | Listar/consultar modalidades |
| POST/PUT/DELETE | `/modalidades` | Organizador | Criar/alterar/remover modalidade (`id` em query) |
| GET | `/inscricoes` | Logado | Listar/consultar inscrições |
| POST | `/inscricoes` | Logado | Inscrever time (status inicial `PENDENTE`) |
| PUT | `/inscricoes?id=1` | Organizador | Confirmar ou negar inscrição |
| GET | `/partidas` | Logado | Listar/filtrar partidas (`modalidadeId`, `status`, `data`) |
| POST | `/partidas` | Organizador | Agendar partida |
| PATCH/PUT | `/partidas?id=1` | Organizador | Atualizar placar |
| POST | `/partidas/finalizar?id=1` | Organizador | Finalizar partida e validar regra de empate |
| GET | `/admin/configuracoes` | Organizador | Consultar configurações |
| PUT/POST | `/admin/configuracoes` | Organizador | Atualizar configurações |
| GET | `/classificacao?modalidadeId=1` | Logado | Classificação por pontos, vitórias, saldo e nome |
| GET | `/classificacao?modalidadeId=1&formato=csv` | Logado | Baixar classificação em CSV |
| GET | `/chaveamento?modalidadeId=1` | Logado | Listar partidas agrupadas por fase |
| GET | `/jogadores/ranking` | Logado | Ranking individual de gols e assistências |
| POST | `/jogadores` | Organizador | Cadastrar jogador e estatísticas iniciais |
| PUT | `/jogadores?id=1` | Organizador | Atualizar gols e assistências |

### Payloads principais

**Cadastro:**
```json
{"nome":"Ana Souza","email":"ana@etec.sp.gov.br","senha":"123456"}
```

**Time:**
```json
{"nome":"Falcões","cor":"Azul","responsavel":"Carlos","contato":"11988887777"}
```

**Partida:**
```json
{"modalidadeId":1,"timeAId":1,"timeBId":2,"data":"2026-11-20","hora":"14:00","local":"Quadra 1","fase":"GRUPOS"}
```
Valores aceitos para `fase`: `AVULSO`, `GRUPOS`, `QUARTAS`, `SEMI`, `FINAL`. A API organiza as partidas cadastradas nessas fases; o organizador deve cadastrar as partidas das fases eliminatórias após definir os classificados.

**Jogador:**
```json
{"nome":"João Silva","timeId":1,"gols":2,"assistencias":1}
```

**Atualizar estatísticas:**
```json
{"gols":5,"assistencias":3}
```

## 4. Exemplos curl

Cadastro e login (guarda o cookie da sessão):
```bash
curl -i -X POST http://localhost:8082/api/auth/registrar-organizador \
  -H "Content-Type: application/json" \
  -d '{"nome":"Prof Carlos","email":"carlos@etec.sp.gov.br","senha":"123456","chaveMestra":"ETEC-INTERCLASSE-2026"}'

curl -i -c cookies.txt -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@etec.sp.gov.br","senha":"123456"}'
```

Criar partida, consultar chaveamento e exportar CSV:
```bash
curl -i -b cookies.txt -X POST http://localhost:8082/api/partidas \
  -H "Content-Type: application/json" \
  -d '{"modalidadeId":1,"timeAId":1,"timeBId":2,"data":"2026-11-20","hora":"14:00","local":"Quadra 1","fase":"GRUPOS"}'

curl -i -b cookies.txt "http://localhost:8082/api/chaveamento?modalidadeId=1"
curl -L -b cookies.txt "http://localhost:8082/api/classificacao?modalidadeId=1&formato=csv" -o classificacao.csv
```

Cadastro e atualização de jogador:
```bash
curl -i -b cookies.txt -X POST http://localhost:8082/api/jogadores \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Silva","timeId":1,"gols":2,"assistencias":1}'

curl -i -b cookies.txt -X PUT "http://localhost:8082/api/jogadores?id=1" \
  -H "Content-Type: application/json" \
  -d '{"gols":5,"assistencias":3}'
curl -i -b cookies.txt http://localhost:8082/api/jogadores/ranking
```

## 5. Checklist de testes manuais

Marque cada caso após executar no Postman/Insomnia ou curl. Para rotas protegidas, teste sem cookie e com usuário participante para confirmar `401` e `403`.

| Rota/operação | Entrada válida | Entrada inválida | Sem permissão | Recurso inexistente |
|---|---|---|---|---|
| `POST /auth/registrar` | [ ] cadastro válido | [ ] nome vazio, email inválido, senha curta | N/A | N/A |
| `POST /auth/registrar-organizador` | [ ] chave válida | [ ] email inválido/chave ausente | [ ] chave mestra inválida | N/A |
| `POST /auth/login` | [ ] credenciais válidas | [ ] corpo vazio/email inválido | N/A | [ ] usuário não cadastrado |
| `POST /auth/logout` | [ ] encerra sessão ativa | [ ] sem corpo (esperado: sucesso) | [ ] sessão já ausente (esperado: sucesso idempotente) | N/A |
| `GET /` | [ ] retorna nome/versão/rotas | N/A | N/A | N/A |
| `GET /auth/me` | [ ] sessão ativa | [ ] sessão ausente | [ ] N/A | [ ] usuário removido/inexistente |
| `GET/POST/PUT/DELETE /times` | [ ] CRUD completo | [ ] nome vazio/id inválido | [ ] participante ou sem login nas escritas | [ ] id inexistente |
| `GET/POST/PUT/DELETE /turmas` | [ ] CRUD completo | [ ] nome/curso vazios, ano inválido | [ ] participante ou sem login nas escritas | [ ] id inexistente |
| `GET/POST/PUT/DELETE /modalidades` | [ ] CRUD completo | [ ] nome vazio/ativo inválido | [ ] participante ou sem login nas escritas | [ ] id inexistente |
| `GET/POST/PUT /inscricoes` | [ ] inscrever e confirmar | [ ] ids ausentes/status inválido | [ ] participante tentando confirmar | [ ] time/modalidade/inscrição inexistente |
| `GET/POST/PATCH /partidas` | [ ] agendar, placar e finalizar | [ ] times iguais/fase inválida/campos vazios | [ ] participante ou sem login nas escritas | [ ] partida/time/modalidade inexistente |
| `GET/PUT /admin/configuracoes` | [ ] consultar/alterar configuração | [ ] valor inválido | [ ] participante ou sem login | [ ] chave desconhecida |
| `GET /classificacao` e `formato=csv` | [ ] JSON e CSV | [ ] modalidadeId ausente/inválido | [ ] sem login | [ ] modalidade sem dados |
| `GET /chaveamento` | [ ] fases com partidas | [ ] modalidadeId ausente/inválido | [ ] sem login | [ ] modalidade inexistente |
| `GET/POST/PUT /jogadores` | [ ] cadastrar, atualizar e ordenar ranking | [ ] nome vazio/gols negativos | [ ] participante ou sem login nas escritas | [ ] time/jogador inexistente |

## 6. Regras e observações

- Inscrição nasce `PENDENTE`; somente organizador pode confirmar/negá-la.
- Somente times com inscrição `CONFIRMADA` entram em partidas.
- Partidas usam `GRUPOS`, `QUARTAS`, `SEMI` ou `FINAL` para compor o chaveamento. O endpoint `/chaveamento` agrupa as partidas existentes por fase; ele não sorteia grupos nem promove automaticamente os classificados.
- Classificação considera partidas `FINALIZADA` e os parâmetros de pontuação configurados.
- Ranking individual usa as estatísticas cadastradas pelo organizador em cada jogador; gols/assistências não são inferidos automaticamente do placar.
- A exportação CSV contém posição, time, pontos, vitórias, empates, derrotas, gols pró, gols contra e saldo.

## 7. Estrutura do projeto

- `controller/`: endpoints HTTP e validações de entrada.
- `dao/`: consultas SQL.
- `model/`: entidades.
- `filter/`: filtro de autorização de organizador.
- `util/`: conexão SQLite, JSON e sessão.
