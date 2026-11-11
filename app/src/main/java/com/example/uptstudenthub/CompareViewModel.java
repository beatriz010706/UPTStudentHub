package com.example.uptstudenthub;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ViewModel da CompareActivity (RF9): três pedidos em paralelo, um por local, num
 * Executors.newFixedThreadPool(3). Cada resultado chega à main thread assim que está pronto;
 * se um local falha, os outros continuam e o falhado guarda o seu erro (falha parcial).
 */
public class CompareViewModel extends ViewModel {

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<LoadState> state = new MutableLiveData<>(LoadState.IDLE);
    private final MutableLiveData<List<PlaceResult>> results =
            new MutableLiveData<>(Collections.<PlaceResult>emptyList());
    private final List<Future<?>> tasks = new ArrayList<>();
    private PlaceResult[] slots = new PlaceResult[Place.ALL.size()];
    private int pending;
    private int requestId;

    /** @return LOADING enquanto faltar algum local, SUCCESS quando todos responderam */
    public LiveData<LoadState> getState() { return state; }

    /** @return um resultado por local (null enquanto esse local não respondeu) */
    public LiveData<List<PlaceResult>> getResults() { return results; }

    /**
     * Lança os três pedidos em paralelo (main thread).
     *
     * @param unit     "celsius" ou "fahrenheit"
     * @param failLast true para forçar um erro HTTP no último local (Faro), teste T11
     */
    public void compare(String unit, boolean failLast) {
        if (state.getValue() == LoadState.LOADING) {
            return;
        }
        final int id = ++requestId;
        slots = new PlaceResult[Place.ALL.size()];
        pending = slots.length;
        tasks.clear();
        results.setValue(snapshot());
        state.setValue(LoadState.LOADING);
        for (int i = 0; i < Place.ALL.size(); i++) {
            final int index = i;
            final Place place = Place.ALL.get(i);
            final boolean broken = failLast && i == Place.ALL.size() - 1;
            tasks.add(executor.submit(() -> {
                // ---- THREAD DE FUNDO (uma por local) ----
                final PlaceResult result = fetchOne(place, unit, broken);
                mainHandler.post(() -> deliver(id, index, result)); // ---- MAIN THREAD ----
            }));
        }
    }

    /** Faz o pedido de um local; nunca lança: a falha fica dentro do PlaceResult. */
    private static PlaceResult fetchOne(Place place, String unit, boolean broken) {
        try {
            return PlaceResult.ok(place, ForecastRepository.fetchToday(place, unit, broken));
        } catch (ExplorerException e) {
            return PlaceResult.failed(place, e.type, e.httpCode);
        }
    }

    /** Corre na main thread: guarda o resultado de um local e publica o conjunto. */
    private void deliver(int id, int index, PlaceResult result) {
        if (id != requestId) {
            return;
        }
        slots[index] = result;
        pending--;
        results.setValue(snapshot());
        if (pending == 0) {
            state.setValue(LoadState.SUCCESS);
        }
    }

    private List<PlaceResult> snapshot() {
        return Collections.unmodifiableList(Arrays.asList(slots.clone()));
    }

    @Override
    protected void onCleared() {
        for (Future<?> f : tasks) {
            f.cancel(true);
        }
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
