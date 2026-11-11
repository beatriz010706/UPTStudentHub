package com.example.uptstudenthub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converte as respostas JSON da Open-Meteo em modelos imutáveis (RT5). */
public final class ExplorerParser {

    private ExplorerParser() { }

    /**
     * Converte a resposta do pedido A.
     *
     * @return lista imutável (vazia se não houver dias)
     * @throws DataException se o JSON for inválido ou faltar o objeto "daily"
     */
    public static List<DailyForecast> parseDaily(String json) throws DataException {
        try {
            JSONObject daily = new JSONObject(json).optJSONObject("daily");
            if (daily == null) {
                throw new DataException("Falta o objeto 'daily'");
            }
            JSONArray time = daily.optJSONArray("time");
            if (time == null) {
                return Collections.emptyList();
            }
            // Arrays paralelos: o índice i de cada array pertence ao mesmo dia.
            JSONArray codes = daily.optJSONArray("weather_code");
            JSONArray max = daily.optJSONArray("temperature_2m_max");
            JSONArray min = daily.optJSONArray("temperature_2m_min");
            JSONArray rain = daily.optJSONArray("precipitation_probability_max");

            List<DailyForecast> result = new ArrayList<>();
            for (int i = 0; i < time.length(); i++) {
                result.add(new DailyForecast(
                        time.getString(i),
                        intAt(codes, i),
                        doubleAt(max, i),
                        doubleAt(min, i),
                        intAt(rain, i)));
            }
            return Collections.unmodifiableList(result);
        } catch (JSONException e) {
            throw new DataException("JSON inválido", e);
        }
    }

    /**
     * Converte a resposta do pedido B (horas de um dia).
     *
     * @throws DataException se o JSON for inválido ou faltar o objeto "hourly"
     */
    public static List<HourlyForecast> parseHourly(String json) throws DataException {
        try {
            JSONObject hourly = new JSONObject(json).optJSONObject("hourly");
            if (hourly == null) {
                throw new DataException("Falta o objeto 'hourly'");
            }
            JSONArray time = hourly.optJSONArray("time");
            if (time == null) {
                return Collections.emptyList();
            }
            JSONArray temp = hourly.optJSONArray("temperature_2m");
            JSONArray rain = hourly.optJSONArray("precipitation_probability");
            JSONArray codes = hourly.optJSONArray("weather_code");

            List<HourlyForecast> result = new ArrayList<>();
            for (int i = 0; i < time.length(); i++) {
                result.add(new HourlyForecast(
                        time.getString(i),
                        doubleAt(temp, i),
                        intAt(rain, i),
                        intAt(codes, i)));
            }
            return Collections.unmodifiableList(result);
        } catch (JSONException e) {
            throw new DataException("JSON inválido", e);
        }
    }

    /** Devolve o double na posição i, ou null se o array ou o valor não existir. */
    private static Double doubleAt(JSONArray array, int i) {
        if (array == null || array.isNull(i)) {
            return null;
        }
        return array.optDouble(i);
    }

    /** Devolve o inteiro na posição i, ou null se o array ou o valor não existir. */
    private static Integer intAt(JSONArray array, int i) {
        if (array == null || array.isNull(i)) {
            return null;
        }
        return array.optInt(i);
    }
}
