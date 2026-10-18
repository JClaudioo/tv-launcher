package com.joao.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/** Escurecido em degradê na metade de baixo, para o texto e os ícones ficarem legíveis sobre a foto. */
class ShadeView extends View {
    private final Paint paint = new Paint();
    private final float strength;

    ShadeView(Context c, float strength) {
        super(c);
        this.strength = strength;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        int mid = Math.round(strength * 0.55f * 255) << 24;
        int end = Math.round(strength * 255) << 24;
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0, 0, mid, end},
                new float[]{0f, 0.36f, 0.56f, 1f},
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
