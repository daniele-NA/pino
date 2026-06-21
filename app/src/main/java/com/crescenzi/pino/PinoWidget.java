package com.crescenzi.pino;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Home screen app widget showing the device's local Wi-Fi IP and the connected
 * Wi-Fi SSID.
 *
 * <p>Updates happen in real time: while at least one widget instance is present
 * we register a {@link ConnectivityManager} network callback backed by a
 * {@link PendingIntent}, so the system wakes the widget on every Wi-Fi change
 * without polling and even if the process was killed.
 */
public class PinoWidget extends AppWidgetProvider {

    private static final String ACTION_NETWORK_CHANGED = "com.crescenzi.pino.action.NETWORK_CHANGED";
    private static final String UNKNOWN_SSID = "<unknown ssid>";
    private static final int REQUEST_NETWORK_CHANGE = 1;
    private static final int REQUEST_OPEN_APP = 2;

    // == Provider lifecycle == //

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        registerNetworkCallback(context);
        RemoteViews views = buildRemoteViews(context);
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        registerNetworkCallback(context);
    }

    @Override
    public void onDisabled(Context context) {
        unregisterNetworkCallback(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_NETWORK_CHANGED.equals(action)) {
            updateAllInstances(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            // The network callback registration is dropped on reboot / app update; re-arm it.
            registerNetworkCallback(context);
            updateAllInstances(context);
        }
    }

    // == Rendering == //

    /** Forces a refresh of every widget instance (e.g. after a permission grant). */
    static void refresh(Context context) {
        updateAllInstances(context);
    }

    private static void updateAllInstances(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, PinoWidget.class));
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        RemoteViews views = buildRemoteViews(context);
        for (int appWidgetId : appWidgetIds) {
            manager.updateAppWidget(appWidgetId, views);
        }
    }

    private static RemoteViews buildRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pino_widget);

        String ip = localWifiIp(context);
        if (ip == null) {
            ip = context.getString(R.string.widget_value_unknown);
        }

        String ssid;
        if (!hasLocationPermission(context)) {
            ssid = context.getString(R.string.widget_permission_needed);
        } else {
            ssid = wifiSsid(context);
            if (ssid == null) {
                ssid = context.getString(R.string.widget_value_unknown);
            }
        }

        views.setTextViewText(R.id.widget_ip, ip);
        views.setTextViewText(R.id.widget_ssid, ssid);
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context));
        return views;
    }

    private static PendingIntent openAppPendingIntent(Context context) {
        return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_APP,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // == Real-time network updates == //

    private static void registerNetworkCallback(Context context) {
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        if (connectivity == null) {
            return;
        }
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        try {
            // Re-registering an equal PendingIntent throws; ignore since it is already active.
            connectivity.registerNetworkCallback(request, networkChangePendingIntent(context));
        } catch (RuntimeException ignored) {
        }
    }

    private static void unregisterNetworkCallback(Context context) {
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        if (connectivity == null) {
            return;
        }
        try {
            connectivity.unregisterNetworkCallback(networkChangePendingIntent(context));
        } catch (RuntimeException ignored) {
        }
    }

    private static PendingIntent networkChangePendingIntent(Context context) {
        Intent intent = new Intent(context, PinoWidget.class).setAction(ACTION_NETWORK_CHANGED);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQUEST_NETWORK_CHANGE, intent, flags);
    }

    // == Wi-Fi info == //

    private static String localWifiIp(Context context) {
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        if (connectivity == null) {
            return null;
        }
        Network network = connectivity.getActiveNetwork();
        if (network == null) {
            return null;
        }
        NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null;
        }
        LinkProperties linkProperties = connectivity.getLinkProperties(network);
        if (linkProperties == null) {
            return null;
        }
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                return address.getHostAddress();
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static String wifiSsid(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return null;
        }
        WifiInfo info = wifi.getConnectionInfo();
        if (info == null || info.getSSID() == null) {
            return null;
        }
        String ssid = info.getSSID();
        if (ssid.length() >= 2 && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        if (ssid.trim().isEmpty() || ssid.equals(UNKNOWN_SSID)) {
            return null;
        }
        return ssid;
    }

    private static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // == State snapshot == //

    /** Compact key of the values currently shown; used by the poller to detect changes. */
    static String stateKey(Context context) {
        String ip = localWifiIp(context);
        String ssid = hasLocationPermission(context) ? wifiSsid(context) : "no-permission";
        return ip + "|" + ssid;
    }
}
