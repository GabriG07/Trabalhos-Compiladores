package br.ufscar.dc.compiladores;

import br.ufscar.dc.compiladores.TabelaDeSimbolos.TipoLA;
import org.antlr.v4.runtime.Token;

public class LASemantico extends LABaseVisitor<Void> {

    TabelaDeSimbolos tabela;

    // Programa: inicializa a tabela global
    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }


    // Declaração local: declare, constante, tipo
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {

        // 'declare' variavel
        if (ctx.variavel() != null) {
            LAParser.VariavelContext var = ctx.variavel();
            String strTipo = var.tipo().getText();

            // Verifica se é um tipo definido pelo usuário (IDENT em tipo_basico_ident)
            TipoLA tipoVar = resolverTipo(strTipo, var.tipo().start);

            for (var ident : var.identificador()) {
                String nome = ident.IDENT(0).getText();
                if (tabela.existe(nome)) {
                    LASemanticoUtils.adicionarErroSemantico(ident.start,
                            "identificador " + nome + " ja declarado anteriormente");
                } else {
                    tabela.adicionar(nome, tipoVar);
                }
            }
        }

        // 'constante' IDENT ':' tipo_basico '=' valor_constante
        if (ctx.IDENT() != null && ctx.tipo_basico() != null) {
            String nome = ctx.IDENT().getText();
            TipoLA tipo = resolverTipoBasico(ctx.tipo_basico().getText());
            if (tabela.existe(nome)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                        "identificador " + nome + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nome, tipo);
            }
        }

        // 'tipo' IDENT ':' tipo: registra o nome do tipo customizado
        if (ctx.getChildCount() > 0 && ctx.getChild(0).getText().equals("tipo")
                && ctx.IDENT() != null && ctx.tipo() != null) {
            String nome = ctx.IDENT().getText();
            if (tabela.existe(nome)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                        "identificador " + nome + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nome, TipoLA.TIPO_DEFINIDO);
            }
        }

        return super.visitDeclaracao_local(ctx);
    }

    // Declaração global: procedimento e função
    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();

        if (tabela.existe(nome)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                    "identificador " + nome + " ja declarado anteriormente");
        } else {
            // Função tem tipo de retorno, procedimento não tem
            if (ctx.tipo_estendido() != null) {
                TipoLA tipoRetorno = resolverTipoEstendido(ctx.tipo_estendido());
                tabela.adicionar(nome, tipoRetorno);
            } else {
                tabela.adicionar(nome, TipoLA.INVALIDO); // procedimento
            }
        }

        // Cria escopo local para os parâmetros e corpo
        tabela.criarNovoEscopo();

        if (ctx.parametros() != null) {
            for (var param : ctx.parametros().parametro()) {
                TipoLA tipoParam = resolverTipoEstendido(param.tipo_estendido());
                for (var ident : param.identificador()) {
                    String nomeParam = ident.IDENT(0).getText();
                    if (tabela.existe(nomeParam)) {
                        LASemanticoUtils.adicionarErroSemantico(ident.start,
                                "identificador " + nomeParam + " ja declarado anteriormente");
                    } else {
                        tabela.adicionar(nomeParam, tipoParam);
                    }
                }
            }
        }

        // Visita declarações locais e comandos dentro do escopo
        for (var decl : ctx.declaracao_local()) visit(decl);
        for (var cmd : ctx.cmd()) visit(cmd);

        tabela.abandonarEscopo();
        return null;
    }

    // Atribuição: verifica se var existe e se tipos estão certos
    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        String nome = ctx.identificador().IDENT(0).getText();

        if (!tabela.existe(nome)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.identificador().start,
                    "identificador " + nome + " nao declarado");
        } else {
            TipoLA tipoVar = tabela.verificar(nome);
            TipoLA tipoExp = LASemanticoUtils.verificarTipo(tabela, ctx.expressao());

            if (!tiposCompativeis(tipoVar, tipoExp)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.identificador().start,
                        "atribuicao nao compativel para " + nome);
            }
        }

        return super.visitCmdAtribuicao(ctx);
    }

    // Leia: verifica se cada identificador foi declarado
    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            String nome = ident.IDENT(0).getText();
            if (!tabela.existe(nome)) {
                LASemanticoUtils.adicionarErroSemantico(ident.start,
                        "identificador " + nome + " nao declarado");
            }
        }
        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (var expressao : ctx.expressao()) {
            LASemanticoUtils.verificarTipo(tabela, expressao);
        }
        return super.visitCmdEscreva(ctx);
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        return super.visitCmdEnquanto(ctx);
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        return super.visitCmdSe(ctx);
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        return super.visitCmdFaca(ctx);
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        for (var expArith : ctx.exp_aritmetica()) {
            LASemanticoUtils.verificarTipo(tabela, expArith);
        }
        return super.visitCmdPara(ctx);
    }

    // Resolução de tipos
    private TipoLA resolverTipo(String strTipo, Token tokenTipo) {
        return switch (strTipo) {
            case "inteiro" -> TipoLA.INTEIRO;
            case "real"    -> TipoLA.REAL;
            case "literal" -> TipoLA.LITERAL;
            case "logico"  -> TipoLA.LOGICO;
            default -> {
                if (!tabela.existe(strTipo)) {
                    LASemanticoUtils.adicionarErroSemantico(tokenTipo,
                            "tipo " + strTipo + " nao declarado");
                    yield TipoLA.INVALIDO;
                }
                yield TipoLA.TIPO_DEFINIDO;
            }
        };
    }

    private TipoLA resolverTipoBasico(String str) {
        return switch (str) {
            case "inteiro" -> TipoLA.INTEIRO;
            case "real"    -> TipoLA.REAL;
            case "literal" -> TipoLA.LITERAL;
            case "logico"  -> TipoLA.LOGICO;
            default        -> TipoLA.INVALIDO;
        };
    }

    private TipoLA resolverTipoEstendido(LAParser.Tipo_estendidoContext ctx) {
        if (ctx.tipo_basico_ident().tipo_basico() != null) {
            return resolverTipoBasico(ctx.tipo_basico_ident().tipo_basico().getText());
        }
        // IDENT — tipo customizado
        String nomeT = ctx.tipo_basico_ident().IDENT().getText();
        if (!tabela.existe(nomeT)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.start,
                    "tipo " + nomeT + " nao declarado");
            return TipoLA.INVALIDO;
        }
        return TipoLA.TIPO_DEFINIDO;
    }

    private boolean tiposCompativeis(TipoLA esquerdo, TipoLA direito) {
        if (esquerdo == direito) return true;
        if ((esquerdo == TipoLA.INTEIRO || esquerdo == TipoLA.REAL) &&
                (direito  == TipoLA.INTEIRO || direito  == TipoLA.REAL)) return true;
        return false;
    }
}