package com.joao.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Pedidos que chegam do controle no celular:
 * tvlauncher://abrir?app=com.netflix.ninja  abre o app
 * tvlauncher://apps                         abre a lista de todos os apps
 * Não mostra nada na tela: faz o pedido e fecha.
 */
public class AbrirActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        handle(getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handle(intent);
        finish();
    }

    private void handle(Intent intent) {
        Uri u = intent == null ? null : intent.getData();
        if (u == null) return;
        String host = u.getHost() == null ? "" : u.getHost();
        if (host.equals("apps")) {
            startActivity(new Intent(this, TodosAppsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (host.equals("abrir")) {
            String pkg = u.getQueryParameter("app");
            if (pkg != null && !pkg.isEmpty()) Apps.launch(this, pkg);
        } else if (host.equals("inicio")) {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
