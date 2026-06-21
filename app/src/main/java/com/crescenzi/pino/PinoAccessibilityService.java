package com.crescenzi.pino;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

/**
 * Keeps the app alive in the background so the widget stays up to date.
 *
 * <p>The system keeps enabled accessibility services running (and restarts them
 * if killed), exempt from the usual battery/standby restrictions. We don't react
 * to accessibility events; we only use the always-on lifecycle to poll the Wi-Fi
 * state every few seconds and refresh the widget when it changes.
 *
 * <p>Note: this is an accessibility-API "abuse" that is fine for a personal /
 * sideloaded build but is not allowed on the Play Store.
 */
@SuppressLint("AccessibilityPolicy")
public class PinoAccessibilityService extends AccessibilityService {

    // == Polling == //

    private static final long POLL_INTERVAL_MS = 5000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastStateKey;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            String key = PinoWidget.stateKey(PinoAccessibilityService.this);
            if (!key.equals(lastStateKey)) {
                lastStateKey = key;
                PinoWidget.refresh(getApplicationContext());
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    // == Lifecycle == //

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        lastStateKey = null;
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.removeCallbacks(poll);
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used: the service exists only to stay alive and poll.
    }

    @Override
    public void onInterrupt() {
        // Not used.
    }
}
