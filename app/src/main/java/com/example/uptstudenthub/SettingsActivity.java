package com.example.uptstudenthub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Definições da variante V1 (RF8): RadioGroup (unidade e dias), CheckBox (atualização
 * automática) e EditText (limiar de chuva e intervalo). Valida os campos, mostra o erro no
 * próprio campo e só devolve resultado quando tudo é válido.
 */
public class SettingsActivity extends AppCompatActivity {

    /** Extra (entrada e resultado): "celsius" ou "fahrenheit". */
    public static final String EXTRA_UNIT = "extra_unit";
    /** Extra (entrada e resultado): número de dias (3, 5 ou 7). */
    public static final String EXTRA_DAYS = "extra_days";
    /** Extra (entrada e resultado): limiar de chuva (0 a 100). */
    public static final String EXTRA_THRESHOLD = "extra_threshold";
    /** Extra (entrada e resultado): atualização automática ligada. */
    public static final String EXTRA_AUTO = "extra_auto";
    /** Extra (entrada e resultado): intervalo em segundos (5 a 3600). */
    public static final String EXTRA_INTERVAL = "extra_interval";

    private static final int MIN_INTERVAL = 5;
    private static final int MAX_INTERVAL = 3600;

    private RadioGroup rgUnit;
    private RadioGroup rgDays;
    private CheckBox cbAuto;
    private EditText etThreshold;
    private EditText etInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        rgUnit = findViewById(R.id.rgUnit);
        rgDays = findViewById(R.id.rgDays);
        cbAuto = findViewById(R.id.cbAuto);
        etThreshold = findViewById(R.id.etThreshold);
        etInterval = findViewById(R.id.etInterval);
        Button btnSave = findViewById(R.id.btnSave);

        // Só na primeira criação: depois de uma rotação o Android repõe o estado das Views.
        if (savedInstanceState == null) {
            fillFromIntent(getIntent());
        }
        btnSave.setOnClickListener(v -> save());
    }

    private void fillFromIntent(Intent in) {
        ExplorerSettings d = ExplorerSettings.defaults();
        boolean celsius = !ExplorerSettings.UNIT_FAHRENHEIT.equals(in.getStringExtra(EXTRA_UNIT));
        rgUnit.check(celsius ? R.id.rbCelsius : R.id.rbFahrenheit);
        int days = in.getIntExtra(EXTRA_DAYS, d.days);
        rgDays.check(days == 3 ? R.id.rbDays3 : days == 5 ? R.id.rbDays5 : R.id.rbDays7);
        cbAuto.setChecked(in.getBooleanExtra(EXTRA_AUTO, d.autoRefresh));
        etThreshold.setText(String.valueOf(in.getIntExtra(EXTRA_THRESHOLD, d.rainThreshold)));
        etInterval.setText(String.valueOf(in.getIntExtra(EXTRA_INTERVAL, d.intervalSeconds)));
    }

    /** Valida; se houver erro mostra-o no campo e não devolve nada (teste T9). */
    private void save() {
        etThreshold.setError(null);
        etInterval.setError(null);
        Integer threshold = parseInRange(etThreshold, 0, 100);
        Integer interval = parseInRange(etInterval, MIN_INTERVAL, MAX_INTERVAL);
        boolean valid = true;
        if (threshold == null) {
            etThreshold.setError(getString(R.string.ex_err_threshold));
            valid = false;
        }
        if (interval == null) {
            etInterval.setError(getString(R.string.ex_err_interval));
            valid = false;
        }
        if (!valid) {
            return;
        }
        int daysId = rgDays.getCheckedRadioButtonId();
        int days = daysId == R.id.rbDays3 ? 3 : daysId == R.id.rbDays5 ? 5 : 7;
        Intent result = new Intent();
        result.putExtra(EXTRA_UNIT, rgUnit.getCheckedRadioButtonId() == R.id.rbFahrenheit
                ? ExplorerSettings.UNIT_FAHRENHEIT : ExplorerSettings.UNIT_CELSIUS);
        result.putExtra(EXTRA_DAYS, days);
        result.putExtra(EXTRA_THRESHOLD, threshold.intValue());
        result.putExtra(EXTRA_AUTO, cbAuto.isChecked());
        result.putExtra(EXTRA_INTERVAL, interval.intValue());
        setResult(RESULT_OK, result);
        finish();
    }

    /** @return o inteiro do campo se estiver em [min, max], senão null */
    private static Integer parseInRange(EditText field, int min, int max) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(text);
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Up sem guardar: resultado CANCELED
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
