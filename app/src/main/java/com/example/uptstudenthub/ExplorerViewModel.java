package com.example.uptstudenthub;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ViewModel da ExplorerActivity. Guarda estado, dados, favoritos, definições e o dia
 * selecionado (painel em paisagem), por isso tudo sobrevive à rotação sem novo pedido (RF17).
 * Regras: sem Context/Activity/View (RT7); rede só no executor (RT6); resultados entregues
 * na main thread através do Handler.
 */
public class ExplorerViewModel extends ViewModel {

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final HourlyLoader detail = new HourlyLoader(executor, mainHandler);

    private final MutableLiveData<LoadState> state = new MutableLiveData<>(LoadState.IDLE);
    private final MutableLiveData<List<DailyForecast>> items =
            new MutableLiveData<>(Collections.<DailyForecast>emptyList());
    private final MutableLiveData<ErrorType> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> placeIndex = new MutableLiveData<>(0);
    private final MutableLiveData<ExplorerSettings> settings =
            new MutableLiveData<>(ExplorerSettings.defaults());
    private final MutableLiveData<Set<String>> favorites =
            new MutableLiveData<>(Collections.<String>emptySet());
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> batteryLow = new MutableLiveData<>(false);

    private int httpCode;
    private int requestId;
    private int firstVisible;
    private Future<?> currentTask;

    // ------------------------------------------------------------------ getters

    /** @return estado do pedido A */
    public LiveData<LoadState> getState() { return state; }

    /** @return dados do pedido A (últimos recebidos) */
    public LiveData<List<DailyForecast>> getItems() { return items; }

    /** @return último erro */
    public LiveData<ErrorType> getError() { return error; }

    /** @return último código HTTP de erro */
    public int getHttpCode() { return httpCode; }

    /** @return índice do local atual em {@link Place#ALL} */
    public LiveData<Integer> getPlaceIndex() { return placeIndex; }

    /** @return definições atuais */
    public LiveData<ExplorerSettings> getSettings() { return settings; }

    /** @return chaves dos favoritos (local|data) */
    public LiveData<Set<String>> getFavorites() { return favorites; }

    /** @return data do dia selecionado no painel de paisagem (null se nenhum) */
    public LiveData<String> getSelectedDate() { return selectedDate; }

    /** @return true se a bateria está fraca (atualização periódica suspensa) */
    public LiveData<Boolean> getBatteryLow() { return batteryLow; }

    /** @return estado do pedido B do painel */
    public LiveData<LoadState> getDetailState() { return detail.getState(); }

    /** @return horas do dia selecionado */
    public LiveData<List<HourlyForecast>> getDetailHours() { return detail.getHours(); }

    /** @return erro do pedido B */
    public LiveData<ErrorType> getDetailError() { return detail.getError(); }

    /** @return código HTTP do erro do pedido B */
    public int getDetailHttpCode() { return detail.getHttpCode(); }

    /** @return índice do local atual (nunca null) */
    public int currentPlaceIndex() {
        Integer i = placeIndex.getValue();
        return i == null ? 0 : i;
    }

    /** @return local atual */
    public Place currentPlace() { return Place.ALL.get(currentPlaceIndex()); }

    /** @return definições atuais (nunca null) */
    public ExplorerSettings currentSettings() {
        ExplorerSettings s = settings.getValue();
        return s == null ? ExplorerSettings.defaults() : s;
    }

    /** @return primeira posição visível da lista (guardada para a rotação) */
    public int getFirstVisible() { return firstVisible; }

    /** Guarda a primeira posição visível da lista. */
    public void setFirstVisible(int position) { firstVisible = position; }

    // ------------------------------------------------------------------ pedido A

    /**
     * Lança o pedido A (main thread). Ignorado se já houver um pedido em curso.
     *
     * @param broken true para usar um URL inexistente e provocar um erro HTTP (teste T13)
     */
    public void load(boolean broken) {
        if (state.getValue() == LoadState.LOADING) {
            return; // um pedido de cada vez
        }
        startLoad(broken);
    }

    /** Lança sempre um novo pedido, descartando o que estiver em curso. */
    private void startLoad(boolean broken) {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        final int id = ++requestId;
        final Place place = currentPlace();
        final ExplorerSettings s = currentSettings();
        state.setValue(LoadState.LOADING);
        currentTask = executor.submit(() -> {
            // ---- THREAD DE FUNDO ----
            try {
                List<DailyForecast> result = ForecastRepository.fetchDaily(place, s, broken);
                mainHandler.post(() -> { // ---- MAIN THREAD ----
                    if (id != requestId) {
                        return; // cancelado ou substituído: ignorar
                    }
                    items.setValue(result);
                    state.setValue(result.isEmpty() ? LoadState.EMPTY : LoadState.SUCCESS);
                });
            } catch (ExplorerException e) {
                mainHandler.post(() -> {
                    if (id != requestId) {
                        return;
                    }
                    httpCode = e.httpCode;
                    error.setValue(e.type);
                    state.setValue(LoadState.ERROR); // os dados anteriores mantêm-se
                });
            }
        });
    }

    /**
     * Cancela o pedido em curso (RF4). Future.cancel(true) interrompe a thread; como o
     * socket pode não reagir já, o requestId garante que um resultado tardio é ignorado.
     */
    public void cancel() {
        if (state.getValue() != LoadState.LOADING) {
            return;
        }
        requestId++;
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        state.setValue(LoadState.CANCELLED); // os dados anteriores mantêm-se
    }

    /** Muda de local e recarrega. */
    public void setPlaceIndex(int index) {
        if (index == currentPlaceIndex() || index < 0 || index >= Place.ALL.size()) {
            return;
        }
        placeIndex.setValue(index);
        items.setValue(Collections.<DailyForecast>emptyList());
        selectedDate.setValue(null);
        detail.reset();
        startLoad(false);
    }

    /** Aplica novas definições (vindas da SettingsActivity) e recarrega a lista. */
    public void applySettings(ExplorerSettings newSettings) {
        settings.setValue(newSettings);
        startLoad(false);
        String date = selectedDate.getValue();
        if (date != null) {
            detail.load(currentPlace(), date, newSettings.unit);
        }
    }

    // ------------------------------------------------------------------ painel / favoritos

    /** Seleciona um dia (paisagem) e lança o pedido B para esse dia. */
    public void selectDay(String date) {
        selectedDate.setValue(date);
        detail.load(currentPlace(), date, currentSettings().unit);
    }

    /** @return chave do favorito para uma data no local atual */
    public String favoriteKey(String date) {
        return currentPlace().id + "|" + date;
    }

    /** @return true se a data está marcada como favorita no local atual */
    public boolean isFavorite(String date) {
        Set<String> set = favorites.getValue();
        return set != null && set.contains(favoriteKey(date));
    }

    /** Marca ou desmarca um dia como favorito (guarda uma cópia nova do conjunto). */
    public void setFavorite(String date, boolean value) {
        Set<String> copy = new HashSet<>();
        Set<String> current = favorites.getValue();
        if (current != null) {
            copy.addAll(current);
        }
        if (value) {
            copy.add(favoriteKey(date));
        } else {
            copy.remove(favoriteKey(date));
        }
        favorites.setValue(Collections.unmodifiableSet(copy));
    }

    /** Regista se a bateria está fraca (RF15). */
    public void setBatteryLow(boolean low) { batteryLow.setValue(low); }

    @Override
    protected void onCleared() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        detail.cancelTask();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
