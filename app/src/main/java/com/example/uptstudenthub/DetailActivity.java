package com.example.uptstudenthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

/**
 * Detalhe de um dia: faz o pedido B (horas) e permite marcar/desmarcar o favorito,
 * devolvendo o resultado à ExplorerActivity (RF5, RF6, RF7).
 */
public class DetailActivity extends AppCompatActivity {

    /** Extra: data do dia (yyyy-MM-dd). Também é devolvido no resultado. */
    public static final String EXTRA_DATE = "extra_date";
    /** Extra: índice do local em {@link Place#ALL}. */
    public static final String EXTRA_PLACE_INDEX = "extra_place_index";
    /** Extra: unidade de temperatura. */
    public static final String EXTRA_UNIT = "extra_unit";
    /** Extra: limiar de chuva em %. */
    public static final String EXTRA_THRESHOLD = "extra_threshold";
    /** Extra (entrada e resultado): estado do favorito. */
    public static final String EXTRA_FAVORITE = "extra_favorite";

    private static final String TAG = "DetailLifecycle";

    private DetailViewModel viewModel;
    private DetailPanel panel;
    private AlertDialog dialog;
    private String date;
    private Place place;
    private String unit;
    private int threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate (recreated=" + (savedInstanceState != null) + ")");
        setContentView(R.layout.activity_detail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent in = getIntent();
        date = in.getStringExtra(EXTRA_DATE);
        if (date == null) {
            date = "";
        }
        int placeIndex = in.getIntExtra(EXTRA_PLACE_INDEX, 0);
        place = Place.ALL.get(Math.max(0, Math.min(placeIndex, Place.ALL.size() - 1)));
        unit = in.getStringExtra(EXTRA_UNIT);
        if (unit == null) {
            unit = ExplorerSettings.UNIT_CELSIUS;
        }
        threshold = in.getIntExtra(EXTRA_THRESHOLD, 50);

        panel = new DetailPanel(findViewById(R.id.detailCard));
        panel.setTitle(getString(R.string.ex_detail_title, Format.day(date),
                getString(place.nameRes)));
        panel.setFavoriteVisible(true);

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        if (viewModel.getFavorite().getValue() == null) {
            viewModel.setFavorite(in.getBooleanExtra(EXTRA_FAVORITE, false));
        }
        panel.getFavoriteButton().setOnClickListener(v -> onFavoriteClicked());

        viewModel.getFavorite().observe(this, fav -> {
            panel.setFavorite(Boolean.TRUE.equals(fav));
            deliverResult(Boolean.TRUE.equals(fav));
        });
        viewModel.getState().observe(this, s -> render());
        viewModel.getHours().observe(this, h -> render());

        // Primeira visita: pedido B. Depois de uma rotação o estado já não é IDLE.
        if (viewModel.getState().getValue() == LoadState.IDLE) {
            viewModel.load(place, date, unit);
        }
    }

    /** Desenha o cartão conforme o estado do pedido B. */
    private void render() {
        LoadState s = viewModel.getState().getValue();
        if (s == null) {
            return;
        }
        switch (s) {
            case LOADING:
                panel.showLoading();
                break;
            case SUCCESS:
                List<HourlyForecast> hours = viewModel.getHours().getValue();
                if (hours != null) {
                    panel.showHours(hours, threshold,
                            ExplorerSettings.UNIT_CELSIUS.equals(unit));
                }
                break;
            case EMPTY:
                panel.showMessage(getString(R.string.ex_detail_empty));
                break;
            case ERROR:
                panel.showMessage(Format.error(this, viewModel.getError().getValue(),
                        viewModel.getHttpCode()));
                break;
            default:
                break;
        }
    }

    /** Marcar pede só um toque; desmarcar pede confirmação (RF7). */
    private void onFavoriteClicked() {
        if (Boolean.TRUE.equals(viewModel.getFavorite().getValue())) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.ex_confirm_remove_title)
                    .setMessage(R.string.ex_confirm_remove_msg)
                    .setPositiveButton(R.string.ex_remove, (d, which) -> {
                        viewModel.setFavorite(false);
                        Toast.makeText(this, R.string.ex_toast_fav_removed,
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            viewModel.setFavorite(true);
            Toast.makeText(this, R.string.ex_toast_fav_added, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Devolve o resultado à ExplorerActivity. É chamado também depois de uma rotação,
     * porque o resultado definido com setResult perde-se quando a activity é recriada.
     */
    private void deliverResult(boolean favorite) {
        boolean initial = getIntent().getBooleanExtra(EXTRA_FAVORITE, false);
        if (favorite == initial) {
            setResult(RESULT_CANCELED);
        } else {
            Intent result = new Intent();
            result.putExtra(EXTRA_DATE, date);
            result.putExtra(EXTRA_FAVORITE, favorite);
            setResult(RESULT_OK, result);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Up
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransitionCompat();
    }

    @SuppressWarnings("deprecation")
    private void overridePendingTransitionCompat() {
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }

    @Override
    protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }

    @Override
    protected void onStop() { Log.d(TAG, "onStop"); super.onStop(); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy (finishing=" + isFinishing() + ")");
        if (dialog != null) {
            dialog.dismiss(); // evita "window leaked" na rotação
        }
        super.onDestroy();
    }
}
