package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabelaDeSimbolos {

    public enum TipoLA {
        LITERAL,
        INTEIRO,
        REAL,
        LOGICO,
        ENDERECO,
        TIPO_DEFINIDO,  // para declarações 'tipo'
        INVALIDO
    }

    class EntradaTabelaDeSimbolos {
        String nome;
        TipoLA tipo;

        private EntradaTabelaDeSimbolos(String nome, TipoLA tipo) {
            this.nome = nome;
            this.tipo = tipo;
        }
    }

    // Cada escopo é um Map independente
    private final List<Map<String, EntradaTabelaDeSimbolos>> pilhaDeEscopos;

    public TabelaDeSimbolos() {
        this.pilhaDeEscopos = new ArrayList<>();
        criarNovoEscopo(); // escopo global
    }

    public void criarNovoEscopo() {
        pilhaDeEscopos.add(new HashMap<>());
    }

    public void abandonarEscopo() {
        if (pilhaDeEscopos.size() > 1) { // mantém sempre o global
            pilhaDeEscopos.remove(pilhaDeEscopos.size() - 1);
        }
    }

    // Adiciona apenas no escopo atual (topo da pilha)
    public void adicionar(String nome, TipoLA tipo) {
        escopoAtual().put(nome, new EntradaTabelaDeSimbolos(nome, tipo));
    }

    // Verifica em todos os escopos, do mais interno para o mais externo
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

    // Retorna o tipo, buscando do escopo mais interno para o mais externo
    public TipoLA verificar(String nome) {
        for (int i = pilhaDeEscopos.size() - 1; i >= 0; i--) {
            if (pilhaDeEscopos.get(i).containsKey(nome)) {
                return pilhaDeEscopos.get(i).get(nome).tipo;
            }
        }
        return TipoLA.INVALIDO;
    }

    private Map<String, EntradaTabelaDeSimbolos> escopoAtual() {
        return pilhaDeEscopos.get(pilhaDeEscopos.size() - 1);
    }
}