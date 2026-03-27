package br.ufscar.dc.compiladores;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class Principal {

    /*
     * args[0] -> arquivo do qual será lida a entrada (código na linguagem LA)
     * args[1] -> arquivo onde será escrita a saída (lista com os tokens identificados)
     */
    public static void main(String[] args) {
        try {
            CharStream cs = CharStreams.fromFileName(args[0]);
            String arquivoSaida = args[1];
            LaLexer lex = new LaLexer(cs);

            Token t = null;
            try (PrintWriter pw = new PrintWriter(arquivoSaida)) {
                while ((t = lex.nextToken()).getType() != Token.EOF) {
                    String nomeToken = LaLexer.VOCABULARY.getDisplayName(t.getType());


                    if (nomeToken.equals("ERRO")) {
                        pw.println("Linha " + t.getLine() + ": " + t.getText() +  " - simbolo nao identificado");
                        break;

                    } else if (nomeToken.equals("COMENTARIO_NAO_FECHADO")){
                        pw.println("Linha " + t.getLine() + ": " + "comentario nao fechado");
                        break;
                    }
                    else if (nomeToken.equals("CADEIA_NAO_FECHADA")) {
                        pw.println("Linha " + t.getLine() + ": " + "cadeia literal nao fechada");
                        break;
                    }
                    else { //Caso não tenha nenhum erro, então executa esse else
                        // Formato: <'lexema',TIPO>  para tokens que sejam IDENT,
                        //          CADEIA, NUM_INT ou NUM_REAL, onde o tipo é o nome do token.
                        // Formato: <'lexema','lexema'>  para todos os demais.
                        if (nomeToken.equals("IDENT") || nomeToken.equals("CADEIA") || nomeToken.equals("NUM_INT") || nomeToken.equals("NUM_REAL")) {
                            pw.println("<" + "'" + t.getText() + "'" + "," + nomeToken + ">");
                        } else {
                            pw.println("<" + "'" + t.getText() + "'" + "," + "'" + t.getText() + "'" + ">");
                        }
                    }
                }
            }
        } catch(FileNotFoundException e){
            System.err.println("O arquivo/diretório não existe:" + args[1]);
        } catch (IOException ioException) {
        }

    }
}