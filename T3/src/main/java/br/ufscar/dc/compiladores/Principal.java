package br.ufscar.dc.compiladores;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class Principal {

    /*
     * args[0] -> arquivo do qual será lida a entrada (código na linguagem LA)
     * args[1] -> arquivo onde será escrita a saída (lista com os tokens identificados)
     */
    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(args[1])) {
            CharStream cs = CharStreams.fromFileName(args[0]);
            LALexer lex = new LALexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lex);
            tokens.fill();

            // T1: análise léxica
            boolean erroLexico = false;
            for (Token t : tokens.getTokens()) {
                String nomeToken = LALexer.VOCABULARY.getDisplayName(t.getType());

                if (nomeToken.equals("ERRO")) {
                    pw.println("Linha " + t.getLine() + ": " + t.getText() + " - simbolo nao identificado");
                    erroLexico = true;
                    break;
                } else if (nomeToken.equals("COMENTARIO_NAO_FECHADO")) {
                    pw.println("Linha " + t.getLine() + ": " + "comentario nao fechado");
                    erroLexico = true;
                    break;
                } else if (nomeToken.equals("CADEIA_NAO_FECHADA")) {
                    pw.println("Linha " + t.getLine() + ": " + "cadeia literal nao fechada");
                    erroLexico = true;
                    break;
                }
            }

            // T2: análise sintática
            boolean erroSintatico = false;
            LAParser.ProgramaContext arvore = null;
            if (!erroLexico) {
                LAParser parser = new LAParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new MyCustomErrorListener(pw));
                try {
                    arvore = parser.programa(); // guarda a árvore para o T3
                } catch (ParseCancellationException e) {
                    erroSintatico = true;
                }
            }

            // T3: análise semântica (só roda se não houve erros anteriores)
            if (!erroLexico && !erroSintatico && arvore != null) {
                LASemanticoUtils.errosSemanticos.clear(); // garante lista limpa
                LASemantico semantico = new LASemantico();
                semantico.visitPrograma(arvore);

                for (String erro : LASemanticoUtils.errosSemanticos) {
                    pw.println(erro);
                }
            }

            pw.println("Fim da compilacao");

        } catch (FileNotFoundException e) {
            System.err.println("O arquivo/diretório não existe: " + args[1]);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}