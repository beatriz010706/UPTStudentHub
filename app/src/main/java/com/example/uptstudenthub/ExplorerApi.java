package com.example.uptstudenthub;

import android.net.Uri;

/**
 * Constrói TODOS os URLs do Módulo 3 (V1, previsão semanal) com Uri.Builder.
 * Nenhum URL é escrito fora desta classe (RT4).
 */
public final class ExplorerApi {

    private static final String HOST = "api.open-meteo.com";
    private static final String TIMEZONE = "Europe/Lisbon";
    private static final String ENDPOINT = "forecast";
    private static final String ENDPOINT_BROKEN = "forecast-does-not-exist";

    private ExplorerApi() { }

    /**
     * Pedido A: previsão diária.
     *
     * @param place local
     * @param days  número de dias (3, 5 ou 7)
     * @param unit  "celsius" ou "fahrenheit"
     * @return URL completo
     */
    public static String dailyUrl(Place place, int days, String unit) {
        return daily(place, days, unit, ENDPOINT);
    }

    /** Igual ao pedido A, mas para um caminho inexistente: o servidor responde com erro HTTP. */
    public static String brokenDailyUrl(Place place, int days, String unit) {
        return daily(place, days, unit, ENDPOINT_BROKEN);
    }

    /**
     * Pedido B: previsão hora a hora de um dia.
     *
     * @param date dia no formato yyyy-MM-dd
     */
    public static String hourlyUrl(Place place, String date, String unit) {
        return base(place, unit, ENDPOINT)
                .appendQueryParameter("hourly",
                        "temperature_2m,precipitation_probability,weather_code")
                .appendQueryParameter("start_date", date)
                .appendQueryParameter("end_date", date)
                .build().toString();
    }

    private static String daily(Place place, int days, String unit, String endpoint) {
        return base(place, unit, endpoint)
                .appendQueryParameter("daily",
                        "weather_code,temperature_2m_max,temperature_2m_min,"
                                + "precipitation_probability_max")
                .appendQueryParameter("forecast_days", String.valueOf(days))
                .build().toString();
    }

    /** Parte comum: esquema, host, caminho e parâmetros partilhados. */
    private static Uri.Builder base(Place place, String unit, String endpoint) {
        return new Uri.Builder()
                .scheme("https")
                .authority(HOST)
                .appendPath("v1")
                .appendPath(endpoint)
                .appendQueryParameter("latitude", String.valueOf(place.latitude))
                .appendQueryParameter("longitude", String.valueOf(place.longitude))
                .appendQueryParameter("temperature_unit", unit)
                .appendQueryParameter("timezone", TIMEZONE);
    }
}
