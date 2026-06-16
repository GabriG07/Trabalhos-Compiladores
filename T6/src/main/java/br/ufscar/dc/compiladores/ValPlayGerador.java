package br.ufscar.dc.compiladores;

import br.ufscar.dc.compiladores.BancoDeAgentes.Agente;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Geracao de codigo: transforma um plano valido num "playbook" em HTML
 * autocontido (tema tatico escuro), com a composicao por funcao, a timeline
 * de cada execute e um esquema top-down estilizado do site.
 */
public class ValPlayGerador extends ValPlayBaseVisitor<Void> {

    /** Codigo gerado (o playbook HTML). Preenchido por visitPlano. */
    public String saida = "";

    private static final Map<String, String> COR_FUNCAO = new HashMap<>();
    private static final Map<String, String> INICIAL = new HashMap<>();
    private static final Map<String, String> COR_CAT = new HashMap<>();

    static {
        COR_FUNCAO.put("Duelista", "#ff4655");
        COR_FUNCAO.put("Iniciador", "#f5c542");
        COR_FUNCAO.put("Controlador", "#7a5cff");
        COR_FUNCAO.put("Sentinela", "#26d0a0");

        INICIAL.put("Duelista", "D");
        INICIAL.put("Iniciador", "I");
        INICIAL.put("Controlador", "C");
        INICIAL.put("Sentinela", "S");

        COR_CAT.put("smoke", "#8a8f98");
        COR_CAT.put("flash", "#ffe23d");
        COR_CAT.put("recon", "#3da5ff");
        COR_CAT.put("molly", "#ff7a3c");
        COR_CAT.put("wall", "#26d0a0");
        COR_CAT.put("dash", "#ff4655");
        COR_CAT.put("heal", "#3ddc84");
        COR_CAT.put("trap", "#c77dff");
        COR_CAT.put("dano", "#ff5470");
        COR_CAT.put("outro", "#9aa0a6");
        COR_CAT.put("ultimate", "#ffab00");
    }

    @Override
    public Void visitPlano(ValPlayParser.PlanoContext ctx) {
        String mapa = ctx.IDENT().getText();

        StringBuilder cards = new StringBuilder();
        for (TerminalNode id : ctx.composicao().IDENT()) {
            String n = id.getText();
            Agente a = BancoDeAgentes.AGENTES.get(n);
            String fn = (a != null) ? a.funcao : "?";
            String cor = COR_FUNCAO.getOrDefault(fn, "#555");
            cards.append("<div class=\"agente\" style=\"--c:").append(cor).append("\">")
                    .append("<div class=\"ini\">").append(esc(INICIAL.getOrDefault(fn, "?"))).append("</div>")
                    .append("<div class=\"ag-nome\">").append(esc(n)).append("</div>")
                    .append("<div class=\"ag-fn\">").append(esc(fn)).append("</div></div>");
        }

        StringBuilder blocos = new StringBuilder();
        for (ValPlayParser.ExecuteContext ex : ctx.execute()) {
            blocos.append(bloco(ex));
        }

        saida = PAGINA
                .replace("__MAPA__", esc(mapa))
                .replace("__AVISOS__", montarAvisos())
                .replace("__CARDS__", cards.toString())
                .replace("__BLOCOS__", blocos.toString());
        return null;
    }

    /** Monta o banner de avisos de balanceamento (vazio se nao houver). */
    private String montarAvisos() {
        List<String> avisos = ValPlaySemanticoUtils.avisos;
        if (avisos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"avisos\"><div class=\"avisos-h\">AVISOS DE BALANCEAMENTO</div><ul>");
        for (String a : avisos) {
            sb.append("<li>").append(esc(a)).append("</li>");
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private String bloco(ValPlayParser.ExecuteContext ex) {
        String site = ex.IDENT().getText();
        StringBuilder passos = new StringBuilder();
        List<String[]> marcadores = new ArrayList<>(); // {label, cor}
        int i = 0;
        for (ValPlayParser.AcaoContext ac : ex.acao()) {
            i++;
            passos.append(passo(ac, i, marcadores));
        }
        return "<section class=\"exec\"><h3>EXECUTE &middot; SITE " + esc(site) + "</h3>"
                + "<div class=\"exec-grid\"><ol class=\"timeline\">" + passos + "</ol>"
                + svgSite(site, marcadores) + "</div></section>";
    }

    private String passo(ValPlayParser.AcaoContext ac, int i, List<String[]> marc) {
        String cor = "#9aa0a6";
        String rotulo = "";

        if (ac instanceof ValPlayParser.AcaoUsaContext u) {
            String agente = u.IDENT().getText();
            Agente a = BancoDeAgentes.AGENTES.get(agente);
            String agcor = (a != null) ? COR_FUNCAO.getOrDefault(a.funcao, "#777") : "#777";
            String hab = texto(u.CADEIA().get(0));
            String cat = (a != null && a.habilidades.containsKey(hab))
                    ? a.habilidades.get(hab).categoria : "outro";
            cor = COR_CAT.getOrDefault(cat, "#9aa0a6");
            List<String> alvos = alvos(u.CADEIA());
            rotulo = "<b style=\"color:" + agcor + "\">" + esc(agente) + "</b> "
                    + "<span class=\"vb\">usa</span> "
                    + "<span class=\"hab\" style=\"--hc:" + cor + "\">" + esc(hab) + "</span>"
                    + alvoTexto(alvos);
            marcar(marc, alvos, hab, cor);
        } else if (ac instanceof ValPlayParser.AcaoUltimateContext ul) {
            String agente = ul.IDENT().getText();
            Agente a = BancoDeAgentes.AGENTES.get(agente);
            String agcor = (a != null) ? COR_FUNCAO.getOrDefault(a.funcao, "#777") : "#777";
            String hab = texto(ul.CADEIA().get(0));
            cor = COR_CAT.get("ultimate");
            List<String> alvos = alvos(ul.CADEIA());
            rotulo = "<b style=\"color:" + agcor + "\">" + esc(agente) + "</b> "
                    + "<span class=\"vb\">ULT</span> "
                    + "<span class=\"hab\" style=\"--hc:" + cor + "\">" + esc(hab) + "</span>"
                    + alvoTexto(alvos);
            marcar(marc, alvos, hab, cor);
        } else if (ac instanceof ValPlayParser.AcaoEntraContext e) {
            String agente = e.IDENT().getText();
            Agente a = BancoDeAgentes.AGENTES.get(agente);
            String agcor = (a != null) ? COR_FUNCAO.getOrDefault(a.funcao, "#777") : "#777";
            cor = "#ff4655";
            rotulo = "<b style=\"color:" + agcor + "\">" + esc(agente) + "</b> "
                    + "<span class=\"vb entry\">ENTRA no site</span>";
        } else if (ac instanceof ValPlayParser.AcaoPlantaContext p) {
            String agente = p.IDENT().getText();
            Agente a = BancoDeAgentes.AGENTES.get(agente);
            String agcor = (a != null) ? COR_FUNCAO.getOrDefault(a.funcao, "#777") : "#777";
            cor = "#eceef3";
            rotulo = "<b style=\"color:" + agcor + "\">" + esc(agente) + "</b> "
                    + "<span class=\"vb plant\">PLANTA a spike</span>";
        }

        return "<li style=\"--dot:" + cor + "\"><span class=\"num\">"
                + String.format("%02d", i) + "</span><span class=\"txt\">" + rotulo + "</span></li>";
    }

    private List<String> alvos(List<TerminalNode> cadeias) {
        List<String> alvos = new ArrayList<>();
        for (int k = 1; k < cadeias.size(); k++) {
            alvos.add(texto(cadeias.get(k)));
        }
        return alvos;
    }

    private String alvoTexto(List<String> alvos) {
        if (alvos.isEmpty()) {
            return "";
        }
        List<String> escapados = new ArrayList<>();
        for (String a : alvos) {
            escapados.add(esc(a));
        }
        return " &rarr; " + String.join(", ", escapados);
    }

    private void marcar(List<String[]> marc, List<String> alvos, String hab, String cor) {
        if (alvos.isEmpty()) {
            marc.add(new String[]{hab, cor});
        } else {
            for (String al : alvos) {
                marc.add(new String[]{al, cor});
            }
        }
    }

    private String svgSite(String site, List<String[]> marc) {
        final int w = 380;
        final int h = 300;
        final int cols = 3;
        final int x0 = 70;
        final int y0 = 70;
        final int dx = 110;
        final int dy = 62;

        StringBuilder pins = new StringBuilder();
        int max = Math.min(marc.size(), 9);
        for (int idx = 0; idx < max; idx++) {
            String label = marc.get(idx)[0];
            String cor = marc.get(idx)[1];
            int r = idx / cols;
            int c = idx % cols;
            int cx = x0 + c * dx;
            int cy = y0 + r * dy;
            String lab = esc(label.length() > 14 ? label.substring(0, 14) : label);
            pins.append("<g>")
                    .append("<circle cx=\"").append(cx).append("\" cy=\"").append(cy)
                    .append("\" r=\"9\" fill=\"").append(cor)
                    .append("\" fill-opacity=\"0.9\" stroke=\"#0c0e12\" stroke-width=\"2\"/>")
                    .append("<circle cx=\"").append(cx).append("\" cy=\"").append(cy)
                    .append("\" r=\"16\" fill=\"none\" stroke=\"").append(cor)
                    .append("\" stroke-opacity=\"0.35\"/>")
                    .append("<text x=\"").append(cx).append("\" y=\"").append(cy + 26)
                    .append("\" class=\"pin-l\">").append(lab).append("</text></g>");
        }

        return "<svg class=\"schem\" viewBox=\"0 0 " + w + " " + h + "\" role=\"img\" aria-label=\"esquema do site "
                + esc(site) + "\">"
                + "<defs>"
                + "<pattern id=\"grid\" width=\"20\" height=\"20\" patternUnits=\"userSpaceOnUse\">"
                + "<path d=\"M20 0H0V20\" fill=\"none\" stroke=\"#1b1f27\" stroke-width=\"1\"/></pattern>"
                + "<marker id=\"arr\" markerWidth=\"10\" markerHeight=\"10\" refX=\"6\" refY=\"3\" orient=\"auto\">"
                + "<path d=\"M0 0 L6 3 L0 6 Z\" fill=\"#ff4655\"/></marker>"
                + "</defs>"
                + "<rect x=\"0\" y=\"0\" width=\"" + w + "\" height=\"" + h + "\" fill=\"#0c0e12\"/>"
                + "<rect x=\"34\" y=\"28\" width=\"312\" height=\"214\" rx=\"10\" fill=\"#12151c\" stroke=\"#262b35\"/>"
                + "<rect x=\"34\" y=\"28\" width=\"312\" height=\"214\" rx=\"10\" fill=\"url(#grid)\"/>"
                + "<text x=\"46\" y=\"48\" class=\"site-t\">SITE " + esc(site) + "</text>"
                + "<rect x=\"250\" y=\"180\" width=\"58\" height=\"40\" rx=\"6\" fill=\"none\" stroke=\"#ff4655\" stroke-dasharray=\"4 3\"/>"
                + "<text x=\"279\" y=\"204\" class=\"spike\">SPIKE</text>"
                + "<path d=\"M190 290 L190 246\" stroke=\"#ff4655\" stroke-width=\"3\" marker-end=\"url(#arr)\"/>"
                + "<text x=\"196\" y=\"276\" class=\"entry-t\">ENTRADA</text>"
                + pins
                + "</svg>";
    }

    private static String texto(TerminalNode t) {
        String s = t.getText();
        return (s.length() >= 2) ? s.substring(1, s.length() - 1) : s;
    }

    private static String esc(String t) {
        if (t == null) {
            return "";
        }
        return t.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // Template do playbook. Placeholders: __MAPA__, __CARDS__, __BLOCOS__.
    private static final String PAGINA = """
<!doctype html><html lang="pt-br"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Playbook - __MAPA__</title>
<style>
@import url('https://fonts.googleapis.com/css2?family=Teko:wght@500;600;700&family=Chivo:wght@400;600&display=swap');
:root{--bg:#0a0c10;--panel:#12151c;--line:#262b35;--ink:#e7eaf0;--mut:#8a909c;--red:#ff4655;}
*{box-sizing:border-box}
body{margin:0;background:radial-gradient(1200px 600px at 70% -10%,#161a22,#0a0c10);
 color:var(--ink);font-family:'Chivo',system-ui,sans-serif;padding:32px 20px 60px}
.wrap{max-width:880px;margin:0 auto}
.top{display:flex;align-items:baseline;gap:14px;border-bottom:2px solid var(--red);padding-bottom:10px}
.kick{font-family:'Teko';letter-spacing:.18em;color:var(--red);font-size:18px;font-weight:600}
h1{font-family:'Teko';font-size:64px;line-height:.9;margin:0;letter-spacing:.02em;text-transform:uppercase}
h2{font-family:'Teko';font-size:26px;letter-spacing:.12em;color:var(--mut);margin:34px 0 12px;text-transform:uppercase}
.comp{display:grid;grid-template-columns:repeat(5,1fr);gap:10px}
.agente{background:var(--panel);border:1px solid var(--line);border-top:3px solid var(--c);
 border-radius:10px;padding:12px 10px;text-align:center;position:relative;overflow:hidden}
.agente:before{content:"";position:absolute;inset:0;background:radial-gradient(60px 60px at 50% -10%,var(--c),transparent);opacity:.18}
.ini{font-family:'Teko';font-size:30px;color:var(--c);line-height:1}
.ag-nome{font-weight:600;font-size:15px;margin-top:2px}
.ag-fn{font-size:11px;color:var(--mut);text-transform:uppercase;letter-spacing:.08em}
.exec{background:var(--panel);border:1px solid var(--line);border-radius:14px;padding:18px 20px;margin-top:16px}
.exec h3{font-family:'Teko';letter-spacing:.12em;font-size:24px;margin:0 0 14px;color:var(--red)}
.exec-grid{display:grid;grid-template-columns:1fr 380px;gap:20px;align-items:start}
@media(max-width:760px){.exec-grid{grid-template-columns:1fr} .comp{grid-template-columns:repeat(2,1fr)} h1{font-size:44px}}
.timeline{list-style:none;margin:0;padding:0}
.timeline li{position:relative;padding:8px 0 8px 30px;border-left:2px solid #20242e;margin-left:8px}
.timeline li:before{content:"";position:absolute;left:-7px;top:14px;width:12px;height:12px;border-radius:50%;
 background:var(--dot);box-shadow:0 0 0 3px #0c0e12}
.num{font-family:'Teko';color:var(--mut);font-size:15px;margin-right:8px}
.txt{font-size:14px}
.vb{font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:var(--mut);padding:1px 6px;border:1px solid var(--line);border-radius:5px;margin:0 4px}
.vb.entry{color:#fff;background:var(--red);border-color:var(--red)}
.vb.plant{color:#0a0c10;background:#eceef3;border-color:#eceef3}
.hab{color:var(--hc);font-weight:600}
.schem{width:380px;max-width:100%;border-radius:10px;border:1px solid var(--line)}
.site-t{font-family:'Teko';fill:#3a4150;font-size:18px;letter-spacing:.1em}
.pin-l{fill:#c9ced8;font-size:10px;text-anchor:middle;font-family:'Chivo'}
.spike{fill:var(--red);font-size:10px;text-anchor:middle;font-family:'Teko';letter-spacing:.1em}
.entry-t{fill:var(--red);font-size:10px;font-family:'Teko';letter-spacing:.12em}
.foot{color:var(--mut);font-size:12px;margin-top:24px;text-align:center}
.leg{display:flex;flex-wrap:wrap;gap:12px;margin-top:10px;color:var(--mut);font-size:11px}
.leg span{display:inline-flex;align-items:center;gap:5px}
.leg i{width:10px;height:10px;border-radius:50%;display:inline-block}
.avisos{background:rgba(245,197,66,.07);border:1px solid #4d4322;border-left:3px solid #f5c542;border-radius:10px;padding:12px 16px;margin-top:16px}
.avisos-h{font-family:'Teko';letter-spacing:.12em;color:#f5c542;font-size:18px;margin-bottom:4px}
.avisos ul{margin:0;padding-left:18px}
.avisos li{font-size:13px;color:#d7c89a;margin:2px 0}
</style></head><body><div class="wrap">
<div class="top"><span class="kick">VALPLAY &middot; PLAYBOOK</span></div>
<h1>__MAPA__</h1>
<h2>Composicao</h2>
<div class="comp">__CARDS__</div>
<div class="leg">
  <span><i style="background:#8a8f98"></i>smoke</span><span><i style="background:#ffe23d"></i>flash</span>
  <span><i style="background:#3da5ff"></i>recon</span><span><i style="background:#ff7a3c"></i>molly</span>
  <span><i style="background:#26d0a0"></i>wall</span><span><i style="background:#ffab00"></i>ultimate</span>
</div>
__AVISOS__
<h2>Estrategias de Site</h2>
__BLOCOS__
<p class="foot">Gerado por ValPlay - compilador de composicoes e estrategias para Valorant.</p>
</div></body></html>""";
}