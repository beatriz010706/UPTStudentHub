package com.example.uptstudenthub;

/** Definições da variante V1 (modelo imutável, vive em memória). */
public final class ExplorerSettings {

    /** Valor do parâmetro temperature_unit para graus Celsius. */
    public static final String UNIT_CELSIUS = "celsius";
    /** Valor do parâmetro temperature_unit para graus Fahrenheit. */
    public static final String UNIT_FAHRENHEIT = "fahrenheit";

    /** "celsius" ou "fahrenheit". */
    public final String unit;
    /** Número de dias da previsão (3, 5 ou 7). */
    public final int days;
    /** Limiar de chuva em % (0 a 100). */
    public final int rainThreshold;
    /** true se a atualização periódica está ligada. */
    public final boolean autoRefresh;
    /** Intervalo da atualização periódica em segundos. */
    public final int intervalSeconds;

    /** Cria as definições. */
    public ExplorerSettings(String unit, int days, int rainThreshold,
                            boolean autoRefresh, int intervalSeconds) {
        this.unit = unit;
        this.days = days;
        this.rainThreshold = rainThreshold;
        this.autoRefresh = autoRefresh;
        this.intervalSeconds = intervalSeconds;
    }

    /** @return definições iniciais */
    public static ExplorerSettings defaults() {
        return new ExplorerSettings(UNIT_CELSIUS, 7, 50, true, 30);
    }

    /** @return true se a unidade é Celsius */
    public boolean isCelsius() {
        return UNIT_CELSIUS.equals(unit);
    }
}
