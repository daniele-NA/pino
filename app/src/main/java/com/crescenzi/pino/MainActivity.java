package com.crescenzi.pino;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Landing screen that explains the widget, requests the location permission
 * needed to read the Wi-Fi SSID, and lets the user enable the background service
 * that keeps the widget up to date.
 */
public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> requestLocationPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    // The SSID becomes readable once location is granted; refresh either way.
                    granted -> PinoWidget.refresh(this));

    private Button backgroundButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backgroundButton = findViewById(R.id.background_button);
        backgroundButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        ensureLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBackgroundButton();
    }

    // == Permissions == //

    private void ensureLocationPermission() {
        boolean granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // == Background service == //

    private void updateBackgroundButton() {
        boolean enabled = isBackgroundServiceEnabled();
        backgroundButton.setEnabled(!enabled);
        backgroundButton.setText(enabled
                ? R.string.background_updates_enabled
                : R.string.enable_background_updates);
    }

    private boolean isBackgroundServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) {
            return false;
        }
        String service = new ComponentName(this, PinoAccessibilityService.class).flattenToString();
        return enabledServices.contains(service);
    }
}
