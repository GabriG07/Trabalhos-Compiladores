package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TabelaDeSimbolos {

    public enum TipoLA {
        LITERAL,
        INTEIRO,
        REAL,
        LOGICO,
        ENDERECO,
        PONTEIRO,
        REGISTRO,
        TIPO_DEFINIDO, // para declarações 'tipo'
        INVALIDO
    }

    //diferencia, por exemplo, uma variável de uma função/procedimento ou de uma definição de tipo
    public enum Categoria {
        VARIAVEL,
        CONSTANTE,
        FUNCAO,
        PROCEDIMENTO,
        TIPO
    }


    public static class EntradaTabelaDeSimbolos {
        public String nome;
        public TipoLA tipo;
        public Categoria categoria;

        // Campos de variáveis com tipo registro (nome do campo -> entrada)
        public Map<String, EntradaTabelaDeSimbolos> campos = new LinkedHashMap<>();

        // Quando a variável é de um tipo definido pelo usuário, guarda o nome do tipo
        public String nomeTipo;

        // Para ponteiros, guarda o tipo do que está sendo apontado
        public TipoLA tipoApontado = TipoLA.INVALIDO;
        public String nomeTipoApontado; // se aponta para registro nomeado

        // Parametros de funções e procedimentos
        public List<TipoLA> tiposParam = new ArrayList<>();
        public List<String> nomesParam = new ArrayList<>();

        // Tipo de retorno (apenas para funções)
        public TipoLA tipoRetorno = TipoLA.INVALIDO;

        public EntradaTabelaDeSimbolos(String nome, TipoLA tipo, Categoria categoria) {
            this.nome = nome;
            this.tipo = tipo;
            this.categoria = categoria;
        }
    }

    // Pilha de escopos. O índice 0 é o escopo global, o último é o atual.
    private final List<Map<String, EntradaTabelaDeSimbolos>> pilhaDeEscopos;

    public TabelaDeSimbolos() {
        this.pilhaDeEscopos = new ArrayList<>();
        criarNovoEscopo(); // escopo global
    }

    public void criarNovoEscopo() {
        pilhaDeEscopos.add(new HashMap<>());
    }

    public void abandonarEscopo() {
        if (pilhaDeEscopos.size() > 1) { // nunca remove o escopo global
            pilhaDeEscopos.remove(pilhaDeEscopos.size() - 1);
        }
    }

    // Adiciona uma entrada já construída no escopo atual
    public void adicionar(EntradaTabelaDeSimbolos entrada) {
        escopoAtual().put(entrada.nome, entrada);
    }

    public void adicionar(String nome, TipoLA tipo, Categoria categoria) {
        adicionar(new EntradaTabelaDeSimbolos(nome, tipo, categoria));
    }

    // Verifica se o identificador existe em algum escopo visivel
    public boolean existe(String nome) {
        for (int i = pilhaDeEscopos.size() - 1; i >= 0; i--) {
            if (pilhaDeEscopos.get(i).containsKey(nome)) {
                return true;
            }
        }
        return false;
    }

    public boolean existeNoEscopoAtual(String nome) {
        return escopoAtual().containsKey(nome);
    }

    //Retorna o tipo do identificador, buscando do escopo mais interno para o mais externo
    public TipoLA verificar(String nome) {
        EntradaTabelaDeSimbolos e = obterEntrada(nome);
        return (e == null) ? TipoLA.INVALIDO : e.tipo;
    }

    //Retorna a entrada completa do identificador, buscando do escopo mais interno para o mais externo
    public EntradaTabelaDeSimbolos obterEntrada(String nome) {
        for (int i = pilhaDeEscopos.size() - 1; i >= 0; i--) {
            if (pilhaDeEscopos.get(i).containsKey(nome)) {
                return pilhaDeEscopos.get(i).get(nome);
            }
        }
        return null;
    }

    private Map<String, EntradaTabelaDeSimbolos> escopoAtual() {
        return pilhaDeEscopos.get(pilhaDeEscopos.size() - 1);
    }
}