package com.example.uptstudenthub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Módulo 3 «Explorer» (V1, previsão semanal). A Activity só trata da interface; os pedidos
 * correm no ExplorerViewModel. Em paisagem mostra a lista e o detalhe lado a lado (sem Fragments).
 */
public class ExplorerActivity extends AppCompatActivity {

    private static final String TAG = "ExplorerLifecycle";
    private static final int[] PLACE_MENU_IDS = {
            R.id.action_place_porto, R.id.action_place_lisboa, R.id.action_place_faro};

    private ExplorerViewModel viewModel;
    private ConnectivityManager connectivityManager;
    private ItemAdapter adapter;
    private DetailPanel panel; // null em retrato
    private ListView listView;
    private TextView tvOffline;
    private TextView tvBattery;
    private TextView tvStatus;
    private ProgressBar progressLoading;
    private Button btnRefresh;
    private Button btnCancel;
    private AlertDialog dialog;
    private ActivityResultLauncher<Intent> detailLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean visible;      // true entre onStart e onStop
    private boolean online = true;
    private boolean positionRestored; // a posição da lista só é reposta uma vez após a rotação

    /** Atualização periódica (RF14): recarrega e volta a agendar-se. */
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            if (online && viewModel.getState().getValue() != LoadState.LOADING) {
                viewModel.load(false);
            }
            scheduleRefresh();
        }
    };

    /** Bateria fraca suspende a atualização periódica; bateria OK retoma-a (RF15). */
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Corre na MAIN thread: trabalho curto.
            if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                viewModel.setBatteryLow(true);
            } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
                viewModel.setBatteryLow(false);
            }
            scheduleRefresh();
        }
    };

    /** NetworkCallback: corre numa thread de fundo, por isso passa por runOnUiThread (RT8). */
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(true));
                }

                @Override
                public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(false));
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate (recreated=" + (savedInstanceState != null) + ")");
        setContentView(R.layout.activity_explorer);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name == null) {
            name = "";
        }
        ((TextView) findViewById(R.id.tvGreeting)).setText(getString(R.string.ex_greeting, name));
        tvOffline = findViewById(R.id.tvOffline);
        tvBattery = findViewById(R.id.tvBattery);
        tvStatus = findViewById(R.id.tvStatus);
        progressLoading = findViewById(R.id.progressLoading);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnCancel = findViewById(R.id.btnCancel);
        listView = findViewById(R.id.listItems);
        View panelRoot = findViewById(R.id.detailPanel);
        panel = panelRoot == null ? null : new DetailPanel(panelRoot);

        connectivityManager = ContextCompat.getSystemService(this, ConnectivityManager.class);
        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);
        adapter = new ItemAdapter(this);
        listView.setAdapter(adapter);

        registerLaunchers();
        setListeners();
        observe();

        // Primeiro carregamento automático (RF3). Depois de uma rotação o estado já não é IDLE.
        if (viewModel.getState().getValue() == LoadState.IDLE) {
            viewModel.load(false);
        }
    }

    // ------------------------------------------------------------------ setup

    private void registerLaunchers() {
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK && data != null) {
                        String date = data.getStringExtra(DetailActivity.EXTRA_DATE);
                        boolean fav = data.getBooleanExtra(DetailActivity.EXTRA_FAVORITE, false);
                        if (date != null) {
                            viewModel.setFavorite(date, fav);
                        }
                    }
                });
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent d = result.getData();
                    if (result.getResultCode() == RESULT_OK && d != null) {
                        ExplorerSettings defaults = ExplorerSettings.defaults();
                        String unit = d.getStringExtra(SettingsActivity.EXTRA_UNIT);
                        viewModel.applySettings(new ExplorerSettings(
                                unit == null ? defaults.unit : unit,
                                d.getIntExtra(SettingsActivity.EXTRA_DAYS, defaults.days),
                                d.getIntExtra(SettingsActivity.EXTRA_THRESHOLD,
                                        defaults.rainThreshold),
                                d.getBooleanExtra(SettingsActivity.EXTRA_AUTO,
                                        defaults.autoRefresh),
                                d.getIntExtra(SettingsActivity.EXTRA_INTERVAL,
                                        defaults.intervalSeconds)));
                        Toast.makeText(this, R.string.ex_toast_settings_saved,
                                Toast.LENGTH_SHORT).show();
                        scheduleRefresh();
                    }
                });
    }

    private void setListeners() {
        btnRefresh.setOnClickListener(v -> viewModel.load(false));
        btnCancel.setOnClickListener(v -> viewModel.cancel());
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DailyForecast day = adapter.getItem(position);
            if (panel != null) {
                viewModel.selectDay(day.date); // paisagem: atualiza o painel
            } else {
                openDetail(day);               // retrato: abre a DetailActivity
            }
        });
        if (panel != null) {
            panel.getFavoriteButton().setOnClickListener(v -> onPanelFavoriteClicked());
        }
    }

    private void observe() {
        viewModel.getState().observe(this, s -> {
            renderStatus();
            invalidateOptionsMenu();
        });
        viewModel.getPlaceIndex().observe(this, i -> {
            renderStatus();
            renderItems();
            invalidateOptionsMenu();
        });
        viewModel.getItems().observe(this, list -> {
            listView.scheduleLayoutAnimation(); // animação de entrada das linhas (RF12)
            renderItems();
            renderStatus();
        });
        viewModel.getFavorites().observe(this, f -> {
            renderItems();
            renderPanel();
        });
        viewModel.getSettings().observe(this, s -> {
            renderItems();
            renderPanel();
        });
        viewModel.getBatteryLow().observe(this, low ->
                tvBattery.setVisibility(Boolean.TRUE.equals(low) ? View.VISIBLE : View.GONE));
        viewModel.getSelectedDate().observe(this, d -> renderPanel());
        viewModel.getDetailState().observe(this, s -> renderPanel());
        viewModel.getDetailHours().observe(this, h -> renderPanel());
    }

    // ------------------------------------------------------------------ rendering

    private void renderItems() {
        List<DailyForecast> items = viewModel.getItems().getValue();
        if (items == null) {
            return;
        }
        Set<String> favoriteDates = new HashSet<>();
        for (DailyForecast d : items) {
            if (viewModel.isFavorite(d.date)) {
                favoriteDates.add(d.date);
            }
        }
        adapter.setData(items, favoriteDates, viewModel.currentSettings().isCelsius());
        if (!positionRestored && !items.isEmpty()) {
            listView.setSelection(viewModel.getFirstVisible()); // posição da lista (RF17)
            positionRestored = true;
        }
    }

    private void renderStatus() {
        LoadState s = viewModel.getState().getValue();
        if (s == null) {
            return;
        }
        boolean loading = s == LoadState.LOADING;
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        btnCancel.setEnabled(loading);
        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.ex_status_idle);
                break;
            case LOADING:
                tvStatus.setText(R.string.status_loading);
                break;
            case SUCCESS:
                List<DailyForecast> items = viewModel.getItems().getValue();
                tvStatus.setText(getString(R.string.ex_status_success,
                        getString(viewModel.currentPlace().nameRes),
                        items == null ? 0 : items.size()));
                break;
            case EMPTY:
                tvStatus.setText(R.string.ex_status_empty);
                break;
            case ERROR:
                tvStatus.setText(Format.error(this, viewModel.getError().getValue(),
                        viewModel.getHttpCode()));
                break;
            case CANCELLED:
                tvStatus.setText(R.string.ex_status_cancelled);
                break;
        }
    }

    /** Painel de detalhe (só em paisagem). */
    private void renderPanel() {
        if (panel == null) {
            return;
        }
        String date = viewModel.getSelectedDate().getValue();
        if (date == null) {
            panel.setTitle(getString(R.string.ex_detail_label));
            panel.setFavoriteVisible(false);
            panel.showMessage(getString(R.string.ex_panel_hint));
            return;
        }
        panel.setTitle(getString(R.string.ex_detail_title, Format.day(date),
                getString(viewModel.currentPlace().nameRes)));
        panel.setFavoriteVisible(true);
        panel.setFavorite(viewModel.isFavorite(date));
        LoadState s = viewModel.getDetailState().getValue();
        if (s == null) {
            return;
        }
        ExplorerSettings settings = viewModel.currentSettings();
        switch (s) {
            case LOADING:
                panel.showLoading();
                break;
            case SUCCESS:
                List<HourlyForecast> hours = viewModel.getDetailHours().getValue();
                if (hours != null) {
                    panel.showHours(hours, settings.rainThreshold, settings.isCelsius());
                }
                break;
            case EMPTY:
                panel.showMessage(getString(R.string.ex_detail_empty));
                break;
            case ERROR:
                panel.showMessage(Format.error(this, viewModel.getDetailError().getValue(),
                        viewModel.getDetailHttpCode()));
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ navigation / actions

    private ActivityOptionsCompat transition() {
        return ActivityOptionsCompat.makeCustomAnimation(this,
                R.anim.slide_in_right, R.anim.fade_out); // transição entre activities (RF12)
    }

    private void openDetail(DailyForecast day) {
        ExplorerSettings s = viewModel.currentSettings();
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra(DetailActivity.EXTRA_DATE, day.date);
        i.putExtra(DetailActivity.EXTRA_PLACE_INDEX, viewModel.currentPlaceIndex());
        i.putExtra(DetailActivity.EXTRA_UNIT, s.unit);
        i.putExtra(DetailActivity.EXTRA_THRESHOLD, s.rainThreshold);
        i.putExtra(DetailActivity.EXTRA_FAVORITE, viewModel.isFavorite(day.date));
        detailLauncher.launch(i, transition());
    }

    private void openSettings() {
        ExplorerSettings s = viewModel.currentSettings();
        Intent i = new Intent(this, SettingsActivity.class);
        i.putExtra(SettingsActivity.EXTRA_UNIT, s.unit);
        i.putExtra(SettingsActivity.EXTRA_DAYS, s.days);
        i.putExtra(SettingsActivity.EXTRA_THRESHOLD, s.rainThreshold);
        i.putExtra(SettingsActivity.EXTRA_AUTO, s.autoRefresh);
        i.putExtra(SettingsActivity.EXTRA_INTERVAL, s.intervalSeconds);
        settingsLauncher.launch(i, transition());
    }

    private void openCompare() {
        Intent i = new Intent(this, CompareActivity.class);
        i.putExtra(CompareActivity.EXTRA_UNIT, viewModel.currentSettings().unit);
        startActivity(i, transition().toBundle());
    }

    /** Favorito no painel de paisagem: marcar é direto, desmarcar pede confirmação (RF7). */
    private void onPanelFavoriteClicked() {
        final String date = viewModel.getSelectedDate().getValue();
        if (date == null) {
            return;
        }
        if (viewModel.isFavorite(date)) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.ex_confirm_remove_title)
                    .setMessage(R.string.ex_confirm_remove_msg)
                    .setPositiveButton(R.string.ex_remove, (d, which) -> {
                        viewModel.setFavorite(date, false);
                        Toast.makeText(this, R.string.ex_toast_fav_removed,
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            viewModel.setFavorite(date, true);
            Toast.makeText(this, R.string.ex_toast_fav_added, Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ options menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean loading = viewModel.getState().getValue() == LoadState.LOADING;
        menu.findItem(R.id.action_refresh).setEnabled(!loading);
        menu.findItem(R.id.action_settings).setEnabled(!loading);
        menu.findItem(R.id.action_server_error).setEnabled(!loading);
        menu.findItem(R.id.action_place).setEnabled(!loading);
        int current = viewModel.currentPlaceIndex();
        for (int i = 0; i < PLACE_MENU_IDS.length; i++) {
            MenuItem item = menu.findItem(PLACE_MENU_IDS[i]);
            item.setEnabled(!loading);
            item.setChecked(i == current);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish(); // Up
            return true;
        } else if (id == R.id.action_refresh) {
            viewModel.load(false);
            return true;
        } else if (id == R.id.action_compare) {
            openCompare();
            return true;
        } else if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_server_error) {
            viewModel.load(true); // URL inexistente -> erro HTTP (T13)
            return true;
        } else if (id == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        for (int i = 0; i < PLACE_MENU_IDS.length; i++) {
            if (id == PLACE_MENU_IDS[i]) {
                item.setChecked(true);
                viewModel.setPlaceIndex(i);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------ network / periodic refresh

    private boolean isOnlineNow() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /** Mostra/esconde a faixa offline; se a rede voltou e o último pedido falhou, recarrega. */
    private void setOnline(boolean nowOnline) {
        boolean cameBack = !online && nowOnline;
        online = nowOnline;
        tvOffline.setVisibility(online ? View.GONE : View.VISIBLE);
        if (cameBack && viewModel.getState().getValue() == LoadState.ERROR) {
            Toast.makeText(this, R.string.toast_back_online, Toast.LENGTH_SHORT).show();
            viewModel.load(false);
        }
    }

    /** (Re)agenda a atualização periódica, se estiver visível, ligada e sem bateria fraca. */
    private void scheduleRefresh() {
        handler.removeCallbacks(refreshTick);
        ExplorerSettings s = viewModel.currentSettings();
        boolean low = Boolean.TRUE.equals(viewModel.getBatteryLow().getValue());
        if (!visible || !s.autoRefresh || low) {
            return;
        }
        handler.postDelayed(refreshTick, s.intervalSeconds * 1000L);
    }

    // ------------------------------------------------------------------ lifecycle (RF18)

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        visible = true;
        // Receivers e callbacks registados em onStart e anulados em onStop (RT8).
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        ContextCompat.registerReceiver(this, batteryReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        setOnline(isOnlineNow());
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        scheduleRefresh();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        visible = false;
        handler.removeCallbacks(refreshTick);
        unregisterReceiver(batteryReceiver);
        connectivityManager.unregisterNetworkCallback(networkCallback);
        super.onStop();
    }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }

    @Override
    protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        viewModel.setFirstVisible(listView.getFirstVisiblePosition());
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy (finishing=" + isFinishing() + ")");
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }
}
