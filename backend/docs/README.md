# Championshipz — docs

Documentação do backend.

## Rodar o backend

Na pasta `backend/`. O `make` cuida do Mongo do `docker-compose.yml` sozinho.

```bash
make          # lista os alvos
make dev      # sobe em segundo plano, log em build/api.log
make logs     # acompanha o log
make status   # checa se respondeu
make stop     # encerra
make test     # roda os testes
```

`make run` sobe em primeiro plano, se preferir prender o terminal. A porta é
sobrescrevível: `make dev PORT=9090`.

## Modelo de dados

Abra **`data-model.html`** no navegador — diagrama de classes das três coleções
(`teams`, `championships`, `matches`), do documento embutido (`Participant`) e dos
enums (`ChampionshipType`, `MatchStage`). Renderiza sozinho (Mermaid via CDN).

Duas coisas do diagrama que valem atenção porque não são óbvias no código:

- **`Match.championship` guarda a _slug_, não o id.** Por isso renomear a slug de um
  campeonato precisa repontuar os jogos junto — se não, todos ficam órfãos e a
  classificação zera. Coberto por `ChampionshipRenameTest`.
- **`Championship.ownerId` nulo significa campeonato de demonstração**: visível para
  todo mundo e imutável para todo mundo, inclusive para quem está logado.

Nada de classificação é armazenado. Pontos, saldo e forma são derivados dos jogos a
cada requisição, em `StandingsService`.

## API (OpenAPI / Swagger)

- **Swagger UI** (com a app no ar): http://localhost:8080/swagger-ui.html
- **Spec exportada**: [`openapi.yaml`](./openapi.yaml)

Para regravar a spec com a app no ar:

```bash
make openapi
```

## Autenticação

Leitura é pública; escrita exige um JWT emitido pelo Clerk e restrito ao dono do
campeonato.

O backend é um _resource server_: valida a assinatura contra o JWKS público do issuer
e lê a claim `sub` como `ownerId`. Não há segredo compartilhado com o Clerk, e nenhuma
chamada à API deles.

O issuer vem de `app.auth.issuer-uri`, sobrescrevível por `CLERK_ISSUER_URI`. Se ficar
**vazio**, o comportamento é fechar: leitura segue pública e toda escrita é recusada —
em vez de liberar geral, que seria a falha silenciosa perigosa.

As chaves são buscadas na primeira validação de token, não no boot: a API sobe mesmo
sem internet, e só o login depende da rede.

## Testes

```bash
make test
```

- `ChampionshipRenameTest` — renomear preserva os jogos, slug repetida é recusada,
  só o dono renomeia, campeonato de demonstração não é renomeável.
- `CupLifecycleTest` — o ciclo inteiro de uma copa: 8 times em 2 grupos, os 12 jogos
  de grupo, o chaveamento materializando sozinho, semifinal decidida nos pênaltis,
  disputa de 3º lugar e campeão.
