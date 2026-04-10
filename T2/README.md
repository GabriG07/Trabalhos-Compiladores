# T2 — Analisador Sintático para a Linguagem Algorítmica (LA)

Trabalho 2 da disciplina **Construção de Compiladores** — DC/UFSCar  
Implementação de um analisador sintático para a linguagem LA, desenvolvida pelo prof. Jander.

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
target/T2-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## Executando o analisador sintático

O analisador recebe **dois argumentos obrigatórios**: o arquivo de entrada e o arquivo de saída.
 
```cmd
java -jar target\T2-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo-entrada> <arquivo-saida>
```
 
Exemplo:
 
```cmd
java -jar target\T2-1.0-SNAPSHOT-jar-with-dependencies.jar casos-de-teste\entrada.txt saida.txt
```

### O que o analisador produz
 
- **Programa sintaticamente correto** — o arquivo de saída conterá apenas:
  ```
  Fim da compilacao
  ```
- **Erro léxico** — detectado antes da análise sintática; o processamento é interrompido e o erro é reportado, por exemplo, como:
  ```
  Linha 30: | - simbolo nao identificado
  Fim da compilacao
  ```


  ```
  Linha 14: comentario nao fechado
  Fim da compilacao
  ```

- **Erro sintático** — reporta a linha e o lexema que causou a detecção do erro:
  ```
  Linha 10: erro sintatico proximo a leia
  Fim da compilacao
  ```
