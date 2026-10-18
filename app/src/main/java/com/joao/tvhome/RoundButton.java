package com.joao.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Botão redondo com ícone desenhado: todos os apps, Wi-Fi ou configurações. */
class RoundButton extends View {
    static final int APPS = 0, WIFI = 1, SETTINGS = 2;
    private static final float SIZE = 76f;

    private final int kind;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean focused;

    RoundButton(Context c, int kind, String label) {
        super(c);
        this.kind = kind;
        setFocusable(true);
        setClickable(true);
        setContentDescription(label);
        border.setStyle(Paint.Style.STROKE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(Ui.WHITE);
    }

    void setFocusedLook(boolean f) {
        focused = f;
        animate().scaleX(f ? 1.12f : 1f).scaleY(f ? 1.12f : 1f).setDuration(160).start();
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int s = Ui.px(SIZE);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float cx = w / 2f;
        float bw = focused ? Ui.pxf(3) : Ui.pxf(1.5f);
        fill.setColor(focused ? Ui.alpha(Ui.ACCENT, 0.35f) : 0x8C0E0A0C);
        border.setColor(focused ? Ui.ACCENT : Ui.alpha(Ui.ACCENT, 0.55f));
        border.setStrokeWidth(bw);
        c.drawCircle(cx, cx, cx - bw, fill);
        c.drawCircle(cx, cx, cx - bw, border);

        // ícone num quadro de 24 unidades, centralizado
        float iconSize = Ui.pxf(kind == APPS ? 30 : 32);
        float u = iconSize / 24f;
        c.save();
        c.translate(cx - iconSize / 2f, cx - iconSize / 2f);
        stroke.setStrokeWidth(1.8f * u);
        if (kind == APPS) drawApps(c, u);
        else if (kind == WIFI) drawWifi(c, u);
        else drawGear(c, u);
        c.restore();
    }

    private void drawApps(Canvas c, float u) {
        float[][] p = {{4, 4}, {14, 4}, {4, 14}, {14, 14}};
        for (float[] q : p) {
            c.drawRoundRect(new RectF(q[0] * u, q[1] * u, (q[0] + 6) * u, (q[1] + 6) * u), 1.5f * u, 1.5f * u, stroke);
        }
    }

    private void drawWifi(Canvas c, float u) {
        float cx = 12 * u, cy = 21 * u;
        float[] radii = {13.5f, 9.2f, 4.9f};
        for (float r : radii) {
            RectF o = new RectF(cx - r * u, cy - r * u, cx + r * u, cy + r * u);
            c.drawArc(o, 225, 90, false, stroke);
        }
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(Ui.WHITE);
        c.drawCircle(cx, 19.5f * u, 1.2f * u, dot);
    }

    private void drawGear(Canvas c, float u) {
        float cx = 12 * u, cy = 12 * u;
        Path p = new Path();
        int teeth = 8;
        float rOut = 9.6f * u, rIn = 7.4f * u;
        for (int i = 0; i < teeth * 4; i++) {
            double a = Math.PI * 2 * i / (teeth * 4) - Math.PI / 2;
            float r = (i % 4 == 0 || i % 4 == 1) ? rOut : rIn;
            float x = cx + (float) Math.cos(a) * r;
            float y = cy + (float) Math.sin(a) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        c.drawPath(p, stroke);
        c.drawCircle(cx, cy, 3f * u, stroke);
    }
}
