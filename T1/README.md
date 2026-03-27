# T1 — Analisador Léxico para a Linguagem Algorítmica (LA)

Trabalho 1 da disciplina **Construção de Compiladores** — DC/UFSCar  
Implementação de um analisador léxico para a linguagem LA, desenvolvida pelo prof. Jander.

- Gabriel Henrique Rodrigues RA: 813345

---

## Pré-requisitos

| Ferramenta | Versão utilizada | Versão mínima recomendada |
|------------|-----------------|--------------------------|
| [Java JDK](https://www.oracle.com/java/technologies/downloads/) | 21 | 11+ (pode funcionar em versões anteriores) |
| [Apache Maven](https://maven.apache.org/) | 3.9.13 | 3.6+ (pode funcionar em versões anteriores) |

> **Nota:** O projeto foi desenvolvido e testado com Java 21 e Maven 3.9.13. Versões mais antigas podem funcionar, mas não são garantidas.

### Como verificar se já estão instalados

```bash
java -version
mvn -version
```

### Como instalar

- **Java:** baixe e instale o JDK em (https://www.oracle.com/java/technologies/downloads/).  
- **Maven:** baixe em https://maven.apache.org/download.cgi e siga as instruções de instalação. Certifique-se de que o executável `mvn` esteja no `PATH`.

---

## Compilando o projeto

Na raiz do repositório (onde está o arquivo `pom.xml`), execute:

```cmd
mvn package
```

Esse comando compila o código-fonte e empacota o projeto em um arquivo `.jar` executável dentro da pasta `target/`, com nome similar a:

```
target/T1-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## Executando o analisador léxico

O analisador recebe **dois argumentos obrigatórios**: o arquivo de entrada e o arquivo de saída.
 
```cmd
java -jar target\T1-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo-entrada> <arquivo-saida>
```
 
Exemplo:
 
```cmd
java -jar target\T1-1.0-SNAPSHOT-jar-with-dependencies.jar casos-de-teste\entrada.txt saida.txt
```

### O que o analisador produz

- **Tokens válidos** são escritos no arquivo de saída no formato:
  ```
  <'lexema','TIPO'>        (para IDENT, CADEIA, NUM_INT, NUM_REAL)
  <'lexema','lexema'>      (para palavras-chave e símbolos)
  ```
- **Erros léxicos** interrompem o processamento e escrevem uma única linha de erro, por exemplo:
  ```
  Linha 5: ~ - simbolo nao identificado
  Linha 3: comentario nao fechado
  Linha 7: cadeia literal nao fechada
  ```
- Espaços em branco e comentários são ignorados e não aparecem na saída.
