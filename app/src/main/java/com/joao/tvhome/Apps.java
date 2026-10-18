package com.joao.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Utilidades para listar, desenhar e abrir apps. */
final class Apps {
    static final class Entry {
        final String pkg;
        final String label;
        Entry(String pkg, String label) { this.pkg = pkg; this.label = label; }
    }

    private Apps() {}

    static boolean installed(Context c, String pkg) {
        try {
            c.getPackageManager().getApplicationInfo(pkg, 0);
            return launchIntent(c, pkg) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static Drawable icon(Context c, String pkg) {
        try {
            return c.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    static Intent launchIntent(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i;
    }

    static void launch(Context c, String pkg) {
        Intent i = launchIntent(c, pkg);
        if (i == null) {
            Toast.makeText(c, "Não foi possível abrir este app", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(c, "Não foi possível abrir este app", Toast.LENGTH_SHORT).show();
        }
    }

    /** Todos os apps que aparecem numa TV (Leanback) ou num celular (Launcher), sem repetir, em ordem alfabética. */
    static List<Entry> all(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, Entry> map = new LinkedHashMap<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(c.getPackageName()) || map.containsKey(pkg)) continue;
                CharSequence label = ri.activityInfo.applicationInfo.loadLabel(pm);
                map.put(pkg, new Entry(pkg, label == null ? pkg : label.toString()));
            }
        }
        List<Entry> list = new ArrayList<>(map.values());
        final Collator col = Collator.getInstance(new Locale("pt", "BR"));
        list.sort((a, b) -> col.compare(a.label, b.label));
        return list;
    }
}
