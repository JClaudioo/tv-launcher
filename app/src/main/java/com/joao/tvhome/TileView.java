package com.joao.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Bloco de app arredondado, com o tamanho exato do ícone.
 * O brilho vermelho do foco é a sombra nativa do Android (elevação), desenhada fora do bloco pelo sistema.
 */
class TileView extends View {
    private final float tile;
    private final Drawable icon;
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean focused;

    TileView(Context c, Drawable icon, float tileSize) {
        super(c);
        this.icon = icon;
        this.tile = tileSize;
        setFocusable(true);
        setClickable(true);
        base.setColor(0xFF161618);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(Ui.ACCENT);
        ring.setStrokeWidth(Ui.pxf(4));

        final float radius = Ui.pxf(tileSize * 0.24f);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        setClipToOutline(true);
        applyShadow();
    }

    void setFocusedLook(boolean f) {
        focused = f;
        applyShadow();
        animate().scaleX(f ? 1.18f : 1f).scaleY(f ? 1.18f : 1f)
                .translationZ(f ? Ui.pxf(10) : 0f).setDuration(180).start();
        invalidate();
    }

    private void applyShadow() {
        setElevation(focused ? Ui.pxf(26) : Ui.pxf(8));
        if (Build.VERSION.SDK_INT >= 28) {
            int c = focused ? Ui.ACCENT : 0xFF000000;
            setOutlineAmbientShadowColor(c);
            setOutlineSpotShadowColor(c);
        }
    }

    @Override
    protected void onMeasure(int w, int h) {
        int s = Ui.px(tile);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = Ui.pxf(tile * 0.24f);
        RectF box = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawRect(box, base);
        drawIcon(canvas);
        if (focused) {
            float half = ring.getStrokeWidth() / 2f;
            RectF in = new RectF(half, half, getWidth() - half, getHeight() - half);
            canvas.drawRoundRect(in, r - half, r - half, ring);
        }
    }

    private void drawIcon(Canvas canvas) {
        if (icon == null) return;
        Rect b = new Rect(0, 0, getWidth(), getHeight());
        if (icon instanceof AdaptiveIconDrawable) {
            // Ícones adaptativos têm 108 unidades com 72 visíveis: amplia para preencher o bloco.
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) icon;
            int extra = Math.round(b.width() * 0.25f);
            Rect big = new Rect(b.left - extra, b.top - extra, b.right + extra, b.bottom + extra);
            Drawable bg = a.getBackground();
            Drawable fg = a.getForeground();
            if (bg != null) { bg.setBounds(big); bg.draw(canvas); }
            if (fg != null) { fg.setBounds(big); fg.draw(canvas); }
        } else {
            icon.setBounds(b);
            icon.draw(canvas);
        }
    }
}
