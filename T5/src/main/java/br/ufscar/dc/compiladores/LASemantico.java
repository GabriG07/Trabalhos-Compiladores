package br.ufscar.dc.compiladores;

import br.ufscar.dc.compiladores.TabelaDeSimbolos.TipoLA;
import org.antlr.v4.runtime.Token;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.Categoria;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.EntradaTabelaDeSimbolos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LASemantico extends LABaseVisitor<Void> {

    TabelaDeSimbolos tabela;

    private int dentroDeFuncao = 0;//representa a profundidade da pilha de funções. Quando > 0, estamos dentro de uma função

    // Programa: inicializa a tabela global
    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }


    // Declaração local: declare, constante, tipo
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        String palavra = ctx.getChild(0).getText();

        if ("declare".equals(palavra)) {
            // 'declare' variavel
            tratarVariavel(ctx.variavel());
        } else if ("constante".equals(palavra)) {
            // 'constante' IDENT ':' tipo_basico '=' valor_constante
            String nome = ctx.IDENT().getText();
            TipoLA tipo = resolverTipoBasico(ctx.tipo_basico().getText());
            if (tabela.existeNoEscopoAtual(nome)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                        "identificador " + nome + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nome, tipo, Categoria.CONSTANTE);
            }
        } else if ("tipo".equals(palavra)) {
            // 'tipo' IDENT ':' tipo
            String nome = ctx.IDENT().getText();
            if (tabela.existeNoEscopoAtual(nome)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),
                        "identificador " + nome + " ja declarado anteriormente");
            } else {
                EntradaTabelaDeSimbolos novo;
                if (ctx.tipo().registro() != null) {
                    // Registro nomeado
                    novo = new EntradaTabelaDeSimbolos(nome, TipoLA.REGISTRO, Categoria.TIPO);
                    novo.nomeTipo = nome;
                    novo.campos = coletarCamposDoRegistro(ctx.tipo().registro());
                } else {
                    TipoLA tipoBase = resolverTipoEstendido(ctx.tipo().tipo_estendido(), false);
                    novo = new EntradaTabelaDeSimbolos(nome, tipoBase, Categoria.TIPO);
                    novo.nomeTipo = nome;
                }
                tabela.adicionar(novo);
            }
        }
        return null;
    }

    //Cria uma entrada na tabela desímbolos para cada identificador
    private void tratarVariavel(LAParser.VariavelContext var) {
        if (var == null) return;

        LAParser.TipoContext tipoCtx = var.tipo();

        TabelaDeSimbolos.EntradaTabelaDeSimbolos esqueleto = construirEntradaDoTipo(tipoCtx);

        for (LAParser.IdentificadorContext ident : var.identificador()) {
            String nome = ident.IDENT(0).getText();
            if (tabela.existeNoEscopoAtual(nome)) {
                LASemanticoUtils.adicionarErroSemantico(ident.start,"identificador " + nome + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.EntradaTabelaDeSimbolos copia = clonarEntradaComNome(esqueleto, nome);
                tabela.adicionar(copia);
            }
        }
    }

    // Constroi o formato da entrada com base no tipo declarado
    private EntradaTabelaDeSimbolos construirEntradaDoTipo(LAParser.TipoContext tipoCtx) {
        EntradaTabelaDeSimbolos e = new EntradaTabelaDeSimbolos("", TipoLA.INVALIDO, Categoria.VARIAVEL);

        if (tipoCtx.registro() != null) {
            e.tipo = TipoLA.REGISTRO;
            e.nomeTipo = null;
            e.campos = coletarCamposDoRegistro(tipoCtx.registro());
            return e;
        }

        // tipo_estendido = '^'? tipo_basico_ident
        LAParser.Tipo_estendidoContext ext = tipoCtx.tipo_estendido();
        boolean ehPonteiro = ext.getChildCount() > 0 && ext.getChild(0).getText().equals("^");
        String strTipo = ext.tipo_basico_ident().getText();

        if (ehPonteiro) {
            e.tipo = TipoLA.PONTEIRO;
            // Define o tipo apontado
            e.tipoApontado = resolverNomeTipoComoTipo(strTipo, ext.tipo_basico_ident().start);
            // Se for ponteiro para registro nomeado, guarda o nome do tipo
            EntradaTabelaDeSimbolos referenciado = tabela.obterEntrada(strTipo);
            if (referenciado != null && referenciado.tipo == TipoLA.REGISTRO) {
                e.nomeTipoApontado = referenciado.nomeTipo;
            }
            return e;
        }

        // Tipo não ponteiro
        if (ext.tipo_basico_ident().tipo_basico() != null) {
            e.tipo = resolverTipoBasico(strTipo);
        } else {
            // É um IDENT, deve estar declarado como tipo
            EntradaTabelaDeSimbolos referenciado = tabela.obterEntrada(strTipo);
            if (referenciado == null || referenciado.categoria != Categoria.TIPO) {
                LASemanticoUtils.adicionarErroSemantico(ext.tipo_basico_ident().start,"tipo " + strTipo + " nao declarado");
                e.tipo = TipoLA.INVALIDO;
            } else {
                e.tipo = referenciado.tipo;
                e.nomeTipo = referenciado.nomeTipo;
                // Copia campos se for registro nomeado
                if (referenciado.tipo == TipoLA.REGISTRO) {
                    e.campos = clonarCampos(referenciado.campos);
                }
            }
        }
        return e;
    }

    private EntradaTabelaDeSimbolos clonarEntradaComNome(EntradaTabelaDeSimbolos modelo, String nome) {
        EntradaTabelaDeSimbolos c = new EntradaTabelaDeSimbolos(nome, modelo.tipo, modelo.categoria);
        c.nomeTipo = modelo.nomeTipo;
        c.tipoApontado = modelo.tipoApontado;
        c.nomeTipoApontado = modelo.nomeTipoApontado;
        c.campos = clonarCampos(modelo.campos);
        return c;
    }

    private Map<String, EntradaTabelaDeSimbolos> clonarCampos(Map<String, EntradaTabelaDeSimbolos> origem) {
        Map<String, EntradaTabelaDeSimbolos> dest = new LinkedHashMap<>();
        for (Map.Entry<String, EntradaTabelaDeSimbolos> en : origem.entrySet()) {
            dest.put(en.getKey(), clonarEntradaComNome(en.getValue(), en.getValue().nome));
        }
        return dest;
    }

    //Coloca as variáveis declaradas dentro de um registro em um mapa de campos. Não cria entradas na tabela principal
    private Map<String, EntradaTabelaDeSimbolos> coletarCamposDoRegistro(LAParser.RegistroContext reg) {
        Map<String, EntradaTabelaDeSimbolos> campos = new LinkedHashMap<>();
        for (LAParser.VariavelContext var : reg.variavel()) {
            EntradaTabelaDeSimbolos esqueleto = construirEntradaDoTipo(var.tipo());
            for (LAParser.IdentificadorContext ident : var.identificador()) {
                String nome = ident.IDENT(0).getText();
                if (campos.containsKey(nome)) {
                    LASemanticoUtils.adicionarErroSemantico(ident.start,"identificador " + nome + " ja declarado anteriormente");
                } else {
                    campos.put(nome, clonarEntradaComNome(esqueleto, nome));
                }
            }
        }
        return campos;
    }

    //Converte uma string com o nome do tipo para um TipoLA
    private TipoLA resolverNomeTipoComoTipo(String strTipo, Token tokenTipo) {
        switch (strTipo) {
            case "inteiro": return TipoLA.INTEIRO;
            case "real":    return TipoLA.REAL;
            case "literal": return TipoLA.LITERAL;
            case "logico":  return TipoLA.LOGICO;
            default:
                EntradaTabelaDeSimbolos e = tabela.obterEntrada(strTipo);
                if (e == null || e.categoria != Categoria.TIPO) {
                    LASemanticoUtils.adicionarErroSemantico(tokenTipo, "tipo " + strTipo + " nao declarado");
                    return TipoLA.INVALIDO;
                }
                return e.tipo;
        }
    }

    // Declaração global: procedimento e função
    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        boolean ehFuncao = ctx.tipo_estendido() != null;

        EntradaTabelaDeSimbolos entrada;
        if (tabela.existeNoEscopoAtual(nome)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),"identificador " + nome + " ja declarado anteriormente");

            entrada = new EntradaTabelaDeSimbolos(nome, TipoLA.INVALIDO, ehFuncao ? Categoria.FUNCAO : Categoria.PROCEDIMENTO);
        } else {
            entrada = new EntradaTabelaDeSimbolos(nome,
                    ehFuncao ? TipoLA.INVALIDO : TipoLA.INVALIDO,
                    ehFuncao ? Categoria.FUNCAO : Categoria.PROCEDIMENTO);
            if (ehFuncao) {
                entrada.tipoRetorno = resolverTipoEstendido(ctx.tipo_estendido(), true);
            }
            tabela.adicionar(entrada);
        }

        List<TipoLA> tiposParam = new ArrayList<>();
        List<String> nomesParam = new ArrayList<>();
        if (ctx.parametros() != null) {
            for (var param : ctx.parametros().parametro()) {
                TipoLA tipoParam = resolverTipoEstendido(param.tipo_estendido(), true);
                for (var ident : param.identificador()) {
                    String nomeParam = ident.IDENT(0).getText();
                    tiposParam.add(tipoParam);
                    nomesParam.add(nomeParam);
                }
            }
        }
        entrada.tiposParam = tiposParam;
        entrada.nomesParam = nomesParam;

        // Abre novo escopo, registra parâmetros como variáveis locais
        tabela.criarNovoEscopo();
        if (ctx.parametros() != null) {
            for (var param : ctx.parametros().parametro()) {
                EntradaTabelaDeSimbolos esqueletoParam = construirEntradaDoTipoEstendido(param.tipo_estendido());
                for (var ident : param.identificador()) {
                    String nomeParam = ident.IDENT(0).getText();
                    if (tabela.existeNoEscopoAtual(nomeParam)) {
                        LASemanticoUtils.adicionarErroSemantico(ident.start,
                                "identificador " + nomeParam + " ja declarado anteriormente");
                    } else {
                        tabela.adicionar(clonarEntradaComNome(esqueletoParam, nomeParam));
                    }
                }
            }
        }

        // Marca que estamos dentro de função( se for o estivermos)
        if (ehFuncao) dentroDeFuncao++;

        // Visita declarações locais e comandos dentro do escopo da função ou procedimento
        for (var decl : ctx.declaracao_local()) visit(decl);
        for (var cmd : ctx.cmd()) visit(cmd);

        if (ehFuncao) dentroDeFuncao--;

        tabela.abandonarEscopo();
        return null;
    }

    private EntradaTabelaDeSimbolos construirEntradaDoTipoEstendido(LAParser.Tipo_estendidoContext ext) {
        EntradaTabelaDeSimbolos e = new EntradaTabelaDeSimbolos("", TipoLA.INVALIDO, Categoria.VARIAVEL);
        boolean ehPonteiro = ext.getChildCount() > 0 && ext.getChild(0).getText().equals("^");
        String strTipo = ext.tipo_basico_ident().getText();
        if (ehPonteiro) {
            e.tipo = TipoLA.PONTEIRO;
            e.tipoApontado = resolverNomeTipoComoTipo(strTipo, ext.tipo_basico_ident().start);
            EntradaTabelaDeSimbolos ref = tabela.obterEntrada(strTipo);
            if (ref != null && ref.tipo == TipoLA.REGISTRO) e.nomeTipoApontado = ref.nomeTipo;
            return e;
        }
        if (ext.tipo_basico_ident().tipo_basico() != null) {
            e.tipo = resolverTipoBasico(strTipo);
        } else {
            EntradaTabelaDeSimbolos ref = tabela.obterEntrada(strTipo);
            if (ref == null || ref.categoria != Categoria.TIPO) {
                // erro já reportado em resolverTipoEstendido
                e.tipo = TipoLA.INVALIDO;
            } else {
                e.tipo = ref.tipo;
                e.nomeTipo = ref.nomeTipo;
                if (ref.tipo == TipoLA.REGISTRO) e.campos = clonarCampos(ref.campos);
            }
        }
        return e;
    }

    // Atribuição: verifica se var existe e se tipos estão certos
    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        boolean comDesreferencia = ctx.getChild(0).getText().equals("^");
        LAParser.IdentificadorContext idCtx = ctx.identificador();

        // Resolve a entrada-alvo
        EntradaTabelaDeSimbolos alvo = resolverIdentificador(idCtx);
        String nomeCompleto = construirCaminho(idCtx);

        TipoLA tipoEsq;
        String nomeTipoEsq = null;
        if (alvo == null) {
            // Não declarado
            tipoEsq = TipoLA.INVALIDO;
        } else {
            if (comDesreferencia) {
                if (alvo.tipo == TipoLA.PONTEIRO) {
                    tipoEsq = alvo.tipoApontado;
                    nomeTipoEsq = alvo.nomeTipoApontado;
                } else {
                    tipoEsq = alvo.tipo;
                    nomeTipoEsq = alvo.nomeTipo;
                }
            } else {
                tipoEsq = alvo.tipo;
                nomeTipoEsq = alvo.nomeTipo;
            }
        }

        TipoLA tipoDir = LASemanticoUtils.verificarTipo(tabela, ctx.expressao());

        if (alvo != null && !LASemanticoUtils.tiposCompativeisAtribuicao(
                tipoEsq, tipoDir, nomeTipoEsq, null)) {
            // Para o caminho-alvo, usamos o nome completo (ex: 'pessoa.nome') ou prefixamos com '^' se for desreferência.
            String alvoMsg = comDesreferencia ? "^" + nomeCompleto : nomeCompleto;
            LASemanticoUtils.adicionarErroSemantico(idCtx.start,"atribuicao nao compativel para " + alvoMsg);
        }
        return null;
    }

    // Leia: verifica se cada identificador foi declarado
    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            resolverIdentificador(ident);
        }
        return null;
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (var expressao : ctx.expressao()) {
            LASemanticoUtils.verificarTipo(tabela, expressao);
        }
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = tabela.obterEntrada(nome);
        if (e == null) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),"identificador " + nome + " nao declarado");
        } else if (e.categoria == Categoria.PROCEDIMENTO || e.categoria == Categoria.FUNCAO) {
            LASemanticoUtils.verificarChamada(tabela, e, ctx.expressao(), ctx.IDENT().getSymbol());
        } else {
            // Identificador existe mas não é função/procedimento
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),"identificador " + nome + " nao declarado");
        }
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        if (dentroDeFuncao == 0) {
            LASemanticoUtils.adicionarErroSemantico(ctx.start,"comando retorne nao permitido nesse escopo");
        }
        // verifica a expressão, para encontrar identificadores indefinidos
        if (ctx.expressao() != null) {
            LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        }
        return null;
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
        String contador = ctx.IDENT().getText();
        if (!tabela.existe(contador)) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(),"identificador " + contador + " nao declarado");
        }
        for (var expArit : ctx.exp_aritmetica()) {
            LASemanticoUtils.verificarTipo(tabela, expArit);
        }
        return super.visitCmdPara(ctx);
    }

    private EntradaTabelaDeSimbolos resolverIdentificador(LAParser.IdentificadorContext ctx) {
        String caminho = construirCaminho(ctx);
        String nomeRaiz = ctx.IDENT(0).getText();
        EntradaTabelaDeSimbolos e = tabela.obterEntrada(nomeRaiz);
        if (e == null) {
            LASemanticoUtils.adicionarErroSemantico(ctx.start,
                    "identificador " + caminho + " nao declarado");
            return null;
        }
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            String campo = ctx.IDENT(i).getText();
            if (e.tipo != TipoLA.REGISTRO || !e.campos.containsKey(campo)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.IDENT(i).getSymbol(),
                        "identificador " + caminho + " nao declarado");
                return null;
            }
            e = e.campos.get(campo);
        }
        return e;
    }

    //Constrói o caminho de um identificador separado por '.' (Ex: 'x.y.z'). Inclui parte de dimensão entre colchetes se houver (Ex: valor[0])
    private String construirCaminho(LAParser.IdentificadorContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            if (i > 0) sb.append('.');
            sb.append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao() != null) {
            sb.append(ctx.dimensao().getText());
        }
        return sb.toString();
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

    private TipoLA resolverTipoEstendido(LAParser.Tipo_estendidoContext ctx, boolean reportarErro) {
        boolean ehPonteiro = ctx.getChildCount() > 0 && ctx.getChild(0).getText().equals("^");
        if (ehPonteiro) return TipoLA.PONTEIRO;

        if (ctx.tipo_basico_ident().tipo_basico() != null) {
            return resolverTipoBasico(ctx.tipo_basico_ident().tipo_basico().getText());
        }
        String nomeT = ctx.tipo_basico_ident().IDENT().getText();
        EntradaTabelaDeSimbolos e = tabela.obterEntrada(nomeT);
        if (e == null || e.categoria != Categoria.TIPO) {
            if (reportarErro) {
                LASemanticoUtils.adicionarErroSemantico(ctx.start,"tipo " + nomeT + " nao declarado");
            }
            return TipoLA.INVALIDO;
        }
        return e.tipo;
    }

    private boolean tiposCompativeis(TipoLA esquerdo, TipoLA direito) {
        if (esquerdo == direito) return true;
        if ((esquerdo == TipoLA.INTEIRO || esquerdo == TipoLA.REAL) &&
                (direito  == TipoLA.INTEIRO || direito  == TipoLA.REAL)) return true;
        return false;
    }
}