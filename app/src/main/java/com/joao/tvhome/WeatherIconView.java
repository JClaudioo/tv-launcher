package com.joao.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Ícone do clima em traço fino: sol/lua em vermelho, nuvens em branco. */
class WeatherIconView extends View {
    static final int CLEAR = 0, PARTLY = 1, CLOUDY = 2, RAIN = 3, STORM = 4, FOG = 5;

    private int type = PARTLY;
    private boolean day = true;
    private final Paint red = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float size;

    WeatherIconView(Context c) {
        this(c, 44);
    }

    /** Tamanho em px de uma tela 1920x1080. */
    WeatherIconView(Context c, float size) {
        super(c);
        this.size = size;
        for (Paint p : new Paint[]{red, white}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        red.setColor(Ui.ACCENT);
        white.setColor(Ui.WHITE);
    }

    void set(int type, boolean day) {
        this.type = type;
        this.day = day;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int s = Ui.px(size);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas c) {
        float u = getWidth() / 24f;
        red.setStrokeWidth(1.5f * u);
        white.setStrokeWidth(1.5f * u);
        switch (type) {
            case CLEAR:
                if (day) sun(c, u, 12, 12, 4.2f, true); else moon(c, u, 12, 12, 6f);
                break;
            case PARTLY:
                if (day) sun(c, u, 9, 9, 3.2f, true); else moon(c, u, 9, 8.5f, 4.2f);
                cloud(c, u);
                break;
            case CLOUDY:
                cloud(c, u);
                break;
            case RAIN:
                cloud(c, u);
                for (float x : new float[]{12, 15.5f, 19}) {
                    c.drawLine(x * u, 21.5f * u, (x - 1) * u, 23.5f * u, red);
                }
                break;
            case STORM:
                cloud(c, u);
                Path bolt = new Path();
                bolt.moveTo(15.5f * u, 20.5f * u);
                bolt.lineTo(13.5f * u, 23f * u);
                bolt.lineTo(16f * u, 23f * u);
                bolt.lineTo(14f * u, 24f * u);
                c.drawPath(bolt, red);
                break;
            default:
                for (int i = 0; i < 4; i++) {
                    float y = (8 + i * 3.5f) * u;
                    c.drawLine((4 + (i % 2) * 2) * u, y, (20 - (i % 2) * 2) * u, y, i % 2 == 0 ? white : red);
                }
        }
    }

    private void sun(Canvas c, float u, float cx, float cy, float r, boolean rays) {
        c.drawCircle(cx * u, cy * u, r * u, red);
        if (!rays) return;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float r1 = r + 2.2f, r2 = r + 3.5f;
            c.drawLine((cx + (float) Math.cos(a) * r1) * u, (cy + (float) Math.sin(a) * r1) * u,
                    (cx + (float) Math.cos(a) * r2) * u, (cy + (float) Math.sin(a) * r2) * u, red);
        }
    }

    private void moon(Canvas c, float u, float cx, float cy, float r) {
        Path p = new Path();
        RectF o = new RectF((cx - r) * u, (cy - r) * u, (cx + r) * u, (cy + r) * u);
        p.addArc(o, 60, 260);
        RectF in = new RectF((cx - r * 0.2f) * u, (cy - r * 0.95f) * u, (cx + r * 1.5f) * u, (cy + r * 0.85f) * u);
        p.arcTo(in, 290, -230);
        c.drawPath(p, red);
    }

    private void cloud(Canvas c, float u) {
        Path p = new Path();
        p.moveTo(10.5f * u, 20f * u);
        p.lineTo(18.5f * u, 20f * u);
        p.arcTo(new RectF(15f * u, 13f * u, 22f * u, 20f * u), 90, -180);
        p.arcTo(new RectF(10.2f * u, 9.8f * u, 19.2f * u, 18.8f * u), -40, -150);
        p.arcTo(new RectF(7.6f * u, 14.2f * u, 13.4f * u, 20f * u), 230, -140);
        p.close();
        c.drawPath(p, white);
    }
}
