package com.joao.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** As 6 posições da tela inicial, guardadas na própria box. "" = posição vazia. */
final class Dock {
    static final int SIZE = 6;

    private static final String[] DEFAULT = {
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.disney.disneyplus",
            "com.globo.globotv",
            "com.stremio.one",
            "",
    };

    private Dock() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("dock", Context.MODE_PRIVATE);
    }

    static String[] load(Context c) {
        String saved = prefs(c).getString("posicoes", null);
        String[] out = DEFAULT.clone();
        if (saved != null) {
            String[] parts = saved.split(",", -1);
            for (int i = 0; i < SIZE; i++) out[i] = i < parts.length ? parts[i].trim() : "";
        }
        return out;
    }

    static void save(Context c, String[] slots) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < SIZE; i++) {
            if (i > 0) b.append(',');
            b.append(i < slots.length && slots[i] != null ? slots[i] : "");
        }
        prefs(c).edit().putString("posicoes", b.toString()).apply();
    }

    static void set(Context c, int index, String pkg) {
        String[] s = load(c);
        if (index < 0 || index >= SIZE) return;
        // o mesmo app não aparece duas vezes: se já estava em outra posição, ela fica vazia
        for (int i = 0; i < SIZE; i++) if (i != index && s[i].equals(pkg)) s[i] = "";
        s[index] = pkg;
        save(c, s);
    }

    /** Troca a posição index com a vizinha (dir = -1 esquerda, +1 direita). Devolve a nova posição. */
    static int move(Context c, int index, int dir) {
        String[] s = load(c);
        int to = index + dir;
        if (to < 0 || to >= SIZE) return index;
        String t = s[to];
        s[to] = s[index];
        s[index] = t;
        save(c, s);
        return to;
    }

    /**
     * Tira das posições os apps que não estão mais instalados.
     * Chamado quando a tela volta e quando o sistema avisa que um pacote saiu: sem isso a
     * posição continuava guardando o nome do pacote e a tela mostrava o ícone de um app
     * que já tinha sido desinstalado.
     */
    static boolean limparSumidos(Context c) {
        String[] s = load(c);
        boolean mudou = false;
        for (int i = 0; i < SIZE; i++) {
            if (!s[i].isEmpty() && !Apps.installed(c, s[i])) {
                s[i] = "";
                mudou = true;
            }
        }
        if (mudou) save(c, s);
        return mudou;
    }

    static boolean contains(Context c, String pkg) {
        for (String p : load(c)) if (p.equals(pkg)) return true;
        return false;
    }
}
