package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class LASemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();

    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (var ta : ctx.termo()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, ta);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux) {
                if ((ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL) &&
                        (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) {
                    ret = TabelaDeSimbolos.TipoLA.REAL;
                } else if (aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (var fa : ctx.fator()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, fa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux) {
                if ((ret == TabelaDeSimbolos.TipoLA.INTEIRO || ret == TabelaDeSimbolos.TipoLA.REAL) &&
                        (aux == TabelaDeSimbolos.TipoLA.INTEIRO || aux == TabelaDeSimbolos.TipoLA.REAL)) {
                    ret = TabelaDeSimbolos.TipoLA.REAL;
                } else if (aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (var parcela : ctx.parcela()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, parcela);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                ret = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.ParcelaContext ctx) {
        if (ctx.parcela_nao_unario() != null) {
            return verificarTipo(tabela, ctx.parcela_nao_unario());
        }
        return verificarTipo(tabela, ctx.parcela_unario());
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) {
            return TabelaDeSimbolos.TipoLA.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return TabelaDeSimbolos.TipoLA.REAL;
        }
        if (ctx.IDENT() != null && !ctx.expressao().isEmpty()) {
            String nome = ctx.IDENT().getText();
            if (!tabela.existeNoEscopoAtual(nome)) {
                adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + nome + " nao declarado");
                return TabelaDeSimbolos.TipoLA.INVALIDO;
            }
            return tabela.verificar(nome);
        }
        if (ctx.identificador() != null) {
            String nome = ctx.identificador().IDENT(0).getText();
            if (!tabela.existeNoEscopoAtual(nome)) {
                adicionarErroSemantico(ctx.identificador().start, "identificador " + nome + " nao declarado");
                return TabelaDeSimbolos.TipoLA.INVALIDO;
            }
            return tabela.verificar(nome);
        }
        if (!ctx.expressao().isEmpty()) {
            return verificarTipo(tabela, ctx.expressao(0));
        }
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return TabelaDeSimbolos.TipoLA.LITERAL;
        }

        return TabelaDeSimbolos.TipoLA.ENDERECO;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.ExpressaoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (var tl : ctx.termo_logico()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, tl);
            if (ret == null) ret = aux;
            else if (ret != aux) ret = TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Termo_logicoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (var fl : ctx.fator_logico()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, fl);
            if (ret == null) ret = aux;
            else if (ret != aux) ret = TabelaDeSimbolos.TipoLA.INVALIDO;
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Fator_logicoContext ctx) {
        return verificarTipo(tabela, ctx.parcela_logica());
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return verificarTipo(tabela, ctx.exp_relacional());
        }
        return TabelaDeSimbolos.TipoLA.LOGICO;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_relacionalContext ctx) {
        if (ctx.op_relacional() != null) {
            verificarTipo(tabela, ctx.exp_aritmetica(0)); // verifica os lados
            verificarTipo(tabela, ctx.exp_aritmetica(1));
            return TabelaDeSimbolos.TipoLA.LOGICO;
        }
        return verificarTipo(tabela, ctx.exp_aritmetica(0));
    }
}
