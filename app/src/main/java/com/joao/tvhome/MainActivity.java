package com.joao.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Tela inicial: foto fixa, relógio, data, clima de Ceres e dock de apps. */
public class MainActivity extends Activity {

    /** Apps do dock, na ordem em que aparecem. Os que não estiverem instalados são pulados. */
    static final String[][] DOCK = {
            {"YouTube", "com.google.android.youtube.tv"},
            {"Netflix", "com.netflix.ninja"},
            {"Disney+", "com.disney.disneyplus"},
            {"Globoplay", "com.globo.globotv"},
            {"Stremio", "com.stremio.one"},
    };

    // medidas do dock, em px de uma tela 1920x1080
    private static final float TILE = 120f;      // lado de cada ícone
    private static final float GAP = 36f;        // folga entre ícones
    private static final float DOT = 10f;        // pontinho vermelho
    private static final float DOT_GAP = 20f;    // do ícone ao pontinho
    private static final float PILL_GAP = 28f;   // da etiqueta ao ícone
    private static final float PILL_H = 46f;     // altura da etiqueta
    private static final float DOT_BOTTOM = 96f; // do pontinho à base da tela
    private static final float DOCK_TOP = DOT_BOTTOM + DOT + DOT_GAP + TILE * 1.09f + PILL_GAP + PILL_H;
    private static final Locale PT_BR = new Locale("pt", "BR");

    private TextView greet, clock, date, temp, cond, extra;
    private WeatherIconView wicon;
    private final List<TileView> tiles = new ArrayList<>();
    private int lastFocus = 0;
    private Weather.Data weather;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable weatherLoop = new Runnable() {
        @Override
        public void run() {
            refreshWeather();
            handler.postDelayed(this, Weather.REFRESH_MS);
        }
    };
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateClock();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        lastFocus = getPreferences(MODE_PRIVATE).getInt("foco", 0);
        setContentView(build());
        weather = Weather.cached(this);
        showWeather();
    }

    private View build() {
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);

        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImageResource(R.drawable.fundo);
        root.addView(bg, match());
        root.addView(new ShadeView(this, 0.8f), match());

        // o bloco do relógio fica acima do dock, com folga para a etiqueta do app em foco
        root.addView(buildInfo(), anchored(Gravity.BOTTOM | Gravity.START, Ui.px(112), Ui.px(DOCK_TOP + 34)));

        // o pontinho vermelho fica a 96 px da base
        root.addView(buildDock(), anchored(Gravity.BOTTOM | Gravity.START, Ui.px(116), Ui.px(DOT_BOTTOM)));

        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        blp.rightMargin = Ui.px(112);
        // centro dos botões alinhado ao centro dos blocos do dock
        blp.bottomMargin = Ui.px(DOT_BOTTOM + DOT + DOT_GAP + TILE / 2 - 38);
        root.addView(buildButtons(), blp);
        return root;
    }

    private View buildInfo() {
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);

        greet = Ui.text(this, 26, 400, Ui.ACCENT);
        greet.setLetterSpacing(0.18f);
        greet.setAllCaps(true);
        info.addView(greet);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = wrap();
        rlp.topMargin = Ui.px(14);
        info.addView(row, rlp);

        clock = Ui.text(this, 150, 200, Ui.WHITE);
        clock.setLetterSpacing(-0.02f);
        row.addView(clock);

        View divider = new View(this);
        GradientDrawable dg = new GradientDrawable();
        dg.setColor(Ui.ACCENT);
        dg.setCornerRadius(Ui.pxf(2));
        divider.setBackground(dg);
        LinearLayout.LayoutParams divlp = new LinearLayout.LayoutParams(Ui.px(3), Ui.px(120));
        divlp.leftMargin = Ui.px(44);
        divlp.rightMargin = Ui.px(44);
        row.addView(divider, divlp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        row.addView(col);

        date = Ui.text(this, 36, 300, Ui.WHITE);
        col.addView(date);

        LinearLayout wrow = new LinearLayout(this);
        wrow.setOrientation(LinearLayout.HORIZONTAL);
        wrow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams wlp = wrap();
        wlp.topMargin = Ui.px(16);
        col.addView(wrow, wlp);

        wicon = new WeatherIconView(this);
        wrow.addView(wicon);
        temp = Ui.text(this, 40, 300, Ui.WHITE);
        LinearLayout.LayoutParams tlp = wrap();
        tlp.leftMargin = Ui.px(16);
        wrow.addView(temp, tlp);
        cond = Ui.text(this, 28, 300, 0xCCFFFFFF);
        LinearLayout.LayoutParams clp = wrap();
        clp.leftMargin = Ui.px(16);
        wrow.addView(cond, clp);

        extra = Ui.text(this, 22, 300, 0xADFFFFFF);
        extra.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams elp = wrap();
        elp.topMargin = Ui.px(16);
        col.addView(extra, elp);
        return info;
    }

    private View buildDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.BOTTOM);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);

        tiles.clear();
        for (String[] app : DOCK) {
            final String name = app[0];
            final String pkg = app[1];
            if (!Apps.installed(this, pkg)) continue;

            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL);
            column.setClipChildren(false);

            final TextView pill = Ui.text(this, 24, 400, Ui.WHITE);
            pill.setText(name);
            pill.setPadding(Ui.px(22), Ui.px(8), Ui.px(22), Ui.px(8));
            GradientDrawable pg = new GradientDrawable();
            pg.setCornerRadius(Ui.pxf(999));
            pg.setColor(Ui.alpha(Ui.ACCENT, 0.22f));
            pg.setStroke(Math.max(1, Ui.px(1)), Ui.alpha(Ui.ACCENT, 0.75f));
            pill.setBackground(pg);
            pill.setVisibility(View.INVISIBLE);
            LinearLayout.LayoutParams plp = wrap();
            plp.bottomMargin = Ui.px(PILL_GAP);
            column.addView(pill, plp);

            final TileView tile = new TileView(this, Apps.icon(this, pkg), TILE);
            tile.setContentDescription(name);
            column.addView(tile);

            final View dot = new View(this);
            GradientDrawable dd = new GradientDrawable();
            dd.setShape(GradientDrawable.OVAL);
            dd.setColor(Ui.ACCENT);
            dot.setBackground(dd);
            dot.setAlpha(0f);
            LinearLayout.LayoutParams dotlp = new LinearLayout.LayoutParams(Ui.px(DOT), Ui.px(DOT));
            dotlp.topMargin = Ui.px(DOT_GAP);
            column.addView(dot, dotlp);

            final int index = tiles.size();
            tile.setOnFocusChangeListener((v, has) -> {
                tile.setFocusedLook(has);
                pill.setVisibility(has ? View.VISIBLE : View.INVISIBLE);
                dot.animate().alpha(has ? 1f : 0f).setDuration(160).start();
                if (has) lastFocus = index;
            });
            tile.setOnClickListener(v -> Apps.launch(this, pkg));

            LinearLayout.LayoutParams colp = wrap();
            if (index > 0) colp.leftMargin = Ui.px(GAP);
            dock.addView(column, colp);
            tiles.add(tile);
        }
        return dock;
    }

    private View buildButtons() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        int[] kinds = {RoundButton.APPS, RoundButton.WIFI, RoundButton.SETTINGS};
        String[] labels = {"Todos os apps", "Wi-Fi", "Configurações"};
        for (int i = 0; i < kinds.length; i++) {
            final int kind = kinds[i];
            final RoundButton b = new RoundButton(this, kind, labels[i]);
            b.setOnFocusChangeListener((v, has) -> b.setFocusedLook(has));
            b.setOnClickListener(v -> onButton(kind));
            LinearLayout.LayoutParams lp = wrap();
            if (i > 0) lp.leftMargin = Ui.px(22);
            row.addView(b, lp);
        }
        return row;
    }

    private void onButton(int kind) {
        if (kind == RoundButton.APPS) {
            startActivity(new Intent(this, TodosAppsActivity.class));
            return;
        }
        String action = kind == RoundButton.WIFI ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_SETTINGS;
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    // ---------- relógio ----------

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        int h = now.get(Calendar.HOUR_OF_DAY);
        greet.setText((h < 12 ? "Bom dia" : h < 18 ? "Boa tarde" : "Boa noite") + ", João");
        clock.setText(String.format(Locale.US, "%02d:%02d", h, now.get(Calendar.MINUTE)));
        String d = new SimpleDateFormat("EEEE, d 'de' MMMM", PT_BR).format(now.getTime());
        date.setText(d.substring(0, 1).toUpperCase(PT_BR) + d.substring(1));
    }

    // ---------- clima ----------

    private void refreshWeather() {
        Weather.fetch(this, d -> {
            weather = d;
            showWeather();
        });
    }

    private void showWeather() {
        if (weather == null) {
            wicon.set(WeatherIconView.PARTLY, true);
            temp.setText("--°");
            cond.setText("Carregando clima · " + Weather.CITY);
            extra.setText("");
            return;
        }
        wicon.set(Weather.icon(weather.code), weather.day);
        temp.setText(weather.temp + "°");
        cond.setText(Weather.describe(weather.code) + " · " + Weather.CITY);
        extra.setText("Máx " + weather.max + "° · Mín " + weather.min + "° · Chuva " + weather.rain + "%");
    }

    // ---------- ciclo de vida ----------

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeReceiver, f);
        updateClock();

        long age = weather == null ? Long.MAX_VALUE : System.currentTimeMillis() - weather.fetchedAt;
        long first = age >= Weather.REFRESH_MS ? 0 : Weather.REFRESH_MS - age;
        handler.removeCallbacks(weatherLoop);
        handler.postDelayed(weatherLoop, first);

        if (!tiles.isEmpty()) {
            tiles.get(Math.min(lastFocus, tiles.size() - 1)).requestFocus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(timeReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        handler.removeCallbacks(weatherLoop);
        getPreferences(MODE_PRIVATE).edit().putInt("foco", lastFocus).apply();
    }

    @Override
    public void onBackPressed() {
        // é a tela inicial: voltar não faz nada
    }

    // ---------- layout ----------

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static FrameLayout.LayoutParams anchored(int gravity, int left, int bottom) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
        lp.leftMargin = left;
        lp.bottomMargin = bottom;
        return lp;
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
