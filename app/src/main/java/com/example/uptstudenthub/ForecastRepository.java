package com.example.uptstudenthub;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Junta ExplorerApi + HttpClient + ExplorerParser e converte todas as falhas em
 * {@link ExplorerException}. BLOQUEANTE: só pode ser chamada em threads de fundo.
 */
final class ForecastRepository {

    private ForecastRepository() { }

    /** Pedido A: previsão diária do local com as definições dadas. */
    static List<DailyForecast> fetchDaily(Place place, ExplorerSettings settings,
                                          boolean broken) throws ExplorerException {
        String url = broken
                ? ExplorerApi.brokenDailyUrl(place, settings.days, settings.unit)
                : ExplorerApi.dailyUrl(place, settings.days, settings.unit);
        try {
            return ExplorerParser.parseDaily(download(url));
        } catch (DataException e) {
            throw new ExplorerException(ErrorType.PARSE, 0, e);
        }
    }

    /** Pedido B: previsão hora a hora de um dia. */
    static List<HourlyForecast> fetchHourly(Place place, String date,
                                            String unit) throws ExplorerException {
        try {
            return ExplorerParser.parseHourly(download(ExplorerApi.hourlyUrl(place, date, unit)));
        } catch (DataException e) {
            throw new ExplorerException(ErrorType.PARSE, 0, e);
        }
    }

    /** Comparação: previsão de hoje; {@code broken} força um erro HTTP (teste T11). */
    static DailyForecast fetchToday(Place place, String unit,
                                    boolean broken) throws ExplorerException {
        String url = broken
                ? ExplorerApi.brokenDailyUrl(place, 1, unit)
                : ExplorerApi.dailyUrl(place, 1, unit);
        try {
            List<DailyForecast> days = ExplorerParser.parseDaily(download(url));
            if (days.isEmpty()) {
                throw new ExplorerException(ErrorType.EMPTY, 0, null);
            }
            return days.get(0);
        } catch (DataException e) {
            throw new ExplorerException(ErrorType.PARSE, 0, e);
        }
    }

    /** GET com mapeamento das falhas de rede para tipos de erro (RF16). */
    private static String download(String url) throws ExplorerException {
        try {
            return HttpClient.get(url);
        } catch (HttpClient.HttpStatusException e) {
            throw new ExplorerException(ErrorType.HTTP, e.code, e);
        } catch (SocketTimeoutException e) {
            throw new ExplorerException(ErrorType.TIMEOUT, 0, e);
        } catch (IOException e) {
            throw new ExplorerException(ErrorType.NO_CONNECTION, 0, e);
        }
    }
}
