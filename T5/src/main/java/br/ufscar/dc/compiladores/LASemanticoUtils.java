package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.Token;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.EntradaTabelaDeSimbolos;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.TipoLA;

import java.util.ArrayList;
import java.util.List;

public class LASemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();

    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    /**
     * Compatibilidade para Atribuição
     *
     * Permitido:
     *   - inteiro <-> real
     *   - literal <-> literal
     *   - logico <-> logico
     *   - ponteiro <- endereço
     *   - registro <- registro do mesmo tipo
     */
    public static boolean tiposCompativeisAtribuicao(TipoLA esq, TipoLA dir, String nomeTipoEsq, String nomeTipoDir) {
        // Numérico <-> numérico
        boolean esqNum = (esq == TipoLA.INTEIRO || esq == TipoLA.REAL);
        boolean dirNum = (dir == TipoLA.INTEIRO || dir == TipoLA.REAL);
        if (esqNum && dirNum) return true;

        // Ponteiro <- endereço
        if (esq == TipoLA.PONTEIRO && dir == TipoLA.ENDERECO) return true;

        // Registro <- Registro
        if (esq == TipoLA.REGISTRO && dir == TipoLA.REGISTRO) {
            if (nomeTipoEsq == null || nomeTipoDir == null) return true;
            return nomeTipoEsq.equals(nomeTipoDir);
        }

        return esq == dir;
    }

    public static boolean tiposCompativeisParametro(TipoLA formal, TipoLA atual, String nomeTipoFormal, String nomeTipoAtual) {
        if (formal == TipoLA.INVALIDO || atual == TipoLA.INVALIDO) return true;

        if (formal == TipoLA.PONTEIRO && atual == TipoLA.ENDERECO) return true;

        if (formal == TipoLA.REGISTRO && atual == TipoLA.REGISTRO) {
            if (nomeTipoFormal == null || nomeTipoAtual == null) return true;
            return nomeTipoFormal.equals(nomeTipoAtual);
        }

        return formal == atual;
    }

    public static TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_aritmeticaContext ctx) {
        TipoLA ret = null;
        for (var ta : ctx.termo()) {
            TipoLA aux = verificarTipo(tabela, ta);
            ret = combinarAritmetico(ret, aux);
        }
        return ret == null ? TipoLA.INVALIDO : ret;
    }

    public static TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.TermoContext ctx) {
        TipoLA ret = null;
        for (var fa : ctx.fator()) {
            TipoLA aux = verificarTipo(tabela, fa);
            ret = combinarAritmetico(ret, aux);
        }
        return ret == null ? TipoLA.INVALIDO : ret;
    }

    public static TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.FatorContext ctx) {
        TipoLA ret = null;
        for (var parcela : ctx.parcela()) {
            TipoLA aux = verificarTipo(tabela, parcela);
            ret = combinarAritmetico(ret, aux);
        }
        return ret == null ? TipoLA.INVALIDO : ret;
    }

    /**
     * Regra de combinação para operadores aritméticos
     *   - dois numéricos -> real se algum for real, inteiro caso contrário
     *   - dois tipos iguais (não numéricosx'x') -> mantém o tipo
     *   - qualquer outra combinação -> INVALIDO
     */
    private static TipoLA combinarAritmetico(TipoLA ret, TipoLA aux) {
        if (ret == null) return aux;
        if (ret == aux) return ret;

        boolean retNum = (ret == TipoLA.INTEIRO || ret == TipoLA.REAL);
        boolean auxNum = (aux == TipoLA.INTEIRO || aux == TipoLA.REAL);
        if (retNum && auxNum) {
            return (ret == TipoLA.REAL || aux == TipoLA.REAL) ? TipoLA.REAL : TipoLA.INTEIRO;
        }
        return TipoLA.INVALIDO;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.ParcelaContext ctx) {
        if (ctx.parcela_nao_unario() != null) {
            return verificarTipo(tabela, ctx.parcela_nao_unario());
        }
        return verificarTipo(tabela, ctx.parcela_unario());
    }

    public static TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) {
            return TipoLA.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return TipoLA.REAL;
        }

        // Chamada de função na expressão (IDENT '(' expressao* ')')
        if (ctx.IDENT() != null && ctx.identificador() == null && temParenteses(ctx)) {
            String nome = ctx.IDENT().getText();
            EntradaTabelaDeSimbolos e = tabela.obterEntrada(nome);
            if (e == null) {
                adicionarErroSemantico(ctx.IDENT().getSymbol(),
                        "identificador " + nome + " nao declarado");
                return TipoLA.INVALIDO;
            }
            // Verifica compatibilidade de parâmetros
            verificarChamada(tabela, e, ctx.expressao(), ctx.IDENT().getSymbol());
            // Retorna o tipo de retorno da função (se for função)
            if (e.categoria == TabelaDeSimbolos.Categoria.FUNCAO) {
                return e.tipoRetorno;
            }
            // Se foi usado um procedimento como expressão, considera INVALIDO
            return TipoLA.INVALIDO;
        }

        // Identificador (com possíveis campos via '.'), pode estar precedido por '^'
        if (ctx.identificador() != null) {
            return verificarTipoIdentificador(tabela, ctx.identificador(),
                    parcelaUnarioTemAcento(ctx));
        }

        // Subexpressão entre parênteses
        if (ctx.expressao() != null && !ctx.expressao().isEmpty()) {
            return verificarTipo(tabela, ctx.expressao(0));
        }

        return TipoLA.INVALIDO;
    }

    private static boolean parcelaUnarioTemAcento(LAParser.Parcela_unarioContext ctx) {
        return ctx.getChildCount() > 0 && ctx.getChild(0).getText().equals("^");
    }

    private static boolean temParenteses(LAParser.Parcela_unarioContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if ("(".equals(ctx.getChild(i).getText())) return true;
        }
        return false;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return TabelaDeSimbolos.TipoLA.LITERAL;
        }
        // '&' identificador -> retorna ENDERECO. Antes verifica se o ident existe
        if (ctx.identificador() != null) {
            String nome = ctx.identificador().IDENT(0).getText();
            if (!tabela.existe(nome)) {
                adicionarErroSemantico(ctx.identificador().start,"identificador " + nome + " nao declarado");
            }
            return TipoLA.ENDERECO;
        }
        return TipoLA.INVALIDO;
    }

    public static TipoLA verificarTipoIdentificador(TabelaDeSimbolos tabela, LAParser.IdentificadorContext ctx, boolean comDesreferencia) {
        StringBuilder caminho = new StringBuilder();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            if (i > 0) caminho.append('.');
            caminho.append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao() != null) {
            caminho.append(ctx.dimensao().getText());
        }

        String nomeRaiz = ctx.IDENT(0).getText();
        EntradaTabelaDeSimbolos e = tabela.obterEntrada(nomeRaiz);
        if (e == null) {
            adicionarErroSemantico(ctx.start,"identificador " + caminho.toString() + " nao declarado");
            return TipoLA.INVALIDO;
        }

        // Acessos a campos (se houver mais IDENTs)
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            String campo = ctx.IDENT(i).getText();
            if (e.tipo != TipoLA.REGISTRO || !e.campos.containsKey(campo)) {
                adicionarErroSemantico(ctx.IDENT(i).getSymbol(),"identificador " + caminho.toString() + " nao declarado");
                return TipoLA.INVALIDO;
            }
            e = e.campos.get(campo);
        }

        // Se houve desreferência (^), retorna o tipo apontado
        if (comDesreferencia) {
            if (e.tipo == TipoLA.PONTEIRO) {
                return e.tipoApontado;
            }
            return e.tipo;
        }
        return e.tipo;
    }

    public static void verificarChamada(TabelaDeSimbolos tabela, EntradaTabelaDeSimbolos entrada, List<LAParser.ExpressaoContext> argumentos, Token token) {
        if (entrada.categoria != TabelaDeSimbolos.Categoria.FUNCAO && entrada.categoria != TabelaDeSimbolos.Categoria.PROCEDIMENTO) {
            return;
        }

        boolean incompativel = false;
        if (argumentos.size() != entrada.tiposParam.size()) {
            incompativel = true;
        } else {
            for (int i = 0; i < argumentos.size(); i++) {
                TipoLA tipoFormal = entrada.tiposParam.get(i);
                TipoLA tipoAtual = verificarTipo(tabela, argumentos.get(i));

                if (!tiposCompativeisParametro(tipoFormal, tipoAtual, null, null)) {
                    incompativel = true;
                    break;
                }
            }
        }
        if (incompativel) {
            adicionarErroSemantico(token,"incompatibilidade de parametros na chamada de " + entrada.nome);
        }
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
