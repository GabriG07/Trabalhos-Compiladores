package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.List;

public class ValPlaySemanticoUtils {

    public static List<String> errosSemanticos = new ArrayList<>();
    public static List<String> avisos = new ArrayList<>();

    public static void adicionarErro(int linha, String mensagem) {
        errosSemanticos.add("Linha " + linha + ": " + mensagem);
    }

    public static void adicionarAviso(String mensagem) {
        avisos.add(mensagem);
    }
}