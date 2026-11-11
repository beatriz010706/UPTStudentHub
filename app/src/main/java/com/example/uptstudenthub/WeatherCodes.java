package com.example.uptstudenthub;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
/**
 * Maps a WMO weather code (number sent by the server) to a string resource.
 * Returns a resource ID, not text: no Context is needed and all text stays in strings.xml.
 */
public final class WeatherCodes {
    private WeatherCodes() { }
    @StringRes
    public static int describe(int code) {
        if (code == 0) return R.string.wmo_clear;
        if (code == 1) return R.string.wmo_mainly_clear;
        if (code == 2) return R.string.wmo_partly_cloudy;
        if (code == 3) return R.string.wmo_overcast;
        if (code == 45 || code == 48) return R.string.wmo_fog;
        if (code >= 51 && code <= 57) return R.string.wmo_drizzle;
        if (code >= 61 && code <= 67) return R.string.wmo_rain;
        if (code >= 71 && code <= 77) return R.string.wmo_snow;
        if (code >= 80 && code <= 82) return R.string.wmo_showers;
        if (code == 85 || code == 86) return R.string.wmo_snow_showers;
        if (code >= 95 && code <= 99) return R.string.wmo_thunderstorm;
        return R.string.wmo_unknown;
    }

    /** Texto do código WMO, ou "desconhecido" se o código for null (Módulo 3). */
    @StringRes
    public static int describeOrUnknown(Integer code) {
        return code == null ? R.string.wmo_unknown : describe(code);
    }

    /** Ícone vetorial que muda com o código WMO (Módulo 3, RF2). */
    @DrawableRes
    public static int iconFor(Integer code) {
        if (code == null) return R.drawable.ic_wmo_unknown;
        int c = code;
        if (c == 0 || c == 1) return R.drawable.ic_wmo_clear;
        if (c == 2 || c == 3) return R.drawable.ic_wmo_cloud;
        if (c == 45 || c == 48) return R.drawable.ic_wmo_fog;
        if ((c >= 51 && c <= 67) || (c >= 80 && c <= 82)) return R.drawable.ic_wmo_rain;
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return R.drawable.ic_wmo_snow;
        if (c >= 95 && c <= 99) return R.drawable.ic_wmo_storm;
        return R.drawable.ic_wmo_unknown;
    }
}
