package com.agridrone.safety.ui.theme;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.agridrone.safety.R;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.SafetyState;

public final class UiTheme {

    private UiTheme() {
    }

    public static int getConnectionColor(Context context, ConnectionState state) {
        if (context == null) {
            return Color.parseColor("#7A8AA0");
        }

        switch (state) {
            case CONNECTED:
                return ContextCompat.getColor(context, R.color.safe_green);
            case CONNECTING:
                return ContextCompat.getColor(context, R.color.warning_amber);
            case ERROR:
                return ContextCompat.getColor(context, R.color.critical_red);
            case DISCONNECTED:
            default:
                return ContextCompat.getColor(context, R.color.disconnected);
        }
    }

    public static int getSafetyColor(Context context, SafetyState state) {
        if (context == null) {
            return Color.parseColor("#2DE2A6");
        }

        switch (state) {
            case EMERGENCY:
            case CRITICAL:
                return ContextCompat.getColor(context, R.color.critical_red);
            case CELL_FAULT:
                return ContextCompat.getColor(context, R.color.fault_orange);
            case WARNING:
                return ContextCompat.getColor(context, R.color.warning_amber);
            case NOTICE:
                return ContextCompat.getColor(context, R.color.notice_yellow);
            case NORMAL:
            default:
                return ContextCompat.getColor(context, R.color.safe_green);
        }
    }
}
