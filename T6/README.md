# T6 - ValPlay

Trabalho 6 da disciplina **Construção de Compiladores** - DC/UFSCar  

**Compilador para a linguagem ValPlay:** uma linguagem declarativa de domínio
específico para descrever **composições de time** e **estratégias de site** em
Valorant, validá-las contra um banco de agentes e mapas reais, e gerar um
*playbook* visual em HTML.

- Gabriel Henrique Rodrigues RA: 813345

---

## 1. A linguagem 

Um programa (`.vp`) declara o **mapa**, uma **composição de 5 agentes** e um ou
mais blocos **execute** (a sequência de ações do time num site):

```
// Composicao padrao de ataque para Ascent
mapa Ascent

composicao {
    Jett
    Sova
    Omen
    Killjoy
    Sage
}

execute site A {
    Sova usa "Owl Drone"
    Sova usa "Recon Bolt" em "Boba"
    Omen usa "Dark Cover" em "Mid", "Market"
    Killjoy usa "Nanoswarm" em "Lixo"
    Jett usa "Cloudburst" em "entrada A"
    Jett entra
    Sova ultimate "Hunter's Fury" em "Boba"
    Jett planta
}
```

Os nomes de agentes, habilidades, ultimates e mapas são os **reais** do jogo. O
compilador conhece esse "vocabulário" através de um banco embutido
(`BancoDeAgentes.java`), que funciona como a biblioteca padrão da linguagem.

### Palavras-chave

`mapa`, `composicao`, `execute`, `site`, `usa`, `ultimate`, `em`, `entra`, `planta`

### Ações dentro de um `execute`

| Forma                                            | Significado                                  |
|--------------------------------------------------|----------------------------------------------|
| `Agente usa "Habilidade"`                         | usa uma habilidade básica                    |
| `Agente usa "Habilidade" em "Pos1", "Pos2"`       | usa mirando uma ou mais posições             |
| `Agente ultimate "Ultimate" em "Pos"`             | usa a ultimate                               |
| `Agente entra`                                    | entrada no site                              |
| `Agente planta`                                   | planta a spike                               |

---

## 2. Como compilar

Pré-requisitos: **JDK 21** e **Maven 3.9+**.

```bash
mvn clean package
```

O Maven baixa o ANTLR, gera o lexer/parser/visitor a partir de
`src/main/antlr4/.../ValPlay.g4` e empacota tudo:

```
target/T6-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 3. Como executar

```bash
java -jar target/T6-1.0-SNAPSHOT-jar-with-dependencies.jar <entrada.vp> <saida>
```

- `entrada.vp` - arquivo de código-fonte.
- `saida` - arquivo de saída. **Em caso de sucesso** recebe o **playbook HTML**
  (use a extensão `.html`); **em caso de erro** recebe o **relatório** de erros
  seguido de `Fim da compilacao`.

Os avisos de balanceamento, por não impedirem a compilação, aparecem **dentro do
playbook** (um banner), e não como texto no relatório.

Exemplo:

```bash
java -jar target/T6-1.0-SNAPSHOT-jar-with-dependencies.jar \
     casos-de-teste/01_valido.vp playbook.html
```

Abra `playbook.html` no navegador para ver a composição (cartões coloridos por
função), a *timeline* de cada execute e o esquema top-down do site com os
utilitários marcados por categoria.

---

## 4. Verificações semânticas

Além do que a gramática já garante, o analisador semântico
(`ValPlaySemantico.java`) acumula **todos** os erros (não para no primeiro):

1. **Mapa existe** - o mapa precisa estar no banco de mapas.
2. **Tamanho da composição** - exatamente 5 agentes.
3. **Sem repetição** - nenhum agente repetido na composição.
4. **Agente existe** - cada agente da composição precisa existir no banco.
5. **Site existe no mapa** - `execute site C` só vale se o mapa tiver site C
   (ex.: Haven tem A/B/C; Ascent só A/B).
6. **Agente em jogo** - todo agente citado numa ação precisa estar na composição.
7. **Habilidade pertence ao agente** - checagem de "tipo": Jett não usa
   "Recon Bolt" (do Sova).
8. **Verbo correto** - `usa` para habilidades básicas e `ultimate` para a
   ultimate; trocar os dois é erro.
9. **Cargas** - o nº de usos de uma habilidade num execute não pode passar das
   cargas (ex.: "Recon Bolt" tem 1 carga).

E ainda **avisos** de balanceamento (não impedem a compilação):

10. Composição sem Controlador, sem Iniciador, ou com 4+ Duelistas.

Os casos em `casos-de-teste/` abrangem cada uma dessas regras:

| Arquivo                       | O que demonstra                                       |
|-------------------------------|-------------------------------------------------------|
| `01_valido.vp`                | programa correto → gera o playbook                    |
| `02_habilidade_errada.vp`     | habilidade que não é do agente / verbo trocado        |
| `03_cargas.vp`                | uso de habilidade acima das cargas                    |
| `04_comp_e_mapa.vp`           | mapa inexistente, composição inválida, site/agente    |
| `05_aviso_balanceamento.vp`   | avisos de função (5 duelistas)                         |

---


## 5. Editando o "jogo" (agentes, habilidades, mapas)

Tudo que o compilador conhece sobre Valorant fica em **`BancoDeAgentes.java`**.
Para adicionar um agente, basta uma linha no bloco `static`:

```java
ag("Reyna", "Duelista", "Empress",
        "Leer", 2, "flash", "Dismiss", 1, "dash", "Devour", 1, "heal");
//  nome    função      ultimate    habilidade  cargas  categoria  ...
```

Para um mapa novo: `MAPAS.put("Abyss", List.of("A", "B"));`

As cargas e a lista de mapas foram simplificadas para a elaboração desse trabalho.

---
