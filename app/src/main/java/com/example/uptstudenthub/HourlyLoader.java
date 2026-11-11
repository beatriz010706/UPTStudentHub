package com.example.uptstudenthub;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Carrega o pedido B (horas de um dia) num executor emprestado pelo ViewModel dono
 * e publica o estado em LiveData. Partilhado pelo ExplorerViewModel (painel em paisagem)
 * e pelo DetailViewModel. Não guarda Context nem Views.
 */
final class HourlyLoader {

    private final ExecutorService executor;
    private final Handler mainHandler;
    private final MutableLiveData<LoadState> state = new MutableLiveData<>(LoadState.IDLE);
    private final MutableLiveData<List<HourlyForecast>> hours =
            new MutableLiveData<>(Collections.<HourlyForecast>emptyList());
    private final MutableLiveData<ErrorType> error = new MutableLiveData<>();
    private int httpCode;
    private int requestId;
    private Future<?> task;

    HourlyLoader(ExecutorService executor, Handler mainHandler) {
        this.executor = executor;
        this.mainHandler = mainHandler;
    }

    LiveData<LoadState> getState() { return state; }

    LiveData<List<HourlyForecast>> getHours() { return hours; }

    LiveData<ErrorType> getError() { return error; }

    int getHttpCode() { return httpCode; }

    /** Lança o pedido B (main thread). Um pedido anterior ainda em curso é descartado. */
    void load(Place place, String date, String unit) {
        cancelTask();
        final int id = ++requestId;
        state.setValue(LoadState.LOADING);
        task = executor.submit(() -> {
            // ---- THREAD DE FUNDO ----
            try {
                List<HourlyForecast> result = ForecastRepository.fetchHourly(place, date, unit);
                mainHandler.post(() -> { // ---- MAIN THREAD ----
                    if (id != requestId) {
                        return; // resultado de um pedido já descartado
                    }
                    hours.setValue(result);
                    state.setValue(result.isEmpty() ? LoadState.EMPTY : LoadState.SUCCESS);
                });
            } catch (ExplorerException e) {
                mainHandler.post(() -> {
                    if (id != requestId) {
                        return;
                    }
                    httpCode = e.httpCode;
                    error.setValue(e.type);
                    state.setValue(LoadState.ERROR);
                });
            }
        });
    }

    /** Volta ao estado inicial e descarta qualquer pedido em curso. */
    void reset() {
        cancelTask();
        requestId++;
        hours.setValue(Collections.<HourlyForecast>emptyList());
        state.setValue(LoadState.IDLE);
    }

    /** Cancela a tarefa em curso (usado também em onCleared). */
    void cancelTask() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }
}
