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
     * args[0] -> arquivo do qual sera lida a entrada (codigo na linguagem ValPlay)
     * args[1] -> arquivo onde sera escrita a saida:
     *            - em caso de sucesso: o playbook HTML gerado;
     *            - em caso de erro: o relatorio de erros + "Fim da compilacao".
     */
    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(args[1])) {
            CharStream cs = CharStreams.fromFileName(args[0]);
            ValPlayLexer lexer = new ValPlayLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();

            boolean erroLexico = verificarErrosLexicos(tokens, pw);
            boolean erroSintatico = false;
            ValPlayParser.PlanoContext arvore = null;

            if (!erroLexico) {
                ValPlayParser parser = new ValPlayParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new MyCustomErrorListener(pw));
                try {
                    arvore = parser.plano();
                } catch (ParseCancellationException e) {
                    erroSintatico = true;
                }
            }

            boolean erroSemantico = false;
            if (!erroLexico && !erroSintatico && arvore != null) {
                ValPlaySemanticoUtils.errosSemanticos.clear();
                ValPlaySemanticoUtils.avisos.clear();
                ValPlaySemantico semantico = new ValPlaySemantico();
                semantico.visitPlano(arvore);
                erroSemantico = !ValPlaySemanticoUtils.errosSemanticos.isEmpty();
                if (erroSemantico) {
                    ValPlaySemanticoUtils.errosSemanticos.forEach(pw::println);
                }
            }

            if (erroLexico || erroSintatico || erroSemantico) {
                pw.println("Fim da compilacao");
            } else if (arvore != null) {
                ValPlayGerador gerador = new ValPlayGerador();
                gerador.visitPlano(arvore);
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
            String nomeToken = ValPlayLexer.VOCABULARY.getDisplayName(token.getType());
            if (nomeToken.equals("ERRO")) {
                pw.println("Linha " + token.getLine() + ": " + token.getText() + " - simbolo nao identificado");
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