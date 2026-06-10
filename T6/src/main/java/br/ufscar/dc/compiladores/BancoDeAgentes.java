package br.ufscar.dc.compiladores;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Banco de dados embutido com os agentes e mapas reais de Valorant.
 *
 * Funciona como a "biblioteca padrao" da linguagem: o usuario apenas referencia
 * os agentes pelo nome, e o analisador semantico valida tudo com esses dados.
 *
 * Para atualizar o jogo basta editar este arquivo.
 */
public final class BancoDeAgentes {

    /** Uma habilidade: quantas cargas tem e a categoria (usada so para colorir o esquema). */
    public static final class Habilidade {
        public final int cargas;
        public final String categoria;
        public Habilidade(int cargas, String categoria){
            this.cargas = cargas;
            this.categoria = categoria;
        }
    }

    /** Um agente: funcao, habilidades basicas (com cargas) e o nome da ultimate. */
    public static final class Agente {
        public final String funcao;
        public final Map<String, Habilidade> habilidades;
        public final String ultimate;
        public Agente(String funcao, Map<String, Habilidade> habilidades, String ultimate) {
            this.funcao = funcao;
            this.habilidades = habilidades;
            this.ultimate = ultimate;
        }
    }

    public static final Map<String, Agente> AGENTES = new LinkedHashMap<>();
    public static final Map<String, List<String>> MAPAS = new LinkedHashMap<>();

    // Helper para cadastrar um agente: ag(nome, funcao, ultimate, hab, cargas, cat, ...)
    private static void ag(String nome, String funcao, String ultimate, Object... kit) {
        Map<String, Habilidade> h = new LinkedHashMap<>();
        for (int i = 0; i < kit.length; i += 3) {
            h.put((String) kit[i], new Habilidade((Integer) kit[i + 1], (String) kit[i + 2]));
        }
        AGENTES.put(nome, new Agente(funcao, h, ultimate));
    }

    static {
        // ---- Duelistas ----
        ag("Jett", "Duelista", "Blade Storm",
                "Cloudburst", 2, "smoke", "Updraft", 2, "dash", "Tailwind", 1, "dash");
        ag("Phoenix", "Duelista", "Run It Back",
                "Blaze", 1, "wall", "Curveball", 2, "flash", "Hot Hands", 1, "molly");
        ag("Raze", "Duelista", "Showstopper",
                "Boom Bot", 1, "recon", "Blast Pack", 2, "dash", "Paint Shells", 1, "molly");
        ag("Neon", "Duelista", "Overdrive",
                "Fast Lane", 1, "wall", "Relay Bolt", 2, "dano", "High Gear", 1, "dash");
        ag("Yoru", "Duelista", "Dimensional Drift",
                "Fakeout", 2, "flash", "Blindside", 2, "flash", "Gatecrash", 2, "dash");

        // ---- Iniciadores ----
        ag("Sova", "Iniciador", "Hunter's Fury",
                "Owl Drone", 1, "recon", "Shock Bolt", 2, "dano", "Recon Bolt", 1, "recon");
        ag("Breach", "Iniciador", "Rolling Thunder",
                "Aftershock", 1, "dano", "Flashpoint", 2, "flash", "Fault Line", 1, "flash");
        ag("Skye", "Iniciador", "Seekers",
                "Regrowth", 1, "heal", "Trailblazer", 1, "recon", "Guiding Light", 2, "flash");
        ag("KAY/O", "Iniciador", "NULL/cmd",
                "FRAG/ment", 1, "molly", "Flash/drive", 2, "flash", "Zero/point", 1, "recon");
        ag("Fade", "Iniciador", "Nightfall",
                "Prowler", 2, "recon", "Seize", 1, "trap", "Haunt", 1, "recon");

        // ---- Controladores ----
        ag("Brimstone", "Controlador", "Orbital Strike",
                "Stim Beacon", 1, "outro", "Incendiary", 1, "molly", "Sky Smoke", 1, "smoke");
        ag("Omen", "Controlador", "From the Shadows",
                "Shrouded Step", 2, "dash", "Paranoia", 1, "flash", "Dark Cover", 2, "smoke");
        ag("Viper", "Controlador", "Viper's Pit",
                "Snake Bite", 2, "molly", "Poison Cloud", 1, "smoke", "Toxic Screen", 1, "wall");
        ag("Harbor", "Controlador", "Reckoning",
                "Cascade", 1, "wall", "Cove", 1, "smoke", "High Tide", 1, "wall");

        // ---- Sentinelas ----
        ag("Killjoy", "Sentinela", "Lockdown",
                "Nanoswarm", 2, "molly", "Alarmbot", 1, "trap", "Turret", 1, "trap");
        ag("Sage", "Sentinela", "Resurrection",
                "Barrier Orb", 1, "wall", "Slow Orb", 2, "trap", "Healing Orb", 1, "heal");
        ag("Cypher", "Sentinela", "Neural Theft",
                "Trapwire", 2, "trap", "Cyber Cage", 1, "smoke", "Spycam", 1, "recon");
        ag("Deadlock", "Sentinela", "Annihilation",
                "GravNet", 1, "trap", "Sonic Sensor", 1, "trap", "Barrier Mesh", 1, "wall");

        // ---- Mapas e seus sites ----
        MAPAS.put("Ascent", List.of("A", "B"));
        MAPAS.put("Bind", List.of("A", "B"));
        MAPAS.put("Haven", List.of("A", "B", "C"));
        MAPAS.put("Split", List.of("A", "B"));
        MAPAS.put("Lotus", List.of("A", "B", "C"));
        MAPAS.put("Icebox", List.of("A", "B"));
        MAPAS.put("Sunset", List.of("A", "B"));
    }

    public static boolean agenteExiste(String nome) {
        return AGENTES.containsKey(nome);
    }

    public static boolean mapaExiste(String mapa) {
        return MAPAS.containsKey(mapa);
    }

    public static List<String> sitesDoMapa(String mapa) {
        return MAPAS.getOrDefault(mapa, Collections.emptyList());
    }

    private BancoDeAgentes() { }
}
