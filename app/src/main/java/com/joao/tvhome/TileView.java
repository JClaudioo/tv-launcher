package com.joao.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Bloco de app arredondado. A view tem uma folga em volta (PAD) para o brilho vermelho
 * não ser cortado quando o bloco está em foco.
 */
class TileView extends View {
    static final float PAD = 36f;

    private final float tile;
    private final Drawable icon;
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF box = new RectF();
    private boolean focused;

    TileView(Context c, Drawable icon, float tileSize) {
        super(c);
        this.icon = icon;
        this.tile = tileSize;
        setLayerType(LAYER_TYPE_SOFTWARE, null); // necessário para a sombra/brilho
        setFocusable(true);
        setClickable(true);
        base.setColor(0xFF161618);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(Ui.ACCENT);
        ring.setStrokeWidth(Ui.pxf(4));
    }

    int sizePx() {
        return Ui.px(tile + PAD * 2);
    }

    void setFocusedLook(boolean f) {
        focused = f;
        animate().scaleX(f ? 1.18f : 1f).scaleY(f ? 1.18f : 1f).setDuration(180).start();
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int s = sizePx();
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pad = Ui.pxf(PAD);
        float r = Ui.pxf(tile * 0.24f);
        box.set(pad, pad, getWidth() - pad, getHeight() - pad);

        if (focused) {
            base.setShadowLayer(Ui.pxf(30), 0, Ui.pxf(8), Ui.alpha(Ui.ACCENT, 0.6f));
        } else {
            base.setShadowLayer(Ui.pxf(18), 0, Ui.pxf(8), 0x73000000);
        }
        canvas.drawRoundRect(box, r, r, base);

        clip.reset();
        clip.addRoundRect(box, r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        drawIcon(canvas);
        canvas.restore();

        if (focused) {
            float half = ring.getStrokeWidth() / 2f;
            RectF o = new RectF(box.left - half, box.top - half, box.right + half, box.bottom + half);
            canvas.drawRoundRect(o, r + half, r + half, ring);
        }
    }

    private void drawIcon(Canvas canvas) {
        if (icon == null) return;
        Rect b = new Rect(Math.round(box.left), Math.round(box.top), Math.round(box.right), Math.round(box.bottom));
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
