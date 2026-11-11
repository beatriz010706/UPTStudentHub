package com.example.uptstudenthub;

/** Previsão de uma hora (modelo imutável). Os campos opcionais podem ser null. */
public final class HourlyForecast {

    /** Instante no formato yyyy-MM-ddTHH:mm. */
    public final String time;
    /** Temperatura (pode ser null). */
    public final Double temperature;
    /** Probabilidade de precipitação em % (pode ser null). */
    public final Integer precipProbability;
    /** Código WMO (pode ser null). */
    public final Integer weatherCode;

    /** Cria a previsão de uma hora. */
    public HourlyForecast(String time, Double temperature,
                          Integer precipProbability, Integer weatherCode) {
        this.time = time;
        this.temperature = temperature;
        this.precipProbability = precipProbability;
        this.weatherCode = weatherCode;
    }
}
