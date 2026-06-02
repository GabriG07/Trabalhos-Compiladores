package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.List;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.Categoria;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.EntradaTabelaDeSimbolos;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.TipoLA;

public class LAGeradorC extends LABaseVisitor<Void> {

    final StringBuilder saida = new StringBuilder();
    private final TabelaDeSimbolos tabela = new TabelaDeSimbolos();


    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        linha("#include <stdio.h>");
        linha("#include <stdlib.h>");
        linha("#include <string.h>");
        linha("");

        for (LAParser.Decl_local_globalContext declaracao : ctx.declaracoes().decl_local_global()) {
            if (declaracao.declaracao_local() != null) {
                gerarDeclaracaoLocal(declaracao.declaracao_local());
            } else {
                gerarDeclaracaoGlobal(declaracao.declaracao_global());
            }
        }

        linha("int main() {");
        tabela.criarNovoEscopo();
        for (LAParser.Declaracao_localContext declaracao : ctx.corpo().declaracao_local()) {
            gerarDeclaracaoLocal(declaracao);
        }
        for (LAParser.CmdContext comando : ctx.corpo().cmd()) {
            visit(comando);
        }
        linha("return 0;");
        tabela.abandonarEscopo();
        linha("}");
        return null;
    }

    private void gerarDeclaracaoGlobal(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        boolean procedimento = ctx.getChild(0).getText().equals("procedimento");
        EntradaTabelaDeSimbolos retorno = procedimento ? novaEntrada("", TipoLA.INVALIDO, Categoria.PROCEDIMENTO) : resolverTipoEstendido(ctx.tipo_estendido());
        EntradaTabelaDeSimbolos simbolo = novaEntrada(nome, TipoLA.INVALIDO, procedimento ? Categoria.PROCEDIMENTO : Categoria.FUNCAO);
        if (!procedimento) {
            simbolo.tipoRetorno = retorno.tipo;
            simbolo.nomeTipo = retorno.nomeTipo;
            simbolo.tipoApontado = retorno.tipoApontado;
            simbolo.nomeTipoApontado = retorno.nomeTipoApontado;
        }
        tabela.adicionar(simbolo);

        String tipoRetorno = procedimento ? "void" : nomeTipoC(retorno, false);
        StringBuilder cabecalho = new StringBuilder(tipoRetorno).append(" ").append(nome).append("(");
        List<String> parametros = new ArrayList<>();
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext parametro : ctx.parametros().parametro()) {
                EntradaTabelaDeSimbolos tipoParam = resolverTipoEstendido(parametro.tipo_estendido());
                for (LAParser.IdentificadorContext identificador : parametro.identificador()) {
                    String nomeParam = identificador.IDENT(0).getText();
                    parametros.add(declaracaoSimples(nomeParam, identificador.dimensao().getText(), tipoParam, true));
                }
            }
        }
        cabecalho.append(String.join(", ", parametros)).append(") {");
        linha(cabecalho.toString());

        tabela.criarNovoEscopo();
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext parametro : ctx.parametros().parametro()) {
                EntradaTabelaDeSimbolos tipoParam = resolverTipoEstendido(parametro.tipo_estendido());
                for (LAParser.IdentificadorContext identificador : parametro.identificador()) {
                    tabela.adicionar(clonarEntradaComNome(tipoParam, identificador.IDENT(0).getText()));
                }
            }
        }
        for (LAParser.Declaracao_localContext declaracao : ctx.declaracao_local()) {
            gerarDeclaracaoLocal(declaracao);
        }
        for (LAParser.CmdContext comando : ctx.cmd()) {
            visit(comando);
        }
        tabela.abandonarEscopo();
        linha("}");
        linha("");
    }

    private void gerarDeclaracaoLocal(LAParser.Declaracao_localContext ctx) {
        String palavra = ctx.getChild(0).getText();
        if (palavra.equals("constante")) {
            EntradaTabelaDeSimbolos tipo = resolverTipoBasico(ctx.tipo_basico().getText());
            tabela.adicionar(clonarEntradaComNome(tipo, ctx.IDENT().getText()));
            linha("#define " + ctx.IDENT().getText() + " " + ctx.valor_constante().getText());
            return;
        }
        if (palavra.equals("tipo")) {
            EntradaTabelaDeSimbolos tipo = resolverTipo(ctx.tipo());
            tipo.nome = ctx.IDENT().getText();
            tipo.nomeTipo = ctx.IDENT().getText();
            tipo.categoria = Categoria.TIPO;
            tabela.adicionar(tipo);
            if (ctx.tipo().registro() != null) {
                linha("typedef struct {");
                gerarCamposRegistro(ctx.tipo().registro(), false);
                linha("} " + ctx.IDENT().getText() + ";");
            } else {
                linha("typedef " + nomeTipoC(tipo, false) + " " + ctx.IDENT().getText() + ";");
            }
            return;
        }

        EntradaTabelaDeSimbolos tipo = resolverTipo(ctx.variavel().tipo());
        for (LAParser.IdentificadorContext identificador : ctx.variavel().identificador()) {
            String nome = identificador.IDENT(0).getText();
            String dimensao = gerarDimensoes(identificador.dimensao());
            tabela.adicionar(clonarEntradaComNome(tipo, nome));
            if (tipo.tipo == TipoLA.REGISTRO && tipo.categoria != Categoria.TIPO && tipo.nomeTipo == null) {
                linha("struct {");
                gerarCamposRegistro(ctx.variavel().tipo().registro(), false);
                linha("} " + nome + dimensao + ";");
            } else {
                linha(declaracaoSimples(nome, dimensao, tipo, false) + ";");
            }
        }
    }

    private void gerarCamposRegistro(LAParser.RegistroContext registro, boolean parametro) {
        for (LAParser.VariavelContext variavel : registro.variavel()) {
            EntradaTabelaDeSimbolos tipoCampo = resolverTipo(variavel.tipo());
            for (LAParser.IdentificadorContext identificador : variavel.identificador()) {
                String nome = identificador.IDENT(0).getText();
                String dimensao = gerarDimensoes(identificador.dimensao());
                linha(declaracaoSimples(nome, dimensao, tipoCampo, parametro) + ";");
            }
        }
    }

    private String declaracaoSimples(String nome, String dimensao, EntradaTabelaDeSimbolos tipo, boolean parametro) {
        if (tipo.tipo == TipoLA.LITERAL) {
            return parametro ? "char* " + nome : "char " + nome + dimensao + "[80]";
        }
        return nomeTipoC(tipo, false) + " " + nome + dimensao;
    }

    private EntradaTabelaDeSimbolos resolverTipo(LAParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            EntradaTabelaDeSimbolos registro = novaEntrada("", TipoLA.REGISTRO, Categoria.VARIAVEL);
            for (LAParser.VariavelContext variavel : ctx.registro().variavel()) {
                EntradaTabelaDeSimbolos tipoCampo = resolverTipo(variavel.tipo());
                for (LAParser.IdentificadorContext identificador : variavel.identificador()) {
                    registro.campos.put(identificador.IDENT(0).getText(),
                            clonarEntradaComNome(tipoCampo, identificador.IDENT(0).getText()));
                }
            }
            return registro;
        }
        return resolverTipoEstendido(ctx.tipo_estendido());
    }

    private EntradaTabelaDeSimbolos resolverTipoEstendido(LAParser.Tipo_estendidoContext ctx) {
        String texto = ctx.getText();
        if (texto.startsWith("^")) {
            EntradaTabelaDeSimbolos ponteiro = novaEntrada("", TipoLA.PONTEIRO, Categoria.VARIAVEL);
            EntradaTabelaDeSimbolos apontado = resolverTipoNome(texto.substring(1));
            ponteiro.tipoApontado = apontado.tipo;
            ponteiro.nomeTipoApontado = apontado.nomeTipo;
            ponteiro.campos = clonarCampos(apontado.campos);
            return ponteiro;
        }
        return resolverTipoNome(texto);
    }

    private EntradaTabelaDeSimbolos resolverTipoNome(String nome) {
        EntradaTabelaDeSimbolos basico = resolverTipoBasico(nome);
        if (basico.tipo != TipoLA.INVALIDO) {
            return basico;
        }
        EntradaTabelaDeSimbolos definido = tabela.obterEntrada(nome);
        if (definido != null && definido.categoria == Categoria.TIPO) {
            EntradaTabelaDeSimbolos copia = clonarEntradaComNome(definido, "");
            copia.nomeTipo = nome;
            copia.categoria = Categoria.VARIAVEL;
            return copia;
        }
        return basico;
    }

    private EntradaTabelaDeSimbolos resolverTipoBasico(String nome) {
        return switch (nome) {
            case "inteiro" -> novaEntrada("", TipoLA.INTEIRO, Categoria.VARIAVEL);
            case "real" -> novaEntrada("", TipoLA.REAL, Categoria.VARIAVEL);
            case "literal" -> novaEntrada("", TipoLA.LITERAL, Categoria.VARIAVEL);
            case "logico" -> novaEntrada("", TipoLA.LOGICO, Categoria.VARIAVEL);
            default -> novaEntrada("", TipoLA.INVALIDO, Categoria.VARIAVEL);
        };
    }

    private EntradaTabelaDeSimbolos novaEntrada(String nome, TipoLA tipo, Categoria categoria) {
        return new EntradaTabelaDeSimbolos(nome, tipo, categoria);
    }

    private EntradaTabelaDeSimbolos clonarEntradaComNome(EntradaTabelaDeSimbolos origem, String nome) {
        EntradaTabelaDeSimbolos copia = novaEntrada(nome, origem.tipo, origem.categoria);
        copia.nomeTipo = origem.nomeTipo;
        copia.tipoApontado = origem.tipoApontado;
        copia.nomeTipoApontado = origem.nomeTipoApontado;
        copia.tipoRetorno = origem.tipoRetorno;
        copia.tiposParam = new ArrayList<>(origem.tiposParam);
        copia.nomesParam = new ArrayList<>(origem.nomesParam);
        copia.campos = clonarCampos(origem.campos);
        return copia;
    }

    private java.util.Map<String, EntradaTabelaDeSimbolos> clonarCampos(
            java.util.Map<String, EntradaTabelaDeSimbolos> campos) {
        java.util.Map<String, EntradaTabelaDeSimbolos> copia = new java.util.LinkedHashMap<>();
        campos.forEach((nome, campo) -> copia.put(nome, clonarEntradaComNome(campo, nome)));
        return copia;
    }

    private String nomeTipoC(EntradaTabelaDeSimbolos tipo, boolean parametro) {
        if (tipo.nomeTipo != null && tipo.tipo == TipoLA.REGISTRO) {
            return tipo.nomeTipo;
        }
        return switch (tipo.tipo) {
            case INTEIRO -> "int";
            case REAL -> "float";
            case LITERAL -> parametro ? "char*" : "char";
            case LOGICO -> "int";
            case PONTEIRO -> nomeTipoApontadoC(tipo) + "*";
            default -> "int";
        };
    }

    private String nomeTipoApontadoC(EntradaTabelaDeSimbolos ponteiro) {
        if (ponteiro.nomeTipoApontado != null) {
            return ponteiro.nomeTipoApontado;
        }
        return switch (ponteiro.tipoApontado) {
            case INTEIRO -> "int";
            case REAL -> "float";
            case LITERAL -> "char";
            case LOGICO -> "int";
            default -> "int";
        };
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (int i = 0; i < ctx.identificador().size(); i++) {
            LAParser.IdentificadorContext id = ctx.identificador(i);
            boolean desreferencia = ctx.getChild(2 * i + 2).getText().equals("^");
            String destino = (desreferencia ? "*" : "") + gerarIdentificador(id);
            TipoLA tipo = LASemanticoUtils.verificarTipoIdentificador(tabela, id, desreferencia);
            switch (tipo) {
                case INTEIRO -> linha("scanf(\"%d\", &" + destino + ");");
                case REAL -> linha("scanf(\"%f\", &" + destino + ");");
                case LITERAL -> linha("scanf(\" %79[^\\n]\", " + destino + ");");
                default -> linha("/* leitura de tipo nao suportado */");
            }
        }
        return null;
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (LAParser.ExpressaoContext expressao : ctx.expressao()) {
            TipoLA tipo = LASemanticoUtils.verificarTipo(tabela, expressao);
            String formato = switch (tipo) {
                case REAL -> "%f";
                case LITERAL -> "%s";
                default -> "%d";
            };
            linha("printf(\"" + formato + "\", " + gerarExpressao(expressao) + ");");
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        boolean desreferencia = ctx.getChild(0).getText().equals("^");
        String esquerda = (desreferencia ? "*" : "") + gerarIdentificador(ctx.identificador());
        TipoLA tipoDestino = LASemanticoUtils.verificarTipoIdentificador(tabela, ctx.identificador(), desreferencia);
        if (tipoDestino == TipoLA.LITERAL) {
            linha("strcpy(" + esquerda + ", " + gerarExpressao(ctx.expressao()) + ");");
        } else {
            linha(esquerda + " = " + gerarExpressao(ctx.expressao()) + ";");
        }
        return null;
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        linha("if (" + gerarExpressao(ctx.expressao()) + ") {");
        int limiteEntao = possuiSenao(ctx) ? indicePrimeiroComandoSenao(ctx) : ctx.cmd().size();
        for (int i = 0; i < limiteEntao; i++) visit(ctx.cmd(i));
        linha("}");
        if (limiteEntao < ctx.cmd().size()) {
            linha("else {");
            for (int i = limiteEntao; i < ctx.cmd().size(); i++) visit(ctx.cmd(i));
            linha("}");
        }
        return null;
    }

    private boolean possuiSenao(LAParser.CmdSeContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i).getText().equals("senao")) return true;
        }
        return false;
    }

    //Localiza quantos comandos pertencem ao bloco entao pela posição do token senao.
    private int indicePrimeiroComandoSenao(LAParser.CmdSeContext ctx) {
        int posSenao = -1;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i).getText().equals("senao")) {
                posSenao = ctx.getChild(i).getSourceInterval().a;
                break;
            }
        }
        int antes = 0;
        for (LAParser.CmdContext comando : ctx.cmd()) {
            if (comando.getSourceInterval().a < posSenao) antes++;
        }
        return antes;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        linha("switch (" + gerarExpAritmetica(ctx.exp_aritmetica()) + ") {");
        for (LAParser.Item_selecaoContext item : ctx.selecao().item_selecao()) {
            for (LAParser.Numero_intervaloContext intervalo : item.constantes().numero_intervalo()) {
                int inicio = valorIntervalo(intervalo, 0);
                int fim = intervalo.NUM_INT().size() > 1 ? valorIntervalo(intervalo, 1) : inicio;
                for (int numero = inicio; numero <= fim; numero++) {
                    linha("case " + numero + ":");
                }
            }
            for (LAParser.CmdContext comando : item.cmd()) visit(comando);
            linha("break;");
        }
        if (!ctx.cmd().isEmpty()) {
            linha("default:");
            for (LAParser.CmdContext comando : ctx.cmd()) visit(comando);
        }
        linha("}");
        return null;
    }

    private int valorIntervalo(LAParser.Numero_intervaloContext ctx, int indice) {
        int valor = Integer.parseInt(ctx.NUM_INT(indice).getText());
        if (indice < ctx.op_unario().size() && ctx.op_unario(indice) != null) {
            valor = -valor;
        }
        return valor;
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        linha("for (" + ctx.IDENT().getText() + " = " + gerarExpAritmetica(ctx.exp_aritmetica(0))
                + "; " + ctx.IDENT().getText() + " <= " + gerarExpAritmetica(ctx.exp_aritmetica(1))
                + "; " + ctx.IDENT().getText() + "++) {");
        for (LAParser.CmdContext comando : ctx.cmd()) visit(comando);
        linha("}");
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        linha("while (" + gerarExpressao(ctx.expressao()) + ") {");
        for (LAParser.CmdContext comando : ctx.cmd()) visit(comando);
        linha("}");
        return null;
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        linha("do {");
        for (LAParser.CmdContext comando : ctx.cmd()) visit(comando);
        linha("} while (" + gerarExpressao(ctx.expressao()) + ");");
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        List<String> argumentos = new ArrayList<>();
        for (LAParser.ExpressaoContext expressao : ctx.expressao()) argumentos.add(gerarExpressao(expressao));
        linha(ctx.IDENT().getText() + "(" + String.join(", ", argumentos) + ");");
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        linha("return " + gerarExpressao(ctx.expressao()) + ";");
        return null;
    }

    private String gerarExpressao(LAParser.ExpressaoContext ctx) {
        List<String> termos = new ArrayList<>();
        for (LAParser.Termo_logicoContext termo : ctx.termo_logico()) termos.add(gerarTermoLogico(termo));
        return juntar(termos, ctx.op_logico_1().stream().map(op -> "||").toList());
    }

    private String gerarTermoLogico(LAParser.Termo_logicoContext ctx) {
        List<String> fatores = new ArrayList<>();
        for (LAParser.Fator_logicoContext fator : ctx.fator_logico()) fatores.add(gerarFatorLogico(fator));
        return juntar(fatores, ctx.op_logico_2().stream().map(op -> "&&").toList());
    }

    private String gerarFatorLogico(LAParser.Fator_logicoContext ctx) {
        String valor = gerarParcelaLogica(ctx.parcela_logica());
        return ctx.getChild(0).getText().equals("nao") ? "!(" + valor + ")" : valor;
    }

    private String gerarParcelaLogica(LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) return gerarExpRelacional(ctx.exp_relacional());
        return ctx.getText().equals("verdadeiro") ? "1" : "0";
    }

    private String gerarExpRelacional(LAParser.Exp_relacionalContext ctx) {
        String esquerda = gerarExpAritmetica(ctx.exp_aritmetica(0));
        if (ctx.op_relacional() == null) return esquerda;
        String operador = switch (ctx.op_relacional().getText()) {
            case "=" -> "==";
            case "<>" -> "!=";
            default -> ctx.op_relacional().getText();
        };
        return esquerda + " " + operador + " " + gerarExpAritmetica(ctx.exp_aritmetica(1));
    }

    private String gerarExpAritmetica(LAParser.Exp_aritmeticaContext ctx) {
        List<String> termos = new ArrayList<>();
        for (LAParser.TermoContext termo : ctx.termo()) termos.add(gerarTermo(termo));
        List<String> ops = ctx.op1().stream().map(op -> op.getText()).toList();
        return juntar(termos, ops);
    }

    private String gerarTermo(LAParser.TermoContext ctx) {
        List<String> fatores = new ArrayList<>();
        for (LAParser.FatorContext fator : ctx.fator()) fatores.add(gerarFator(fator));
        List<String> ops = ctx.op2().stream().map(op -> op.getText()).toList();
        return juntar(fatores, ops);
    }

    private String gerarFator(LAParser.FatorContext ctx) {
        List<String> parcelas = new ArrayList<>();
        for (LAParser.ParcelaContext parcela : ctx.parcela()) parcelas.add(gerarParcela(parcela));
        List<String> ops = ctx.op3().stream().map(op -> op.getText()).toList();
        return juntar(parcelas, ops);
    }

    private String gerarParcela(LAParser.ParcelaContext ctx) {
        if (ctx.parcela_nao_unario() != null) {
            if (ctx.parcela_nao_unario().CADEIA() != null) return ctx.parcela_nao_unario().CADEIA().getText();
            return "&" + gerarIdentificador(ctx.parcela_nao_unario().identificador());
        }
        LAParser.Parcela_unarioContext p = ctx.parcela_unario();
        String sinal = ctx.op_unario() == null ? "" : "-";
        if (p.NUM_INT() != null) return sinal + p.NUM_INT().getText();
        if (p.NUM_REAL() != null) return sinal + p.NUM_REAL().getText();
        if (p.identificador() != null) {
            boolean desreferencia = p.getText().startsWith("^");
            return sinal + (desreferencia ? "*" : "") + gerarIdentificador(p.identificador());
        }
        if (p.IDENT() != null) {
            List<String> argumentos = new ArrayList<>();
            for (LAParser.ExpressaoContext expressao : p.expressao()) argumentos.add(gerarExpressao(expressao));
            return sinal + p.IDENT().getText() + "(" + String.join(", ", argumentos) + ")";
        }
        return sinal + "(" + gerarExpressao(p.expressao(0)) + ")";
    }

    private String gerarIdentificador(LAParser.IdentificadorContext ctx) {
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            if (i > 0) id.append('.');
            id.append(ctx.IDENT(i).getText());
        }
        id.append(gerarDimensoes(ctx.dimensao()));
        return id.toString();
    }

    private String gerarDimensoes(LAParser.DimensaoContext ctx) {
        StringBuilder dimensoes = new StringBuilder();
        for (LAParser.Exp_aritmeticaContext dimensao : ctx.exp_aritmetica()) {
            dimensoes.append('[').append(gerarExpAritmetica(dimensao)).append(']');
        }
        return dimensoes.toString();
    }


    private String juntar(List<String> elementos, List<String> operadores) {
        StringBuilder resultado = new StringBuilder(elementos.get(0));
        for (int i = 1; i < elementos.size(); i++) resultado.append(' ').append(operadores.get(i - 1)).append(' ').append(elementos.get(i));
        return resultado.toString();
    }

    private void linha(String texto) {
        saida.append(texto).append('\n');
    }
}
