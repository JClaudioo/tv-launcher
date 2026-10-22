package com.joao.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/**
 * Tela inicial do tablet da criança (Galaxy Tab 2, Android 6): céu com sol e nuvens, hora, bateria
 * e só três botões grandes — Vídeos (NewPipe), Câmera e Fotos (Galeria).
 * O adulto entra segurando a bateria por 5 segundos e respondendo uma conta.
 */
public class MainActivity extends Activity {

    private static final String NEWPIPE = "org.schabi.newpipe";
    private static final String CAMERA = "com.android.camera2";
    private static final String GALLERY = "com.android.gallery3d";
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final int INK = 0xFF2B2340;

    /** Tudo é medido numa tela de 1024x600, igual ao layout aprovado. */
    private float scale = 1f;

    private TextView clock, date, battery;
    private BatteryIcon batteryIcon;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateClock();
        }
    };
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int max = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            int pct = level < 0 || max <= 0 ? 0 : Math.round(level * 100f / max);
            battery.setText(pct + "%" + (charging ? " ⚡" : ""));
            batteryIcon.set(pct);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        scale = Math.max(dm.widthPixels, dm.heightPixels) / 1024f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(build());
    }

    private int px(float v) {
        return Math.round(v * scale);
    }

    private TextView text(float size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale);
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private View build() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new Sky(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // hora e data
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.BOTTOM);
        clock = text(44, true, INK);
        top.addView(clock);
        date = text(20, false, 0xBF2B2340);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.leftMargin = px(14);
        dlp.bottomMargin = px(4);
        top.addView(date, dlp);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tlp.leftMargin = px(40);
        tlp.topMargin = px(26);
        root.addView(top, tlp);

        // bateria: segurar por 5 s abre o acesso do adulto
        LinearLayout bat = new LinearLayout(this);
        bat.setOrientation(LinearLayout.HORIZONTAL);
        bat.setGravity(Gravity.CENTER_VERTICAL);
        bat.setPadding(px(16), px(12), px(16), px(12));
        batteryIcon = new BatteryIcon(this);
        bat.addView(batteryIcon, new LinearLayout.LayoutParams(px(34), px(18)));
        battery = text(20, true, INK);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = px(8);
        bat.addView(battery, blp);
        final Runnable adult = this::askAdult;
        bat.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                handler.postDelayed(adult, 5000);
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(adult);
            }
            return true;
        });
        FrameLayout.LayoutParams brp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        brp.rightMargin = px(24);
        brp.topMargin = px(22);
        root.addView(bat, brp);

        // os três botões grandes
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        String[] names = {"Vídeos", "Câmera", "Fotos"};
        int[] colors = {0xFFE5484D, 0xFF3E8BF0, 0xFFF2A32B};
        for (int i = 0; i < 3; i++) {
            final int kind = i;
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            BigButton bb = new BigButton(this, kind, colors[i]);
            bb.setContentDescription(names[i]);
            bb.setOnClickListener(v -> open(kind));
            col.addView(bb, new LinearLayout.LayoutParams(px(220), px(232)));
            TextView label = text(34, true, INK);
            label.setText(names[i]);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.topMargin = px(10);
            col.addView(label, llp);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) clp.leftMargin = px(44);
            row.addView(col, clp);
        }
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        rlp.topMargin = px(150);
        root.addView(row, rlp);
        return root;
    }

    private void open(int kind) {
        Intent i;
        if (kind == 0) {
            i = getPackageManager().getLaunchIntentForPackage(NEWPIPE);
        } else if (kind == 1) {
            i = getPackageManager().getLaunchIntentForPackage(CAMERA);
            if (i == null) i = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        } else {
            i = getPackageManager().getLaunchIntentForPackage(GALLERY);
        }
        if (i == null) {
            Toast.makeText(this, kind == 0 ? "Instale o NewPipe" : "App não encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "App não encontrado", Toast.LENGTH_LONG).show();
        }
    }

    // ---------- acesso do adulto ----------

    private void askAdult() {
        Random r = new Random();
        final int a = 6 + r.nextInt(4), b = 6 + r.nextInt(4);
        final EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setHint("resposta");
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("Acesso do adulto");
        d.setMessage("Quanto é " + a + " × " + b + "?");
        d.setView(field);
        d.setPositiveButton("Entrar", (dlg, w) -> {
            String s = field.getText().toString().trim();
            if (s.equals(String.valueOf(a * b))) adultMenu();
        });
        d.setNegativeButton("Cancelar", null);
        d.show();
    }

    private void adultMenu() {
        String[] items = {"Wi-Fi", "Configurações"};
        new AlertDialog.Builder(this)
                .setTitle("Adulto")
                .setItems(items, (dlg, which) -> {
                    Intent i = new Intent(which == 0 ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_SETTINGS);
                    // fora da lista de recentes: a criança não volta para as configurações por ali
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException ignored) {
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    // ---------- relógio ----------

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        clock.setText(String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
        date.setText(new SimpleDateFormat("EEEE, d 'de' MMMM", PT_BR).format(now.getTime()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeReceiver, f);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        updateClock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(timeReceiver);
            unregisterReceiver(batteryReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        // é a tela inicial: voltar não sai daqui
    }

    // ---------- desenhos ----------

    /** Fundo: céu, sol, duas nuvens e um gramado. */
    class Sky extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Sky(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float sx = w / 1024f, sy = h / 600f;
            c.drawColor(0xFFBFE7FF);
            p.setColor(0xFFFFD25A);
            c.drawCircle(900 * sx, 96 * sy, 52 * Math.min(sx, sy), p);
            p.setColor(0xD9FFFFFF);
            cloud(c, 120 * sx, 110 * sy, 1.0f * Math.min(sx, sy));
            p.setColor(0xB3FFFFFF);
            cloud(c, 560 * sx, 70 * sy, 0.75f * Math.min(sx, sy));
            p.setColor(0xFF8FD18B);
            path.reset();
            path.moveTo(0, 520 * sy);
            path.cubicTo(200 * sx, 490 * sy, 380 * sx, 540 * sy, 560 * sx, 515 * sy);
            path.cubicTo(740 * sx, 490 * sy, 880 * sx, 490 * sy, w, 520 * sy);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            c.drawPath(path, p);
        }

        private void cloud(Canvas c, float x, float y, float k) {
            c.drawCircle(x + 30 * k, y - 22 * k, 30 * k, p);
            c.drawCircle(x + 70 * k, y - 38 * k, 40 * k, p);
            c.drawCircle(x + 110 * k, y - 22 * k, 28 * k, p);
            c.drawRoundRect(new RectF(x, y - 26 * k, x + 140 * k, y), 13 * k, 13 * k, p);
        }
    }

    /** Botão grande, arredondado e colorido, com ícone branco; afunda um pouco ao tocar. */
    class BigButton extends View {
        private final int kind, color;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean pressed;

        BigButton(Context c, int kind, int color) {
            super(c);
            this.kind = kind;
            this.color = color;
            setClickable(true);
            fill.setColor(color);
            shadow.setColor(darker(color));
            icon.setColor(Color.WHITE);
            icon.setStyle(Paint.Style.STROKE);
            icon.setStrokeCap(Paint.Cap.ROUND);
            icon.setStrokeJoin(Paint.Join.ROUND);
        }

        private int darker(int c) {
            return Color.rgb(Color.red(c) * 3 / 4, Color.green(c) * 3 / 4, Color.blue(c) * 3 / 4);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                pressed = true;
                invalidate();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed = false;
                invalidate();
            }
            return super.onTouchEvent(e);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float side = w;
            float r = side * 0.25f;
            float drop = side * 0.055f;
            float off = pressed ? drop * 0.8f : 0;
            c.drawRoundRect(new RectF(0, drop, side, side + drop), r, r, shadow);
            c.drawRoundRect(new RectF(0, off, side, side + off), r, r, fill);
            float u = side / 24f * 0.5f;
            float ox = side / 2f - 12 * u, oy = side / 2f - 12 * u + off;
            icon.setStrokeWidth(1.8f * u);
            c.save();
            c.translate(ox, oy);
            Path p = new Path();
            if (kind == 0) {
                p.moveTo(8 * u, 5.5f * u);
                p.lineTo(19 * u, 12 * u);
                p.lineTo(8 * u, 18.5f * u);
                p.close();
                icon.setStyle(Paint.Style.FILL_AND_STROKE);
                c.drawPath(p, icon);
                icon.setStyle(Paint.Style.STROKE);
            } else if (kind == 1) {
                p.moveTo(4 * u, 8 * u);
                p.lineTo(7 * u, 8 * u);
                p.lineTo(9 * u, 5 * u);
                p.lineTo(15 * u, 5 * u);
                p.lineTo(17 * u, 8 * u);
                p.lineTo(20 * u, 8 * u);
                p.lineTo(20 * u, 19 * u);
                p.lineTo(4 * u, 19 * u);
                p.close();
                c.drawPath(p, icon);
                c.drawCircle(12 * u, 13.5f * u, 3.5f * u, icon);
            } else {
                c.drawRoundRect(new RectF(3 * u, 5 * u, 21 * u, 19 * u), 2 * u, 2 * u, icon);
                c.drawCircle(8.5f * u, 8.5f * u, 1.5f * u, icon);
                p.moveTo(21 * u, 15 * u);
                p.lineTo(16 * u, 10 * u);
                p.lineTo(5 * u, 19 * u);
                c.drawPath(p, icon);
            }
            c.restore();
        }
    }

    /** Ícone da bateria: verde, ou vermelho com 20% ou menos. */
    static class BatteryIcon extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int pct = 0;

        BatteryIcon(Context c) {
            super(c);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(INK);
            fill.setColor(INK);
        }

        void set(int pct) {
            this.pct = pct;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float u = w / 34f;
            stroke.setStrokeWidth(2 * u);
            c.drawRoundRect(new RectF(1 * u, 1 * u, 29 * u, h - 1 * u), 4 * u, 4 * u, stroke);
            fill.setColor(INK);
            c.drawRoundRect(new RectF(30 * u, h / 2 - 3 * u, 33 * u, h / 2 + 3 * u), 1 * u, 1 * u, fill);
            fill.setColor(pct <= 20 ? 0xFFE5484D : 0xFF3BB273);
            c.drawRoundRect(new RectF(4 * u, 4 * u, (4 + 22 * pct / 100f) * u, h - 4 * u), 2 * u, 2 * u, fill);
        }
    }
}
