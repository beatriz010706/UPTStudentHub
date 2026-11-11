package com.example.uptstudenthub;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * Cartão de detalhe (pedido B) ligado a um layout view_detail_card. Usado pela
 * DetailActivity e pelo painel lateral da ExplorerActivity em paisagem (RF10).
 */
final class DetailPanel {

    private final Context context;
    private final View card;
    private final TextView title;
    private final TextView message;
    private final ProgressBar progress;
    private final ListView hoursList;
    private final Button favoriteButton;
    private final HourAdapter adapter;
    private List<HourlyForecast> lastShown;

    /** @param root raiz do layout view_detail_card */
    DetailPanel(View root) {
        context = root.getContext();
        card = root;
        title = root.findViewById(R.id.tvDetailTitle);
        message = root.findViewById(R.id.tvDetailMessage);
        progress = root.findViewById(R.id.progressDetail);
        hoursList = root.findViewById(R.id.listHours);
        favoriteButton = root.findViewById(R.id.btnFavorite);
        adapter = new HourAdapter(context);
        hoursList.setAdapter(adapter);
    }

    Button getFavoriteButton() { return favoriteButton; }

    void setTitle(String text) { title.setText(text); }

    /** Mostra/esconde o botão de favorito. */
    void setFavoriteVisible(boolean visible) {
        favoriteButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Ajusta o texto do botão conforme o dia é ou não favorito. */
    void setFavorite(boolean favorite) {
        favoriteButton.setText(favorite
                ? R.string.ex_btn_favorite_remove : R.string.ex_btn_favorite_add);
    }

    /** Mostra o indicador de progresso. */
    void showLoading() {
        progress.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);
        hoursList.setVisibility(View.GONE);
    }

    /** Mostra uma mensagem (erro, vazio ou sugestão). */
    void showMessage(String text) {
        progress.setVisibility(View.GONE);
        hoursList.setVisibility(View.GONE);
        message.setText(text);
        message.setVisibility(View.VISIBLE);
        lastShown = null;
    }

    /** Mostra as horas; o cartão entra com animação quando chegam dados novos (RF5). */
    void showHours(List<HourlyForecast> hours, int threshold, boolean celsius) {
        progress.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        hoursList.setVisibility(View.VISIBLE);
        adapter.setData(hours, threshold, celsius);
        if (hours != lastShown) {
            lastShown = hours;
            card.startAnimation(AnimationUtils.loadAnimation(context, R.anim.item_enter));
        }
    }
}
