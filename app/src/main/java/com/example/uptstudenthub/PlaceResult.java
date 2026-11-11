package com.example.uptstudenthub;

/** Resultado da comparação para um local: dados OU erro (modelo imutável). */
public final class PlaceResult {

    /** Local a que o resultado se refere. */
    public final Place place;
    /** Previsão de hoje (null se falhou). */
    public final DailyForecast today;
    /** Tipo de erro (null se correu bem). */
    public final ErrorType error;
    /** Código HTTP quando error == HTTP. */
    public final int httpCode;

    private PlaceResult(Place place, DailyForecast today, ErrorType error, int httpCode) {
        this.place = place;
        this.today = today;
        this.error = error;
        this.httpCode = httpCode;
    }

    /** Cria um resultado com dados. */
    public static PlaceResult ok(Place place, DailyForecast today) {
        return new PlaceResult(place, today, null, 0);
    }

    /** Cria um resultado com erro. */
    public static PlaceResult failed(Place place, ErrorType error, int httpCode) {
        return new PlaceResult(place, null, error, httpCode);
    }
}
