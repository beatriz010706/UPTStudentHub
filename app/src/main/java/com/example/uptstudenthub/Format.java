package com.example.uptstudenthub;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Funções de formatação partilhadas pelos ecrãs (todos os textos vêm de strings.xml). */
final class Format {

    private Format() { }

    /** "2026-11-10" -> "Tue, 10 Nov" (na língua do dispositivo). */
    static String day(String iso) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(iso);
            return new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(date);
        } catch (ParseException | NullPointerException e) {
            return iso;
        }
    }

    /** "2026-11-10T14:00" -> "14:00". */
    static String hour(String iso) {
        int t = iso.indexOf('T');
        return t >= 0 ? iso.substring(t + 1) : iso;
    }

    /** Temperatura com unidade, ou "—" se não existir. */
    static String temperature(Context context, Double value, boolean celsius) {
        if (value == null) {
            return context.getString(R.string.ex_value_missing);
        }
        return context.getString(celsius ? R.string.ex_temp_c : R.string.ex_temp_f, value);
    }

    /** "Rain: 40%" ou "Rain: —". */
    static String rain(Context context, Integer percent) {
        if (percent == null) {
            return context.getString(R.string.ex_rain_missing);
        }
        return context.getString(R.string.ex_row_rain, percent);
    }

    /** Mensagem de erro precisa para o utilizador (RF16). */
    static String error(Context context, ErrorType type, int httpCode) {
        if (type == null) {
            return context.getString(R.string.err_no_connection);
        }
        switch (type) {
            case TIMEOUT:
                return context.getString(R.string.err_timeout);
            case HTTP:
                return context.getString(R.string.err_http, httpCode);
            case PARSE:
                return context.getString(R.string.err_parse);
            case EMPTY:
                return context.getString(R.string.ex_err_empty);
            default:
                return context.getString(R.string.err_no_connection);
        }
    }
}
