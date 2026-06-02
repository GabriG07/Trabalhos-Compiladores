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
            LALexer lexer = new LALexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();

            boolean erroLexico = verificarErrosLexicos(tokens, pw);
            boolean erroSintatico = false;
            LAParser.ProgramaContext arvore = null;

            if (!erroLexico) {
                LAParser parser = new LAParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new MyCustomErrorListener(pw));
                try {
                    arvore = parser.programa();
                } catch (ParseCancellationException e) {
                    erroSintatico = true;
                }
            }

            boolean erroSemantico = false;
            if (!erroLexico && !erroSintatico && arvore != null) {
                LASemanticoUtils.errosSemanticos.clear();
                LASemantico semantico = new LASemantico();
                semantico.visitPrograma(arvore);
                erroSemantico = !LASemanticoUtils.errosSemanticos.isEmpty();
                if (erroSemantico) {
                    LASemanticoUtils.errosSemanticos.forEach(pw::println);
                }
            }

            if (erroLexico || erroSintatico || erroSemantico) {
                pw.println("Fim da compilacao");
            } else if (arvore != null) {
                LAGeradorC gerador = new LAGeradorC();
                gerador.visitPrograma(arvore);
                pw.print(gerador.saida);
            }
        } catch (FileNotFoundException e) {
            System.err.println("O arquivo/diretório não existe: " + args[1]);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static boolean verificarErrosLexicos(CommonTokenStream tokens, PrintWriter pw) {
        for (Token token : tokens.getTokens()) {
            String nomeToken = LALexer.VOCABULARY.getDisplayName(token.getType());
            if (nomeToken.equals("ERRO")) {
                pw.println("Linha " + token.getLine() + ": " + token.getText() + " - simbolo nao identificado");
                return true;
            }
            if (nomeToken.equals("COMENTARIO_NAO_FECHADO")) {
                pw.println("Linha " + token.getLine() + ": comentario nao fechado");
                return true;
            }
            if (nomeToken.equals("CADEIA_NAO_FECHADA")) {
                pw.println("Linha " + token.getLine() + ": cadeia literal nao fechada");
                return true;
            }
        }
        return false;
    }
}