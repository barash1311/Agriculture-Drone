package com.agridrone.safety.ui.main;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import com.agridrone.safety.R;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.source.MockTelemetryDataSource;
import com.agridrone.safety.databinding.ActivityMainBinding;
import com.agridrone.safety.ui.main.adapter.CellVoltageAdapter;

public final class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private CellVoltageAdapter cellAdapter;
    private GridLayoutManager cellGridLayoutManager;
    private OnBackPressedCallback criticalBackCallback;
    private boolean hasRenderedUiState;
    private boolean previousDashboardVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarInsets();

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        setupSimulatorSheetLauncher();
        setupCriticalBackHandling();

        viewModel.getUiStateLiveData().observe(this, this::renderUiState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && binding != null) {
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsetsCompat.Type.navigationBars());
        }
    }

    private void setupRecyclerView() {
        cellAdapter = new CellVoltageAdapter();
        cellGridLayoutManager = new GridLayoutManager(this, 1);
        binding.recyclerBatteryCells.setLayoutManager(cellGridLayoutManager);
        binding.recyclerBatteryCells.setAdapter(cellAdapter);
        binding.recyclerBatteryCells.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    int availableWidth = right - left
                            - view.getPaddingLeft()
                            - view.getPaddingRight();
                    int minimumCellWidth = getResources()
                            .getDimensionPixelSize(R.dimen.cell_item_min_width);
                    int spanCount = Math.max(1, availableWidth / minimumCellWidth);
                    if (cellGridLayoutManager.getSpanCount() != spanCount) {
                        cellGridLayoutManager.setSpanCount(spanCount);
                    }
                });
    }

    private void applySystemBarInsets() {
        int initialLeft = binding.rootLayout.getPaddingLeft();
        int initialTop = binding.rootLayout.getPaddingTop();
        int initialRight = binding.rootLayout.getPaddingRight();
        int initialBottom = binding.rootLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.rootLayout);
    }

    private void setupClickListeners() {
        binding.buttonLandingDemo.setOnClickListener(v -> viewModel.startDemoMode());
        binding.buttonLandingRealTime.setOnClickListener(v -> viewModel.startRealTimeMode());

        binding.buttonConnectionAction.setOnClickListener(v -> {
            MainUiState state = viewModel.getUiStateLiveData().getValue();
            if (state == null || state.getConnectionState() != ConnectionState.CONNECTED) {
                viewModel.connectToDrone();
            } else {
                viewModel.disconnectFromDrone();
            }
        });

        binding.switchSpraySafety.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setSpraySafetyEnabled(isChecked);
        });

        binding.buttonCriticalBack.setOnClickListener(v -> viewModel.minimizeCriticalAlert());
    }

    private void setupCriticalBackHandling() {
        criticalBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                binding.buttonCriticalBack.performClick();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, criticalBackCallback);
    }

    private void setupSimulatorSheetLauncher() {
        ViewGroup container = (ViewGroup) binding.layoutDebugControls;
        container.removeAllViews();
        container.setPadding(dp(8), dp(4), dp(8), dp(4));
        if (container instanceof android.widget.HorizontalScrollView) {
            container.setBackgroundColor(ContextCompat.getColor(this, R.color.background));
            ((android.widget.HorizontalScrollView) container).setFillViewport(true);
            container.setHorizontalScrollBarEnabled(false);
        }

        LinearLayout launcherRow = new LinearLayout(this);
        launcherRow.setGravity(Gravity.CENTER);
        MaterialButton openSimulator = new MaterialButton(this);
        openSimulator.setText(R.string.open_simulator_scenarios);
        openSimulator.setMinHeight(dp(48));
        openSimulator.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.surface)));
        openSimulator.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.divider)));
        openSimulator.setStrokeWidth(dp(1));
        openSimulator.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        openSimulator.setOnClickListener(v -> showSimulatorSheet());
        launcherRow.addView(openSimulator, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(launcherRow, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void showSimulatorSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText(R.string.simulator_sheet_title);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setTextSize(20);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        content.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(0, dp(12), 0, 0);

        SimulatorScenario[] scenarios = {
                new SimulatorScenario(MockTelemetryDataSource.Scenario.NORMAL, R.string.scenario_normal),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.NOTICE, R.string.scenario_notice),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.WARNING_BATTERY, R.string.scenario_warning_battery),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.WARNING_CELL, R.string.scenario_warning_cell),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.CELL_IMBALANCE, R.string.scenario_cell_imbalance),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.CRITICAL_RTL, R.string.scenario_critical_rtl),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.CRITICAL_CELL, R.string.scenario_critical_cell),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.EMERGENCY_CELL, R.string.scenario_emergency),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.RAPID_SAG, R.string.scenario_rapid_sag),
                new SimulatorScenario(MockTelemetryDataSource.Scenario.TELEMETRY_LOST, R.string.scenario_telemetry_lost)
        };

        for (SimulatorScenario scenario : scenarios) {
            MaterialButton option = new MaterialButton(this);
            option.setText(scenario.labelResource);
            option.setOnClickListener(v -> {
                viewModel.setMockScenario(scenario.scenario);
                dialog.dismiss();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(8);
            options.addView(option, params);
        }

        scrollView.addView(options);
        int screenHeightDp = Math.round(
                getResources().getDisplayMetrics().heightPixels
                        / getResources().getDisplayMetrics().density);
        int scrollHeightDp = Math.min(440, Math.max(160, screenHeightDp - 160));
        content.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(scrollHeightDp)));
        dialog.setContentView(content);
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void renderUiState(MainUiState state) {
        boolean shouldFadeDashboard = hasRenderedUiState
                && !previousDashboardVisible
                && state.isDashboardSessionActive()
                && state.getConnectionState() == ConnectionState.CONNECTED;

        MainUiRenderer.render(
                binding,
                state,
                cellAdapter,
                this);
        if (shouldFadeDashboard) {
            binding.scrollContent.setAlpha(0f);
            binding.scrollContent.animate().alpha(1f).setDuration(200L).start();
        }
        previousDashboardVisible = state.isDashboardSessionActive();
        hasRenderedUiState = true;
        criticalBackCallback.setEnabled(binding.overlayCriticalAlert.getVisibility() == View.VISIBLE);
    }

    private static final class SimulatorScenario {
        private final MockTelemetryDataSource.Scenario scenario;
        private final int labelResource;

        private SimulatorScenario(MockTelemetryDataSource.Scenario scenario, int labelResource) {
            this.scenario = scenario;
            this.labelResource = labelResource;
        }
    }
}
