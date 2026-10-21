package com.joao.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tela inicial da TV TCL (Google TV): foto da praia, menu Sistema no topo (Wi-Fi, Play Store e Configurações),
 * hora com a data embaixo, clima de Ceres com a previsão dos próximos dias e todos os apps direto na tela.
 * A TV liga com o foco nos apps; a seta para cima leva ao menu Sistema.
 */
public class MainActivity extends Activity {

    /** Apps que vêm primeiro, nesta ordem. Os outros instalados entram depois, em ordem alfabética. */
    static final String[] ORDER = {
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.disney.disneyplus",
            "com.wbd.stream",
            "com.globo.globotv",
            "com.google.android.youtube.tvmusic",
    };

    /** Apps de fábrica da TCL e do chip da TV: nunca aparecem na fileira (Protetor, Quick Panel, TCL Channel, TCL Home, TV...). */
    static final String[] HIDDEN_PREFIX = {"com.tcl.", "com.droidlogic", "com.amlogic", "com.mirakulo", "com.tvos"};

    /** Não aparecem na fileira de apps: ficam no menu Sistema ou não são apps de uso. */
    static final Set<String> HIDDEN = new HashSet<>(Arrays.asList(
            "com.android.vending",
            "com.android.tv.settings",
            "com.google.android.tv.remote.service",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvlauncher",
            "com.android.documentsui"
    ));

    private static final float TILE = 132f;
    private static final float GAP = 32f;
    private static final Locale PT_BR = new Locale("pt", "BR");

    private FrameLayout root;
    private TextView clock, date, temp, cond, extra;
    private WeatherIconView wicon;
    private LinearLayout forecast, appsRow;
    private final List<TileView> tiles = new ArrayList<>();
    private final List<View> pills = new ArrayList<>();
    private String appsKey = "";
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
        root = new FrameLayout(this);
        root.setClipChildren(false);

        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImageResource(R.drawable.fundo);
        root.addView(bg, match());
        root.addView(new Shade(this), match());

        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tl.leftMargin = Ui.px(112);
        tl.topMargin = Ui.px(64);
        root.addView(buildTop(), tl);

        FrameLayout.LayoutParams tr = new FrameLayout.LayoutParams(Ui.px(640),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        tr.rightMargin = Ui.px(112);
        tr.topMargin = Ui.px(64);
        root.addView(buildWeather(), tr);

        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        bl.leftMargin = Ui.px(88);
        bl.rightMargin = Ui.px(88);
        bl.bottomMargin = Ui.px(60);
        root.addView(buildApps(), bl);
        return root;
    }

    // ---------- topo: menu Sistema, hora e data ----------

    private View buildTop() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        menu.setClipChildren(false);
        pills.clear();
        String[] names = {"Wi-Fi", "Play Store", "Configurações"};
        int[] kinds = {PillIcon.WIFI, PillIcon.PLAY, PillIcon.GEAR};
        for (int i = 0; i < names.length; i++) {
            final int kind = kinds[i];
            View p = pill(names[i], kind);
            p.setOnClickListener(v -> onSystem(kind));
            // largura calculada pelo texto (em negrito, o maior caso), para o nome nunca ser cortado
            android.graphics.Paint m = new android.graphics.Paint();
            m.setTypeface(Ui.font(this, 500));
            m.setTextSize(Ui.pxf(22));
            int w = Ui.px(18 + 26 + 12 + 24 + 8) + Math.round(m.measureText(names[i]));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, Ui.px(60));
            if (i > 0) lp.leftMargin = Ui.px(14);
            menu.addView(p, lp);
            pills.add(p);
        }
        col.addView(menu);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = wrap();
        rlp.topMargin = Ui.px(44);
        col.addView(row, rlp);

        View divider = new View(this);
        GradientDrawable dg = new GradientDrawable();
        dg.setColor(Ui.ACCENT);
        dg.setCornerRadius(Ui.pxf(2));
        divider.setBackground(dg);
        LinearLayout.LayoutParams divlp = new LinearLayout.LayoutParams(Ui.px(3), Ui.px(104));
        divlp.rightMargin = Ui.px(28);
        row.addView(divider, divlp);

        clock = Ui.text(this, 132, 200, Ui.WHITE);
        clock.setLetterSpacing(-0.02f);
        clock.setShadowLayer(Ui.pxf(24), 0, Ui.pxf(4), 0x73000000);
        row.addView(clock);

        date = Ui.text(this, 34, 300, Ui.WHITE);
        date.setShadowLayer(Ui.pxf(12), 0, Ui.pxf(2), 0x80000000);
        date.setPadding(Ui.px(31), 0, 0, 0);
        LinearLayout.LayoutParams dlp = wrap();
        dlp.topMargin = Ui.px(10);
        col.addView(date, dlp);
        return col;
    }

    private View pill(String name, int kind) {
        final LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setFocusable(true);
        p.setClickable(true);
        p.setContentDescription(name);
        p.setPadding(Ui.px(18), 0, Ui.px(24), 0);
        p.setId(View.generateViewId());

        PillIcon icon = new PillIcon(this, kind);
        p.addView(icon, new LinearLayout.LayoutParams(Ui.px(26), Ui.px(26)));
        final TextView t = Ui.text(this, 22, 300, Ui.WHITE);
        t.setText(name);
        t.setSingleLine(true);
        t.setMaxLines(1);
        LinearLayout.LayoutParams tlp = wrap();
        tlp.leftMargin = Ui.px(12);
        p.addView(t, tlp);

        final GradientDrawable off = new GradientDrawable();
        off.setCornerRadius(Ui.pxf(30));
        off.setColor(0x8C0A0E14);
        off.setStroke(Math.max(1, Ui.px(1)), 0x2EFFFFFF);
        final GradientDrawable on = new GradientDrawable();
        on.setCornerRadius(Ui.pxf(30));
        on.setColor(Ui.alpha(Ui.ACCENT, 0.35f));
        on.setStroke(Ui.px(2), Ui.ACCENT);
        p.setBackground(off);
        p.setOnFocusChangeListener((v, has) -> {
            p.setBackground(has ? on : off);
            t.setTypeface(Ui.font(this, has ? 500 : 300));
        });
        return p;
    }

    private void onSystem(int kind) {
        if (kind == PillIcon.PLAY) {
            Apps.launch(this, "com.android.vending");
            return;
        }
        String action = kind == PillIcon.WIFI ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_SETTINGS;
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    // ---------- clima de Ceres ----------

    private View buildWeather() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.px(34), Ui.px(28), Ui.px(34), Ui.px(26));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(Ui.pxf(32));
        bg.setColor(0x8C0A0E14);
        bg.setStroke(Math.max(1, Ui.px(1)), 0x29FFFFFF);
        card.setBackground(bg);

        LinearLayout now = new LinearLayout(this);
        now.setOrientation(LinearLayout.HORIZONTAL);
        now.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(now);

        wicon = new WeatherIconView(this, 76);
        now.addView(wicon);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mlp.leftMargin = Ui.px(22);
        now.addView(mid, mlp);
        temp = Ui.text(this, 64, 300, Ui.WHITE);
        mid.addView(temp);
        cond = Ui.text(this, 24, 300, 0xDBFFFFFF);
        cond.setSingleLine(true);
        cond.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams clp = wrap();
        clp.topMargin = Ui.px(4);
        mid.addView(cond, clp);

        extra = Ui.text(this, 21, 300, 0xC7FFFFFF);
        extra.setGravity(Gravity.END);
        extra.setLineSpacing(Ui.pxf(6), 1f);
        now.addView(extra);

        View line = new View(this);
        line.setBackgroundColor(0x2EFFFFFF);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.px(1)));
        llp.topMargin = Ui.px(22);
        llp.bottomMargin = Ui.px(18);
        card.addView(line, llp);

        forecast = new LinearLayout(this);
        forecast.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(forecast, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void showWeather() {
        forecast.removeAllViews();
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
        extra.setText("Máx " + weather.max + "° · Mín " + weather.min + "°\nChuva " + weather.rain + "%");

        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat out = new SimpleDateFormat("EEE", PT_BR);
        int n = Math.min(5, weather.fDate.length);
        for (int i = 0; i < n; i++) {
            LinearLayout day = new LinearLayout(this);
            day.setOrientation(LinearLayout.VERTICAL);
            day.setGravity(Gravity.CENTER_HORIZONTAL);

            String name;
            try {
                name = out.format(in.parse(weather.fDate[i])).replace(".", "");
            } catch (Exception e) {
                name = "";
            }
            TextView dn = Ui.text(this, 20, 400, 0xCCFFFFFF);
            dn.setText(name);
            day.addView(dn);

            WeatherIconView ic = new WeatherIconView(this, 34);
            ic.set(Weather.icon(weather.fCode[i]), true);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.px(34), Ui.px(34));
            ilp.topMargin = Ui.px(8);
            day.addView(ic, ilp);

            TextView mm = Ui.text(this, 22, 400, Ui.WHITE);
            mm.setText(weather.fMax[i] + "° " + weather.fMin[i] + "°");
            LinearLayout.LayoutParams mmlp = wrap();
            mmlp.topMargin = Ui.px(8);
            day.addView(mm, mmlp);

            boolean wet = weather.fRain[i] >= 50;
            TextView rn = Ui.text(this, 18, wet ? 500 : 300, wet ? Ui.ACCENT : 0xA6FFFFFF);
            rn.setText("Chuva " + weather.fRain[i] + "%");
            LinearLayout.LayoutParams rlp = wrap();
            rlp.topMargin = Ui.px(6);
            day.addView(rn, rlp);

            forecast.addView(day, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private void refreshWeather() {
        Weather.fetch(this, d -> {
            weather = d;
            showWeather();
        });
    }

    // ---------- apps: todos direto na tela ----------

    private View buildApps() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);

        TextView label = Ui.text(this, 22, 400, Ui.WHITE);
        label.setText("APPS");
        label.setLetterSpacing(0.18f);
        label.setShadowLayer(Ui.pxf(10), 0, Ui.pxf(2), 0x99000000);
        label.setPadding(Ui.px(24), 0, 0, 0);
        col.addView(label);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(Ui.px(24), Ui.px(26), Ui.px(24), Ui.px(8));
        appsRow = new LinearLayout(this);
        appsRow.setOrientation(LinearLayout.HORIZONTAL);
        appsRow.setClipChildren(false);
        scroll.addView(appsRow);
        col.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fillApps();
        return col;
    }

    /** Apps de uso: os da lista ORDER primeiro, depois os outros instalados em ordem alfabética. */
    private List<Apps.Entry> appList() {
        List<Apps.Entry> all = Apps.all(this);
        List<Apps.Entry> out = new ArrayList<>();
        for (String pkg : ORDER) {
            for (Apps.Entry e : all) if (e.pkg.equals(pkg)) out.add(e);
        }
        Set<String> first = new HashSet<>(Arrays.asList(ORDER));
        for (Apps.Entry e : all) {
            if (!first.contains(e.pkg) && !HIDDEN.contains(e.pkg) && !factory(e.pkg)) out.add(e);
        }
        return out;
    }

    private static boolean factory(String pkg) {
        for (String p : HIDDEN_PREFIX) if (pkg.startsWith(p)) return true;
        return false;
    }

    private void fillApps() {
        List<Apps.Entry> apps = appList();
        StringBuilder key = new StringBuilder();
        for (Apps.Entry e : apps) key.append(e.pkg).append(',');
        appsKey = key.toString();
        appsRow.removeAllViews();
        tiles.clear();
        int up = pills.isEmpty() ? View.NO_ID : pills.get(0).getId();
        for (int i = 0; i < apps.size(); i++) {
            final int index = i;
            final Apps.Entry e = apps.get(i);
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL);
            column.setClipChildren(false);

            final TileView tile = new TileView(this, Apps.icon(this, e.pkg), TILE);
            tile.setContentDescription(e.label);
            tile.setId(View.generateViewId());
            tile.setNextFocusUpId(up);
            column.addView(tile, new LinearLayout.LayoutParams(Ui.px(TILE), Ui.px(TILE)));

            final TextView name = Ui.text(this, 21, 300, 0xD1FFFFFF);
            name.setText(e.label);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            name.setShadowLayer(Ui.pxf(8), 0, Ui.pxf(2), 0x99000000);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = Ui.px(22);
            column.addView(name, nlp);

            tile.setOnFocusChangeListener((v, has) -> {
                tile.setFocusedLook(has);
                name.setTextColor(has ? Ui.WHITE : 0xD1FFFFFF);
                name.setTypeface(Ui.font(this, has ? 500 : 300));
                if (has) lastFocus = index;
            });
            tile.setOnClickListener(v -> Apps.launch(this, e.pkg));

            // largura fixa por app: o bloco e o nome cabem e nada fica por cima do vizinho
            LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(Ui.px(TILE + 24), ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) colp.leftMargin = Ui.px(GAP - 24);
            appsRow.addView(column, colp);
            tiles.add(tile);
        }
        // do menu Sistema, a seta para baixo volta aos apps
        if (!tiles.isEmpty()) {
            for (View p : pills) p.setNextFocusDownId(tiles.get(0).getId());
        }
    }

    private void focusApps() {
        if (tiles.isEmpty()) {
            if (!pills.isEmpty()) pills.get(0).requestFocus();
            return;
        }
        lastFocus = Math.max(0, Math.min(lastFocus, tiles.size() - 1));
        tiles.get(lastFocus).requestFocus();
    }

    // ---------- relógio ----------

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        clock.setText(String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
        String d = new SimpleDateFormat("EEEE, d 'de' MMMM", PT_BR).format(now.getTime());
        date.setText(d.substring(0, 1).toUpperCase(PT_BR) + d.substring(1));
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

        // app instalado ou removido: monta a fileira de novo
        StringBuilder key = new StringBuilder();
        for (Apps.Entry e : appList()) key.append(e.pkg).append(',');
        if (!appsKey.equals(key.toString())) fillApps();
        // a TV liga (e volta) com o foco nos apps
        focusApps();
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
        // é a tela inicial: voltar só leva o foco de volta aos apps
        focusApps();
    }

    // ---------- layout ----------

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Escurece em cima (hora e menu) e embaixo (apps); o meio da foto fica à vista. */
    static class Shade extends View {
        private final Paint paint = new Paint();

        Shade(Context c) {
            super(c);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{0x70000000, 0x00000000, 0x00000000, 0x8C000000, 0xC7000000},
                    new float[]{0f, 0.30f, 0.48f, 0.72f, 1f},
                    Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    /** Ícones pequenos do menu Sistema, em traço branco. */
    static class PillIcon extends View {
        static final int WIFI = 0, PLAY = 1, GEAR = 2;
        private final int kind;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        PillIcon(Context c, int kind) {
            super(c);
            this.kind = kind;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setColor(Ui.WHITE);
        }

        @Override
        protected void onDraw(Canvas c) {
            float u = getWidth() / 24f;
            stroke.setStrokeWidth(1.8f * u);
            if (kind == WIFI) {
                float cx = 12 * u, cy = 21 * u;
                for (float r : new float[]{13.5f, 9.2f, 4.9f}) {
                    c.drawArc(new RectF(cx - r * u, cy - r * u, cx + r * u, cy + r * u), 225, 90, false, stroke);
                }
                c.drawPoint(cx, 19.5f * u, stroke);
            } else if (kind == PLAY) {
                Path p = new Path();
                p.moveTo(6 * u, 3.5f * u);
                p.lineTo(19 * u, 12 * u);
                p.lineTo(6 * u, 20.5f * u);
                p.close();
                c.drawPath(p, stroke);
            } else {
                float cx = 12 * u, cy = 12 * u;
                c.drawCircle(cx, cy, 3f * u, stroke);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x1 = cx + (float) Math.cos(a) * 6.5f * u, y1 = cy + (float) Math.sin(a) * 6.5f * u;
                    float x2 = cx + (float) Math.cos(a) * 9.5f * u, y2 = cy + (float) Math.sin(a) * 9.5f * u;
                    c.drawLine(x1, y1, x2, y2, stroke);
                }
            }
        }
    }
}
