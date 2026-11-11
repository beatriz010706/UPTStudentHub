package com.example.uptstudenthub;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

/**
 * Compara Porto, Lisboa e Faro lado a lado (RF9). Os três pedidos correm em paralelo no
 * CompareViewModel; se um falhar, os outros dois aparecem e o local falhado mostra o seu erro.
 */
public class CompareActivity extends AppCompatActivity {

    /** Extra: unidade de temperatura. */
    public static final String EXTRA_UNIT = "extra_unit";

    private CompareViewModel viewModel;
    private PlaceColumn[] columns;
    private Button btnCompare;
    private String unit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        unit = getIntent().getStringExtra(EXTRA_UNIT);
        if (unit == null) {
            unit = ExplorerSettings.UNIT_CELSIUS;
        }
        columns = new PlaceColumn[] {
                new PlaceColumn(findViewById(R.id.colPorto)),
                new PlaceColumn(findViewById(R.id.colLisboa)),
                new PlaceColumn(findViewById(R.id.colFaro))};
        btnCompare = findViewById(R.id.btnCompare);

        viewModel = new ViewModelProvider(this).get(CompareViewModel.class);
        btnCompare.setOnClickListener(v -> viewModel.compare(unit, false));
        viewModel.getResults().observe(this, r -> render());
        viewModel.getState().observe(this, s -> {
            btnCompare.setEnabled(s != LoadState.LOADING);
            invalidateOptionsMenu();
            render();
        });
        if (viewModel.getState().getValue() == LoadState.IDLE) {
            viewModel.compare(unit, false);
        }
    }

    private void render() {
        List<PlaceResult> results = viewModel.getResults().getValue();
        boolean loading = viewModel.getState().getValue() == LoadState.LOADING;
        boolean celsius = ExplorerSettings.UNIT_CELSIUS.equals(unit);
        for (int i = 0; i < columns.length; i++) {
            PlaceResult r = results != null && i < results.size() ? results.get(i) : null;
            columns[i].bind(this, Place.ALL.get(i), r, loading, celsius);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compare, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean loading = viewModel.getState().getValue() == LoadState.LOADING;
        menu.findItem(R.id.action_compare_again).setEnabled(!loading);
        menu.findItem(R.id.action_compare_fail).setEnabled(!loading);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish(); // Up
            return true;
        } else if (id == R.id.action_compare_again) {
            viewModel.compare(unit, false);
            return true;
        } else if (id == R.id.action_compare_fail) {
            viewModel.compare(unit, true); // Faro com URL inexistente (T11)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Views de uma coluna (um local). */
    private static final class PlaceColumn {
        private final TextView name;
        private final ProgressBar progress;
        private final TextView message;
        private final ImageView icon;
        private final TextView max;
        private final TextView min;
        private final TextView rain;

        PlaceColumn(View root) {
            name = root.findViewById(R.id.tvColPlace);
            progress = root.findViewById(R.id.progressCol);
            message = root.findViewById(R.id.tvColMessage);
            icon = root.findViewById(R.id.ivColIcon);
            max = root.findViewById(R.id.tvColMax);
            min = root.findViewById(R.id.tvColMin);
            rain = root.findViewById(R.id.tvColRain);
        }

        void bind(CompareActivity a, Place place, PlaceResult r, boolean loading,
                  boolean celsius) {
            name.setText(place.nameRes);
            boolean hasData = r != null && r.error == null && r.today != null;
            progress.setVisibility(r == null && loading ? View.VISIBLE : View.GONE);
            icon.setVisibility(hasData ? View.VISIBLE : View.GONE);
            max.setVisibility(hasData ? View.VISIBLE : View.GONE);
            min.setVisibility(hasData ? View.VISIBLE : View.GONE);
            rain.setVisibility(hasData ? View.VISIBLE : View.GONE);
            if (r != null && r.error != null) {
                message.setText(Format.error(a, r.error, r.httpCode));
                message.setVisibility(View.VISIBLE);
            } else {
                message.setVisibility(View.GONE);
            }
            if (hasData) {
                DailyForecast d = r.today;
                icon.setImageResource(WeatherCodes.iconFor(d.weatherCode));
                max.setText(a.getString(R.string.ex_max,
                        Format.temperature(a, d.tempMax, celsius)));
                min.setText(a.getString(R.string.ex_min,
                        Format.temperature(a, d.tempMin, celsius)));
                rain.setText(Format.rain(a, d.precipProbability));
            }
        }
    }
}
