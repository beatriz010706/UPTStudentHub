package com.example.uptstudenthub;

/** Previsão de um dia (modelo imutável). Os campos opcionais podem ser null. */
public final class DailyForecast {

    /** Data no formato yyyy-MM-dd. */
    public final String date;
    /** Código WMO (pode ser null). */
    public final Integer weatherCode;
    /** Temperatura máxima (pode ser null). */
    public final Double tempMax;
    /** Temperatura mínima (pode ser null). */
    public final Double tempMin;
    /** Probabilidade de precipitação em % (pode ser null). */
    public final Integer precipProbability;

    /** Cria a previsão de um dia. */
    public DailyForecast(String date, Integer weatherCode, Double tempMax,
                         Double tempMin, Integer precipProbability) {
        this.date = date;
        this.weatherCode = weatherCode;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.precipProbability = precipProbability;
    }
}
