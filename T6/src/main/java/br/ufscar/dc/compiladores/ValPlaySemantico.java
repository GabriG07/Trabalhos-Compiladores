package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.tree.TerminalNode;
import br.ufscar.dc.compiladores.BancoDeAgentes.Agente;

import java.util.*;

/**
 * Análise semântica da linguagem ValPlay. Acumula TODOS os erros encontrados
 * e também produz avisos de balanceamento da composição.
 *
 * Verificações implementadas (além da gramática):
 *  1. O mapa precisa existir no banco de mapas
 *  2. A composição deve ter exatamente 5 agentes
 *  3. Agentes não podem se repetir na composição
 *  4. Cada agente da composição precisa existir no banco de agentes
 *  5. O site referenciado num execute precisa existir naquele mapa
 *  6. Todo_ agente citado numa ação precisa estar na composicao
 *  7. A habilidade usada precisa pertencer ao agente (checagem de "tipo")
 *  8. 'usa' x 'ultimate': o verbo precisa casar com a categoria da habilidade
 *  9. O número de usos de uma habilidade num execute não pode exceder as cargas
 * 10. (Aviso) Balanceamento de funções na composicao
 */
public class ValPlaySemantico extends ValPlayBaseVisitor<Void> {

    @Override
    public Void visitPlano(ValPlayParser.PlanoContext ctx) {
        // ---- mapa ----
        TerminalNode mapaTok = ctx.IDENT();
        String mapa = mapaTok.getText();
        int linhaMapa = mapaTok.getSymbol().getLine();
        if (!BancoDeAgentes.mapaExiste(mapa)) {
            ValPlaySemanticoUtils.adicionarErro(linhaMapa, "mapa " + mapa + " desconhecido");
        }
        List<String> sites = BancoDeAgentes.sitesDoMapa(mapa);

        // ---- composicao ----
        Set<String> composicao = new LinkedHashSet<>();
        Set<String> vistos = new HashSet<>();
        List<TerminalNode> ids = ctx.composicao().IDENT();
        for (TerminalNode id : ids) {
            String nome = id.getText();
            int ln = id.getSymbol().getLine();
            if (!BancoDeAgentes.agenteExiste(nome)) {
                ValPlaySemanticoUtils.adicionarErro(ln, "agente " + nome + " desconhecido");
            } else if (!vistos.add(nome)) {
                ValPlaySemanticoUtils.adicionarErro(ln, "agente " + nome + " repetido na composicao");
            } else {
                composicao.add(nome);
            }
        }
        if (ids.size() != 5) {
            int ln = ids.isEmpty() ? linhaMapa : ids.get(0).getSymbol().getLine();
            ValPlaySemanticoUtils.adicionarErro(ln,"a composicao deve ter exatamente 5 agentes (encontrados " + ids.size() + ")");
        }

        // Executes
        for (ValPlayParser.ExecuteContext ex : ctx.execute()) {
            TerminalNode siteTok = ex.IDENT();
            String site = siteTok.getText();
            if (BancoDeAgentes.mapaExiste(mapa) && !sites.contains(site)) {
                ValPlaySemanticoUtils.adicionarErro(siteTok.getSymbol().getLine(),"site " + site + " nao existe no mapa " + mapa);
            }
            // Contagem de usos por (agente, habilidade) dentro deste execute
            Map<String, Integer> usos = new HashMap<>();
            for (ValPlayParser.AcaoContext ac : ex.acao()) {
                verificarAcao(ac, composicao, usos);
            }
        }

        // Avisos de balanceamento
        if (ids.size() == 5 && composicao.size() == 5) {
            List<String> funcoes = new ArrayList<>();
            for (String n : composicao) {
                funcoes.add(BancoDeAgentes.AGENTES.get(n).funcao);
            }
            if (!funcoes.contains("Controlador")) {
                ValPlaySemanticoUtils.adicionarAviso("composicao sem Controlador: dificil bloquear visao/rotacoes");
            }
            if (!funcoes.contains("Iniciador")) {
                ValPlaySemanticoUtils.adicionarAviso("composicao sem Iniciador: pouca informacao para entrar nos sites");
            }
            if (Collections.frequency(funcoes, "Duelista") >= 4) {
                ValPlaySemanticoUtils.adicionarAviso("composicao com 4+ Duelistas: utilitario de controle insuficiente");
            }
        }
        return null;
    }

    private void verificarAcao(ValPlayParser.AcaoContext ac, Set<String> composicao, Map<String, Integer> usos) {
        if (ac instanceof ValPlayParser.AcaoUsaContext u) {
            String agente = u.IDENT().getText();
            int linha = u.IDENT().getSymbol().getLine();
            if (!composicao.contains(agente)) {
                ValPlaySemanticoUtils.adicionarErro(linha, "agente " + agente + " nao esta na composicao");
                return;
            }
            Agente a = BancoDeAgentes.AGENTES.get(agente);
            String hab = texto(u.CADEIA().get(0));
            if (hab.equals(a.ultimate)) {
                ValPlaySemanticoUtils.adicionarErro(linha,"'" + hab + "' eh a ultimate de " + agente + "; use 'ultimate' em vez de 'usa'");
            } else if (!a.habilidades.containsKey(hab)) {
                ValPlaySemanticoUtils.adicionarErro(linha,agente + " nao possui a habilidade '" + hab + "'");
            } else {
                String chave = agente + "|" + hab;
                int n = usos.merge(chave, 1, Integer::sum);
                int cargas = a.habilidades.get(hab).cargas;
                if (n > cargas) {
                    ValPlaySemanticoUtils.adicionarErro(linha,"uso de '" + hab + "' excede as cargas (" + cargas + ") de " + agente);
                }
            }
        } else if (ac instanceof ValPlayParser.AcaoUltimateContext ul) {
            String agente = ul.IDENT().getText();
            int linha = ul.IDENT().getSymbol().getLine();
            if (!composicao.contains(agente)) {
                ValPlaySemanticoUtils.adicionarErro(linha, "agente " + agente + " nao esta na composicao");
                return;
            }
            Agente a = BancoDeAgentes.AGENTES.get(agente);
            String hab = texto(ul.CADEIA().get(0));
            if (!hab.equals(a.ultimate)) {
                if (a.habilidades.containsKey(hab)) {
                    ValPlaySemanticoUtils.adicionarErro(linha,"'" + hab + "' nao eh a ultimate de " + agente + "; use 'usa'");
                } else {
                    ValPlaySemanticoUtils.adicionarErro(linha,agente + " nao possui a ultimate '" + hab + "'");
                }
            }
        } else if (ac instanceof ValPlayParser.AcaoEntraContext e) {
            verificarPresenca(e.IDENT(), composicao);
        } else if (ac instanceof ValPlayParser.AcaoPlantaContext p) {
            verificarPresenca(p.IDENT(), composicao);
        }
    }

    private void verificarPresenca(TerminalNode idTok, Set<String> composicao) {
        String agente = idTok.getText();
        int linha = idTok.getSymbol().getLine();
        if (!composicao.contains(agente)) {
            ValPlaySemanticoUtils.adicionarErro(linha, "agente " + agente + " nao esta na composicao");
        }
    }

    /** Remove as aspas de um token CADEIA. */
    private static String texto(TerminalNode t) {
        String s = t.getText();
        return (s.length() >= 2) ? s.substring(1, s.length() - 1) : s;
    }
}