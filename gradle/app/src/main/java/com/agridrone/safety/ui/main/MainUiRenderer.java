package com.agridrone.safety.ui.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.agridrone.safety.R;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.SafetyState;
import com.agridrone.safety.data.model.TelemetryHealth;
import com.agridrone.safety.databinding.ActivityMainBinding;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.battery.CellHealthResult;
import com.agridrone.safety.domain.rtl.RtlResult;
import com.agridrone.safety.domain.safety.SafetyEvaluation;
import com.agridrone.safety.ui.main.adapter.CellVoltageAdapter;
import com.agridrone.safety.ui.theme.UiTheme;
import com.agridrone.safety.util.NumberFormatter;
import com.agridrone.safety.util.TimeFormatter;

public final class MainUiRenderer {

    private MainUiRenderer() {
    }

    public static void render(
            ActivityMainBinding binding,
            MainUiState state,
            CellVoltageAdapter cellAdapter,
            Context context) {

        if (binding == null || state == null || context == null) {
            return;
        }

        renderConnectionMode(binding, state, context);
        renderHeader(binding, state, context);
        renderBanner(binding, state, context);
        renderBatteryCard(binding, state, context);
        renderCellHealthCard(binding, state, cellAdapter, context);
        renderReturnSafetyCard(binding, state, context);
        renderSystemStatusCard(binding, state, context);
        renderCriticalOverlay(binding, state, context);
        renderCriticalMetricsLayout(binding, context);
    }

    private static void renderConnectionMode(
            ActivityMainBinding binding,
            MainUiState state,
            Context context) {
        ConnectionState connectionState = state.getConnectionState();
        boolean connected = connectionState == ConnectionState.CONNECTED;
        boolean dashboardVisible = state.isDashboardSessionActive();

        binding.layoutConnectionLanding.setVisibility(dashboardVisible ? View.GONE : View.VISIBLE);
        binding.headerContainer.setVisibility(dashboardVisible ? View.VISIBLE : View.GONE);
        binding.scrollContent.setVisibility(dashboardVisible ? View.VISIBLE : View.GONE);
        boolean telemetryNotLive = state.getTelemetryHealth() == TelemetryHealth.STALE
                || state.getTelemetryHealth() == TelemetryHealth.LOST;
        binding.scrollContent.setAlpha(
                dashboardVisible && (!connected || telemetryNotLive) ? 0.6f : 1.0f);
        binding.layoutDebugControls.setVisibility(
                dashboardVisible && connected && state.getConnectionMode() == ConnectionMode.DEMO
                        ? View.VISIBLE : View.GONE);
        binding.textLandingSource.setVisibility(
                !dashboardVisible && state.getConnectionMode() != ConnectionMode.UNSELECTED
                        ? View.VISIBLE : View.GONE);

        if (state.getConnectionMode() == ConnectionMode.DEMO) {
            binding.textLandingSource.setText(R.string.landing_source_demo);
        } else if (state.getConnectionMode() == ConnectionMode.REAL_TIME) {
            binding.textLandingSource.setText(R.string.real_mode_source_label);
        }

        if (connectionState == ConnectionState.CONNECTING) {
            String status = state.getConnectionMode() == ConnectionMode.REAL_TIME
                    ? state.getConnectionStatusMessage()
                    : null;
            binding.textConnectionLandingStatus.setText(
                    status != null && !status.trim().isEmpty()
                            ? status
                            : context.getString(state.getConnectionMode() == ConnectionMode.REAL_TIME
                                    ? R.string.landing_real_time_searching
                                    : R.string.landing_demo_connecting));
            binding.textConnectionLandingStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.status_warning));
            binding.indicatorLandingDot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.status_warning)));
            binding.buttonLandingDemo.setEnabled(state.getConnectionMode() != ConnectionMode.DEMO);
            binding.buttonLandingRealTime.setEnabled(state.getConnectionMode() == ConnectionMode.REAL_TIME);
            binding.buttonLandingRealTime.setText(state.getConnectionMode() == ConnectionMode.REAL_TIME
                    ? R.string.mode_real_time_stop
                    : R.string.mode_real_time_button);
            binding.progressLandingConnect.setVisibility(View.VISIBLE);
        } else if (connectionState == ConnectionState.ERROR) {
            String error = state.getConnectionErrorMessage();
            binding.textConnectionLandingStatus.setText(error != null && !error.trim().isEmpty()
                    ? context.getString(R.string.landing_error_reason, error)
                    : context.getString(R.string.landing_error));
            binding.textConnectionLandingStatus.setTextColor(ContextCompat.getColor(context, R.color.critical_red));
            binding.indicatorLandingDot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.critical_red)));
            binding.buttonLandingDemo.setEnabled(true);
            binding.buttonLandingRealTime.setEnabled(true);
            binding.buttonLandingRealTime.setText(state.getConnectionMode() == ConnectionMode.REAL_TIME
                    ? R.string.mode_real_time_retry
                    : R.string.mode_real_time_button);
            binding.progressLandingConnect.setVisibility(View.GONE);
        } else if (state.getConnectionMode() == ConnectionMode.REAL_TIME) {
            binding.textConnectionLandingStatus.setText(R.string.landing_real_time_ready);
            binding.textConnectionLandingStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary));
            binding.indicatorLandingDot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.status_idle)));
            binding.buttonLandingDemo.setEnabled(true);
            binding.buttonLandingRealTime.setEnabled(true);
            binding.buttonLandingRealTime.setText(R.string.mode_real_time_button);
            binding.progressLandingConnect.setVisibility(View.GONE);
        } else if (!connected) {
            binding.textConnectionLandingStatus.setText(R.string.landing_choose_mode);
            binding.textConnectionLandingStatus.setTextColor(ContextCompat.getColor(context, R.color.disconnected));
            binding.indicatorLandingDot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.status_idle)));
            binding.buttonLandingDemo.setEnabled(true);
            binding.buttonLandingRealTime.setEnabled(true);
            binding.buttonLandingRealTime.setText(R.string.mode_real_time_button);
            binding.progressLandingConnect.setVisibility(View.GONE);
        } else {
            binding.progressLandingConnect.setVisibility(View.GONE);
        }
    }

    private static void renderHeader(ActivityMainBinding binding, MainUiState state, Context context) {
        ConnectionState connState = state.getConnectionState();
        binding.textAppTitle.setText(state.getConnectionMode() == ConnectionMode.DEMO
                ? R.string.dashboard_title_demo
                : R.string.dashboard_title);

        int dotColor;
        String statusText;
        if (state.isDashboardSessionActive()
                && state.getTelemetryHealth() == TelemetryHealth.LOST) {
            dotColor = ContextCompat.getColor(context, R.color.status_critical);
            statusText = context.getString(R.string.state_telemetry_lost);
            boolean connected = connState == ConnectionState.CONNECTED;
            binding.buttonConnectionAction.setText(
                    connected ? R.string.disconnect : R.string.connect_to_drone);
            binding.buttonConnectionAction.setEnabled(true);
            binding.buttonConnectionAction.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.surface)));
            binding.buttonConnectionAction.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.divider)));
            binding.buttonConnectionAction.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        } else if (state.isDashboardSessionActive()
                && state.getTelemetryHealth() == TelemetryHealth.STALE) {
            dotColor = ContextCompat.getColor(context, R.color.status_warning);
            statusText = context.getString(R.string.state_telemetry_stale);
            binding.buttonConnectionAction.setText(
                    connState == ConnectionState.CONNECTED
                            ? R.string.disconnect
                            : R.string.connect_to_drone);
            binding.buttonConnectionAction.setEnabled(true);
            binding.buttonConnectionAction.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.surface)));
            binding.buttonConnectionAction.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.divider)));
            binding.buttonConnectionAction.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary));
        } else switch (connState) {
            case CONNECTED:
                dotColor = UiTheme.getConnectionColor(context, ConnectionState.CONNECTED);
                statusText = context.getString(state.getConnectionMode() == ConnectionMode.DEMO
                        ? R.string.state_connected_demo
                        : R.string.state_connected);
                binding.buttonConnectionAction.setText(R.string.disconnect);
                binding.buttonConnectionAction.setEnabled(true);
                binding.buttonConnectionAction.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.surface)));
                binding.buttonConnectionAction.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.divider)));
                binding.buttonConnectionAction.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                break;
            case CONNECTING:
                dotColor = UiTheme.getConnectionColor(context, ConnectionState.CONNECTING);
                statusText = context.getString(R.string.state_connecting);
                binding.buttonConnectionAction.setText(R.string.state_connecting);
                binding.buttonConnectionAction.setEnabled(false);
                binding.buttonConnectionAction.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)));
                binding.buttonConnectionAction.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)));
                binding.buttonConnectionAction.setTextColor(ContextCompat.getColor(context, R.color.background));
                break;
            case ERROR:
                dotColor = UiTheme.getConnectionColor(context, ConnectionState.ERROR);
                statusText = state.getConnectionErrorMessage() != null ? state.getConnectionErrorMessage() : context.getString(R.string.state_connection_error);
                binding.buttonConnectionAction.setText(R.string.connect_to_drone);
                binding.buttonConnectionAction.setEnabled(true);
                binding.buttonConnectionAction.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)));
                binding.buttonConnectionAction.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)));
                binding.buttonConnectionAction.setTextColor(ContextCompat.getColor(context, R.color.background));
                break;
            case DISCONNECTED:
            default:
                dotColor = UiTheme.getConnectionColor(context, ConnectionState.DISCONNECTED);
                statusText = context.getString(R.string.state_disconnected);
                binding.buttonConnectionAction.setText(R.string.connect_to_drone);
                binding.buttonConnectionAction.setEnabled(true);
                binding.buttonConnectionAction.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)));
                binding.buttonConnectionAction.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.text_primary)));
                binding.buttonConnectionAction.setTextColor(ContextCompat.getColor(context, R.color.background));
                break;
        }

        binding.indicatorConnectionDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));
        binding.textConnectionStatus.setText(statusText);
        binding.textConnectionStatus.setTextColor(dotColor);
    }

    private static void renderBanner(
            ActivityMainBinding binding,
            MainUiState state,
            Context context) {
        SafetyEvaluation safety = state.getSafetyEvaluation();
        SafetyState sState = safety.getPrimaryState();

        if (!state.isDashboardSessionActive()) {
            binding.bannerSafetyAlert.setVisibility(View.GONE);
            return;
        }

        if (safety.isHazardous()) {
            if (sState != SafetyState.EMERGENCY && state.isCriticalAlertMinimized()) {
                binding.bannerSafetyAlert.setVisibility(View.VISIBLE);
                binding.bannerSafetyAlert.setBackgroundResource(R.drawable.bg_banner_critical);
                binding.textSafetyBannerMessage.setText(
                        safety.getDisplayMessage() + " — " + safety.getInstruction());
                binding.textSafetyBannerMessage.setTextSize(15);
                binding.textSafetyBannerMessage.setTextColor(
                        ContextCompat.getColor(context, R.color.critical_red));
            } else {
                binding.bannerSafetyAlert.setVisibility(View.GONE);
            }
            return;
        }

        if (state.getTelemetryHealth() == TelemetryHealth.LOST) {
            binding.bannerSafetyAlert.setVisibility(View.VISIBLE);
            binding.bannerSafetyAlert.setBackgroundResource(R.drawable.bg_banner_critical);
            binding.textSafetyBannerMessage.setText(formatTelemetryAge(
                    context, state, R.string.telemetry_lost_banner_age));
            binding.textSafetyBannerMessage.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            return;
        }
        if (state.getTelemetryHealth() == TelemetryHealth.STALE) {
            binding.bannerSafetyAlert.setVisibility(View.VISIBLE);
            binding.bannerSafetyAlert.setBackgroundResource(R.drawable.bg_banner_warning);
            binding.textSafetyBannerMessage.setText(formatTelemetryAge(
                    context, state, R.string.telemetry_stale_banner_age));
            binding.textSafetyBannerMessage.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
            return;
        }

        switch (sState) {
            case NOTICE:
                binding.bannerSafetyAlert.setVisibility(View.VISIBLE);
                binding.bannerSafetyAlert.setBackgroundResource(R.drawable.bg_banner_notice);
                binding.textSafetyBannerMessage.setText(safety.getDisplayMessage());
                binding.textSafetyBannerMessage.setTextSize(15);
                binding.textSafetyBannerMessage.setTextColor(ContextCompat.getColor(context, R.color.notice_yellow));
                break;
            case WARNING:
                binding.bannerSafetyAlert.setVisibility(View.VISIBLE);
                binding.bannerSafetyAlert.setBackgroundResource(R.drawable.bg_banner_warning);
                binding.textSafetyBannerMessage.setText(safety.getDisplayMessage());
                binding.textSafetyBannerMessage.setTextSize(15);
                binding.textSafetyBannerMessage.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                break;
            case CELL_FAULT:
                binding.bannerSafetyAlert.setVisibility(View.VISIBLE);
                binding.bannerSafetyAlert.setBackgroundResource(R.drawable.bg_banner_fault);
                binding.textSafetyBannerMessage.setText(safety.getDisplayMessage() + " — " + safety.getInstruction());
                binding.textSafetyBannerMessage.setTextSize(15);
                binding.textSafetyBannerMessage.setTextColor(ContextCompat.getColor(context, R.color.fault_orange));
                break;
            case NORMAL:
            default:
                binding.bannerSafetyAlert.setVisibility(View.GONE);
                break;
        }
    }

    private static String formatTelemetryAge(
            Context context,
            MainUiState state,
            int stringResource) {
        long timestamp = state.getLastUpdateTimestampMillis();
        if (timestamp <= 0L) {
            return context.getString(stringResource, context.getString(R.string.telemetry_age_unknown));
        }
        long ageSeconds = Math.max(0L, (System.currentTimeMillis() - timestamp) / 1000L);
        return context.getString(stringResource, Long.toString(ageSeconds));
    }

    private static void renderBatteryCard(ActivityMainBinding binding, MainUiState state, Context context) {
        BatteryHealthResult battery = state.getBatteryHealth();
        SafetyState sState = state.getSafetyEvaluation().getPrimaryState();

        if (!state.isDashboardSessionActive()) {
            binding.textBatteryPercent.setText(R.string.placeholder_percent);
            binding.textBatterySafetyBadge.setText(R.string.state_disconnected);
            binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_badge);
            binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.disconnected));

            binding.textPackVoltage.setText(R.string.placeholder_value);
            binding.textCurrentDraw.setText(R.string.placeholder_value);
            binding.textTemperature.setText(R.string.placeholder_value);
            binding.textRemainingFlightTime.setText(R.string.placeholder_waiting);
            binding.textRestingVoltage.setText(R.string.placeholder_value);
            return;
        }

        binding.textBatteryPercent.setText(NumberFormatter.formatPercentPlain(battery.getBatteryPercent()));

        switch (sState) {
            case EMERGENCY:
                binding.textBatterySafetyBadge.setText(R.string.safety_emergency);
                binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_critical);
                binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.critical_red));
                break;
            case CRITICAL:
                binding.textBatterySafetyBadge.setText(R.string.safety_critical);
                binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_critical);
                binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.critical_red));
                break;
            case CELL_FAULT:
                binding.textBatterySafetyBadge.setText(R.string.safety_cell_fault);
                binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_fault);
                binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.fault_orange));
                break;
            case WARNING:
                binding.textBatterySafetyBadge.setText(R.string.safety_warning);
                binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_warning);
                binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                break;
            case NOTICE:
                binding.textBatterySafetyBadge.setText(R.string.safety_notice);
                binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_notice);
                binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.notice_yellow));
                break;
            case NORMAL:
            default:
                binding.textBatterySafetyBadge.setText(R.string.safety_normal);
                binding.textBatterySafetyBadge.setBackgroundResource(R.drawable.bg_status_safe);
                binding.textBatterySafetyBadge.setTextColor(ContextCompat.getColor(context, R.color.safe_green));
                break;
        }

        binding.textPackVoltage.setText(NumberFormatter.formatVoltage(battery.getPackVoltage()));
        binding.textCurrentDraw.setText(NumberFormatter.formatCurrent(battery.getCurrentAmps()));
        binding.textTemperature.setText(NumberFormatter.formatTemperature(battery.getTemperatureCelsius()));
        binding.textRemainingFlightTime.setText(TimeFormatter.formatDurationSeconds(battery.getRemainingFlightTimeSeconds()));
        binding.textRestingVoltage.setText(NumberFormatter.formatVoltage(battery.getRestingVoltage()));
    }

    private static void renderCellHealthCard(
            ActivityMainBinding binding,
            MainUiState state,
            CellVoltageAdapter cellAdapter,
            Context context) {

        CellHealthResult cells = state.getBatteryHealth().getCellHealth();

        if (!state.isDashboardSessionActive() || !cells.hasData()) {
            binding.textCellConfiguration.setText(R.string.placeholder_detecting);
            binding.textCellAverage.setText(R.string.placeholder_value);
            binding.textCellMin.setText(R.string.placeholder_value);
            binding.textCellMax.setText(R.string.placeholder_value);
            binding.textCellDelta.setText(R.string.placeholder_value);
            binding.textCellLowest.setText(R.string.placeholder_value);
            binding.recyclerBatteryCells.setVisibility(View.GONE);
            binding.textNoCellsPlaceholder.setVisibility(View.VISIBLE);
            cellAdapter.updateCells(null);
            return;
        }

        binding.textCellConfiguration.setText(state.getBatteryHealth().getConfiguration().getDisplayConfiguration());
        binding.textCellAverage.setText(NumberFormatter.formatCellVoltage(cells.getAverageCell()));
        binding.textCellMin.setText(NumberFormatter.formatCellVoltage(cells.getMinCell()));
        binding.textCellMax.setText(NumberFormatter.formatCellVoltage(cells.getMaxCell()));
        binding.textCellDelta.setText(NumberFormatter.formatCellDelta(cells.getCellDelta()));
        binding.textCellLowest.setText(cells.getLowestCellLabel());

        binding.recyclerBatteryCells.setVisibility(View.VISIBLE);
        binding.textNoCellsPlaceholder.setVisibility(View.GONE);
        cellAdapter.updateCells(cells.getCells());
    }

    private static void renderReturnSafetyCard(ActivityMainBinding binding, MainUiState state, Context context) {
        RtlResult rtl = state.getRtlResult();

        if (!state.isDashboardSessionActive() || rtl.getStatus() == RtlResult.Status.UNAVAILABLE) {
            binding.textDistanceHome.setText(R.string.placeholder_value);
            binding.textRtlCurrentBattery.setText(R.string.placeholder_percent);
            binding.textEstimatedReturnTime.setText(R.string.placeholder_calculating);
            binding.textRequiredRtlBattery.setText(R.string.placeholder_percent);
            binding.textSafetyMargin.setText("15%");
            binding.textRtlStatus.setText(R.string.rtl_unavailable);
            binding.textRtlStatus.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
            return;
        }

        binding.textDistanceHome.setText(NumberFormatter.formatDistance(rtl.getDistanceHomeMeters()));
        binding.textRtlCurrentBattery.setText(NumberFormatter.formatPercent(rtl.getCurrentBatteryPercent()));
        binding.textEstimatedReturnTime.setText(TimeFormatter.formatDurationSeconds(rtl.getEstimatedReturnSeconds()));
        binding.textRequiredRtlBattery.setText(NumberFormatter.formatPercent(rtl.getRequiredBatteryPercent()));
        binding.textSafetyMargin.setText(String.format("%.0f%%", rtl.getSafetyMarginPercent()));

        if (rtl.isCritical()) {
            binding.textRtlStatus.setText(R.string.rtl_critical);
            binding.textRtlStatus.setTextColor(ContextCompat.getColor(context, R.color.critical_red));
        } else {
            binding.textRtlStatus.setText(R.string.rtl_safe);
            binding.textRtlStatus.setTextColor(ContextCompat.getColor(context, R.color.safe_green));
        }
    }

    private static void renderSystemStatusCard(ActivityMainBinding binding, MainUiState state, Context context) {
        boolean isConn = (state.getConnectionState() == ConnectionState.CONNECTED);
        TelemetryHealth health = state.getTelemetryHealth();

        if (!isConn) {
            binding.textStatusTelemetry.setText(R.string.telemetry_waiting);
            binding.textStatusTelemetry.setTextColor(ContextCompat.getColor(context, R.color.disconnected));
        } else {
            switch (health) {
                case LIVE:
                    binding.textStatusTelemetry.setText("● LIVE");
                    binding.textStatusTelemetry.setTextColor(ContextCompat.getColor(context, R.color.safe_green));
                    break;
                case STALE:
                    binding.textStatusTelemetry.setText("● STALE");
                    binding.textStatusTelemetry.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                    break;
                case LOST:
                    binding.textStatusTelemetry.setText("● LOST");
                    binding.textStatusTelemetry.setTextColor(ContextCompat.getColor(context, R.color.critical_red));
                    break;
                case WAITING:
                default:
                    binding.textStatusTelemetry.setText("● WAITING");
                    binding.textStatusTelemetry.setTextColor(ContextCompat.getColor(context, R.color.disconnected));
                    break;
            }
        }

        binding.textStatusMavlink.setText(isConn ? "● ACTIVE" : "● NONE");
        binding.textStatusMavlink.setTextColor(ContextCompat.getColor(context, isConn ? R.color.safe_green : R.color.disconnected));

        binding.textStatusGps.setText(isConn ? "● VALID" : "● UNKNOWN");
        binding.textStatusGps.setTextColor(ContextCompat.getColor(context, isConn ? R.color.safe_green : R.color.disconnected));

        boolean homeSet = state.getRtlResult().isHomeKnown();
        binding.textStatusHome.setText(homeSet ? "● SET" : "● NOT SET");
        binding.textStatusHome.setTextColor(ContextCompat.getColor(context, homeSet ? R.color.safe_green : R.color.disconnected));

        binding.textStatusLogging.setText(isConn ? "● ACTIVE" : "● IDLE");
        binding.textStatusLogging.setTextColor(ContextCompat.getColor(context, isConn ? R.color.safe_green : R.color.disconnected));

        binding.textStatusVoice.setText("● READY");
        binding.textStatusVoice.setTextColor(ContextCompat.getColor(context, R.color.safe_green));

        binding.switchSpraySafety.setChecked(state.isSpraySafetyEnabled());
    }

    private static void renderCriticalOverlay(
            ActivityMainBinding binding,
            MainUiState state,
            Context context) {
        SafetyEvaluation safety = state.getSafetyEvaluation();
        SafetyState sState = safety.getPrimaryState();
        boolean minimized = sState != SafetyState.EMERGENCY && state.isCriticalAlertMinimized();

        if (state.isDashboardSessionActive()
                && safety.isHazardous()
                && !minimized) {
            binding.overlayCriticalAlert.setVisibility(View.VISIBLE);
            binding.buttonCriticalBack.setVisibility(
                    sState == SafetyState.EMERGENCY ? View.GONE : View.VISIBLE);

            if (sState == SafetyState.EMERGENCY) {
                binding.overlayCriticalAlert.setBackgroundColor(ContextCompat.getColor(context, R.color.emergency_overlay_bg));
                binding.textCriticalTitle.setText(R.string.emergency_title);
                binding.textCriticalInstruction.setText(R.string.emergency_land);
            } else {
                binding.overlayCriticalAlert.setBackgroundColor(ContextCompat.getColor(context, R.color.critical_overlay_bg));
                binding.textCriticalTitle.setText(R.string.critical_rtl_title);
                binding.textCriticalInstruction.setText(R.string.critical_rtl_message);
            }

            binding.textCriticalDetail.setText(safety.getReason());

            binding.textCriticalBattery.setText(NumberFormatter.formatPercent(state.getBatteryHealth().getBatteryPercent()));
            binding.textCriticalRequiredRtl.setText(NumberFormatter.formatPercent(state.getRtlResult().getRequiredBatteryPercent()));
            binding.textCriticalMinCell.setText(NumberFormatter.formatCellVoltage(state.getBatteryHealth().getCellHealth().getMinCell()));
            binding.textCriticalDistance.setText(NumberFormatter.formatDistance(state.getRtlResult().getDistanceHomeMeters()));
        } else {
            binding.overlayCriticalAlert.setVisibility(View.GONE);
        }
    }

    private static void renderCriticalMetricsLayout(ActivityMainBinding binding, Context context) {
        boolean portrait = context.getResources().getConfiguration().orientation
                != android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        binding.layoutCriticalMetrics.setOrientation(
                portrait ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL);
        for (int i = 0; i < binding.layoutCriticalMetrics.getChildCount(); i++) {
            View child = binding.layoutCriticalMetrics.getChildAt(i);
            android.widget.LinearLayout.LayoutParams params =
                    (android.widget.LinearLayout.LayoutParams) child.getLayoutParams();
            params.width = portrait
                    ? android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    : android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.topMargin = portrait ? context.getResources().getDimensionPixelSize(R.dimen.spacing_tiny) : 0;
            params.rightMargin = portrait ? 0 : context.getResources().getDimensionPixelSize(R.dimen.spacing_large);
            child.setLayoutParams(params);
        }
    }
}
