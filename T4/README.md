# T4 - Analisador Semântico para a Linguagem Algorítmica (LA)

Trabalho 4 da disciplina **Construção de Compiladores** - DC/UFSCar  
Implementação de um analisador semântico para a linguagem LA, desenvolvida pelo prof. Jander.

- Gabriel Henrique Rodrigues RA: 813345

---

## Pré-requisitos

| Ferramenta                                                      | Versão utilizada | Versão mínima recomendada                   |
|-----------------------------------------------------------------|------------------|---------------------------------------------|
| [Java JDK](https://www.oracle.com/java/technologies/downloads/) | 21               | 11+ (pode funcionar em versões anteriores)  |
| [Apache Maven](https://maven.apache.org/)                       | 3.9.13           | 3.6+ (pode funcionar em versões anteriores) |

> **Nota:** O projeto foi desenvolvido e testado com Java 21 e Maven 3.9.13. Versões mais antigas podem funcionar, mas
> não são garantidas.

### Como verificar se já estão instalados

```bash
java -version
mvn -version
```

### Como instalar

- **Java:** baixe e instale o JDK em (https://www.oracle.com/java/technologies/downloads/).
- **Maven:** baixe em https://maven.apache.org/download.cgi e siga as instruções de instalação. Certifique-se de que o
  executável `mvn` esteja no `PATH`.

---

## Compilando o projeto

Na raiz do repositório (onde está o arquivo `pom.xml`), execute:

```cmd
mvn package
```

Esse comando compila o código-fonte e empacota o projeto em um arquivo `.jar` executável dentro da pasta `target/`, com
nome similar a:

```
target/T4-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## Executando o analisador semântico

O analisador recebe **dois argumentos obrigatórios**: o arquivo de entrada e o arquivo de saída.

```cmd
java -jar target\T4-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo-entrada> <arquivo-saida>
```

Exemplo:

```cmd
java -jar target\T4-1.0-SNAPSHOT-jar-with-dependencies.jar casos-de-teste\entrada.txt saida.txt
```

---

## O que o analisador produz

O analisador semântico **não interrompe a execução ao encontrar o primeiro erro**, ele continua processando o arquivo e
reporta todos os erros encontrados. Ao final, sempre imprime `Fim da compilacao`.

- **Programa sem erros semânticos** - o arquivo de saída conterá apenas:
  ```
  Fim da compilacao
  ```

- **Erro léxico** - detectado antes da análise semântica; o processamento é interrompido e o erro é reportado:
  ```
  Linha 30: | - simbolo nao identificado
  Fim da compilacao
  ```

- **Erro sintático** - detectado antes da análise semântica, o processamento é interrompido e o erro é reportado:
  ```
  Linha 10: erro sintatico proximo a leia
  Fim da compilacao
  ```

- **Erros semânticos** - todos os erros encontrados são listados antes do `Fim da compilacao`:
  ```
  Linha 6: identificador troco ja declarado anteriormente
  Linha 17: identificador totalAlimento nao declarado
  Fim da compilacao
  ```

---

## Erros semânticos detectados

O analisador detecta 4 categorias de erros semânticos:

1. **Identificador já declarado** - variável, constante, procedimento, função ou tipo declarado mais de uma vez no mesmo
   escopo:
   ```
   Linha 6: identificador troco ja declarado anteriormente
   ```

2. **Tipo não declarado** - uso de um tipo que não é nativo da linguagem nem foi definido pelo usuário:
   ```
   Linha 7: tipo inteir nao declarado
   ```

3. **Identificador não declarado** - uso de variável, constante, função ou procedimento sem declaração prévia:
   ```
   Linha 11: identificador idades nao declarado
   ```

4. **Atribuição incompatível** - tentativa de atribuir a uma variável um valor de tipo incompatível:
   ```
   Linha 11: atribuicao nao compativel para valorTotal
   ```

5. **Identificadores já declarados ou não declarados envolvendo ponteiros, registros e funções** - as verificações 1 e 3
   foram estendidas para também incluir esses casos, inclusive acessos a campos de registro (`pessoa.endereco.rua`) e
   desreferência de ponteiros (`^p`).


6. **Incompatibilidade entre argumentos e parâmetros formais** - na chamada de um procedimento ou função, o número, a ordem e os tipos dos argumentos devem coincidir exatamente com os parâmetros:
   ```
   Linha 25: incompatibilidade de parametros na chamada de soma
   ```

7. **Atribuição incompatível envolvendo ponteiros e registros**:
   ```
   Linha 14: atribuicao nao compativel para ^p
   Linha 22: atribuicao nao compativel para pessoa.idade

8. **Comando `retorne` em escopo não permitido** - `retorne` só é válido dentro do corpo de uma `funcao`. Usá-lo no corpo do `algoritmo` principal ou dentro de um `procedimento` gera erro:
   ```
   Linha 8: comando retorne nao permitido nesse escopo
   ```
