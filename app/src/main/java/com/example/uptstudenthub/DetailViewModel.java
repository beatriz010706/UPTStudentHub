package com.example.uptstudenthub;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** ViewModel da DetailActivity: pedido B em fundo e estado do favorito (sobrevive à rotação). */
public class DetailViewModel extends ViewModel {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final HourlyLoader loader = new HourlyLoader(executor, mainHandler);
    private final MutableLiveData<Boolean> favorite = new MutableLiveData<>();

    /** @return estado do pedido B */
    public LiveData<LoadState> getState() { return loader.getState(); }

    /** @return horas do dia */
    public LiveData<List<HourlyForecast>> getHours() { return loader.getHours(); }

    /** @return último erro */
    public LiveData<ErrorType> getError() { return loader.getError(); }

    /** @return último código HTTP de erro */
    public int getHttpCode() { return loader.getHttpCode(); }

    /** @return se o dia está marcado como favorito (null até ser inicializado) */
    public LiveData<Boolean> getFavorite() { return favorite; }

    /** Define o estado do favorito (main thread). */
    public void setFavorite(boolean value) { favorite.setValue(value); }

    /** Lança o pedido B. */
    public void load(Place place, String date, String unit) {
        loader.load(place, date, unit);
    }

    @Override
    protected void onCleared() {
        loader.cancelTask();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
