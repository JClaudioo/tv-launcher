package com.joao.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.widget.TextView;

/** Medidas, cores e fonte. Tudo é desenhado na escala de uma tela 1920x1080, igual ao layout aprovado. */
final class Ui {
    static final int ACCENT = 0xFFE0483A;
    static final int WHITE = 0xFFFFFFFF;

    private static float scale = 1f;
    private static final SparseArray<Typeface> FONTS = new SparseArray<>();

    private Ui() {}

    static void init(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        scale = Math.max(dm.widthPixels, dm.heightPixels) / 1920f;
    }

    static int px(float v) {
        return Math.round(v * scale);
    }

    static float pxf(float v) {
        return v * scale;
    }

    static int alpha(int color, float a) {
        return (Math.round(a * 255) << 24) | (color & 0x00FFFFFF);
    }

    static Typeface font(Context c, int weight) {
        Typeface t = FONTS.get(weight);
        if (t == null) {
            try {
                t = new Typeface.Builder(c.getAssets(), "fonts/outfit.ttf")
                        .setFontVariationSettings("'wght' " + weight)
                        .build();
            } catch (Exception e) {
                t = Typeface.create("sans-serif-light", Typeface.NORMAL);
            }
            FONTS.put(weight, t);
        }
        return t;
    }

    static TextView text(Context c, float sizePx, int weight, int color) {
        TextView tv = new TextView(c);
        tv.setTypeface(font(c, weight));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, pxf(sizePx));
        tv.setTextColor(color);
        tv.setIncludeFontPadding(false);
        return tv;
    }
}
