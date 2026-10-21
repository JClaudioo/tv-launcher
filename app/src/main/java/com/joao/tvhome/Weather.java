package com.joao.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Clima de Ceres-GO pelo Open-Meteo (gratuito, sem cadastro). Guarda a última leitura para funcionar offline. */
final class Weather {
    static final String CITY = "Ceres, GO";
    private static final String URL_API = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=-15.3061&longitude=-49.5972"
            + "&current=temperature_2m,weather_code,is_day"
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code"
            + "&timezone=America%2FSao_Paulo&forecast_days=6";
    static final long REFRESH_MS = 30 * 60 * 1000L;

    static final class Data {
        int temp, max, min, rain, code;
        boolean day;
        long fetchedAt;
        /** Previsão dos próximos dias (posição 0 = amanhã). Vazia se a leitura guardada for antiga. */
        String[] fDate = new String[0];
        int[] fMax = new int[0], fMin = new int[0], fRain = new int[0], fCode = new int[0];
    }

    interface Callback {
        void onWeather(Data d);
    }

    private Weather() {}

    static Data cached(Context c) {
        SharedPreferences p = prefs(c);
        String json = p.getString("json", null);
        if (json == null) return null;
        try {
            Data d = parse(json);
            d.fetchedAt = p.getLong("at", 0);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    static void fetch(Context c, Callback cb) {
        final Context app = c.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(URL_API).openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                if (con.getResponseCode() != 200) return;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                final Data d = parse(sb.toString());
                d.fetchedAt = System.currentTimeMillis();
                prefs(app).edit().putString("json", sb.toString()).putLong("at", d.fetchedAt).apply();
                main.post(() -> cb.onWeather(d));
            } catch (Exception ignored) {
                // sem internet: fica com a última leitura
            } finally {
                if (con != null) con.disconnect();
            }
        }, "clima").start();
    }

    private static Data parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        JSONObject cur = o.getJSONObject("current");
        JSONObject daily = o.getJSONObject("daily");
        Data d = new Data();
        d.temp = (int) Math.round(cur.getDouble("temperature_2m"));
        d.code = cur.getInt("weather_code");
        d.day = cur.optInt("is_day", 1) == 1;
        d.max = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0));
        d.min = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0));
        d.rain = daily.getJSONArray("precipitation_probability_max").optInt(0, 0);
        org.json.JSONArray dates = daily.optJSONArray("time");
        org.json.JSONArray codes = daily.optJSONArray("weather_code");
        int n = dates == null ? 0 : Math.max(0, dates.length() - 1);
        d.fDate = new String[n];
        d.fMax = new int[n];
        d.fMin = new int[n];
        d.fRain = new int[n];
        d.fCode = new int[n];
        for (int i = 0; i < n; i++) {
            d.fDate[i] = dates.optString(i + 1, "");
            d.fMax[i] = (int) Math.round(daily.getJSONArray("temperature_2m_max").optDouble(i + 1, 0));
            d.fMin[i] = (int) Math.round(daily.getJSONArray("temperature_2m_min").optDouble(i + 1, 0));
            d.fRain[i] = daily.getJSONArray("precipitation_probability_max").optInt(i + 1, 0);
            d.fCode[i] = codes == null ? 3 : codes.optInt(i + 1, 3);
        }
        return d;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("clima", Context.MODE_PRIVATE);
    }

    /** Códigos WMO usados pelo Open-Meteo. */
    static String describe(int code) {
        switch (code) {
            case 0: return "Céu limpo";
            case 1: return "Predomínio de sol";
            case 2: return "Parcialmente nublado";
            case 3: return "Nublado";
            case 45: case 48: return "Neblina";
            case 51: case 53: case 55: return "Garoa";
            case 56: case 57: return "Garoa gelada";
            case 61: return "Chuva fraca";
            case 63: return "Chuva";
            case 65: return "Chuva forte";
            case 66: case 67: return "Chuva gelada";
            case 71: case 73: case 75: case 77: return "Neve";
            case 80: return "Pancadas fracas";
            case 81: return "Pancadas de chuva";
            case 82: return "Pancadas fortes";
            case 95: return "Tempestade";
            case 96: case 99: return "Tempestade com granizo";
            default: return "—";
        }
    }

    static int icon(int code) {
        if (code == 0) return WeatherIconView.CLEAR;
        if (code == 1 || code == 2) return WeatherIconView.PARTLY;
        if (code == 3) return WeatherIconView.CLOUDY;
        if (code == 45 || code == 48) return WeatherIconView.FOG;
        if (code >= 95) return WeatherIconView.STORM;
        if (code >= 51) return WeatherIconView.RAIN;
        return WeatherIconView.CLOUDY;
    }
}
