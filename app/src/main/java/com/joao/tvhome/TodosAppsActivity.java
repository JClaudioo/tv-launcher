package com.joao.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Grade com todos os apps instalados, no mesmo visual da tela inicial. */
public class TodosAppsActivity extends Activity {
    private static final int COLS = 6;
    private static final float TILE = 120f;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);

        FrameLayout root = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImageResource(R.drawable.fundo);
        root.addView(bg, match());
        View dim = new View(this);
        dim.setBackgroundColor(0xD9000000);
        root.addView(dim, match());

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        root.addView(scroll, match());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setPadding(Ui.px(112), Ui.px(90), Ui.px(112), Ui.px(90));
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<Apps.Entry> apps = Apps.all(this);

        TextView kicker = Ui.text(this, 26, 400, Ui.ACCENT);
        kicker.setLetterSpacing(0.18f);
        kicker.setText("TODOS OS APPS");
        LinearLayout.LayoutParams klp = wrap();
                content.addView(kicker, klp);

        TextView count = Ui.text(this, 22, 300, 0xADFFFFFF);
        count.setText(apps.size() + (apps.size() == 1 ? " app instalado" : " apps instalados"));
        LinearLayout.LayoutParams clp = wrap();
                clp.topMargin = Ui.px(10);
        clp.bottomMargin = Ui.px(20);
        content.addView(count, clp);

        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rowlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowlp.topMargin = Ui.px(44);
                content.addView(row, rowlp);
            }
            final Apps.Entry e = apps.get(i);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setClipChildren(false);

            final TileView tile = new TileView(this, Apps.icon(this, e.pkg), TILE);
            tile.setContentDescription(e.label);
            cell.addView(tile);

            final TextView label = Ui.text(this, 22, 300, 0xD9FFFFFF);
            label.setText(e.label);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            label.setMaxWidth(Ui.px(210));
            LinearLayout.LayoutParams llp = wrap();
            llp.topMargin = Ui.px(18);
            cell.addView(label, llp);

            tile.setOnFocusChangeListener((v, has) -> {
                tile.setFocusedLook(has);
                label.setTextColor(has ? Ui.ACCENT : 0xD9FFFFFF);
            });
            tile.setOnClickListener(v -> Apps.launch(this, e.pkg));
            if (first == null) first = tile;

            row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        // completa a última linha para as colunas ficarem alinhadas
        if (row != null) {
            int rest = (COLS - apps.size() % COLS) % COLS;
            for (int i = 0; i < rest; i++) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }

        setContentView(root);
        if (first != null) first.requestFocus();
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
