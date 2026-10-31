package com.github.leaf.hyperportal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Fallback entry point for the patched AES Bridge.
 *
 * Some HyperOS builds refuse a cross-app startForegroundService() from a
 * third-party target process. An explicit broadcast to this exported receiver
 * gets a short foreground-service allowlist and can then start PortalService.
 */
public class PortalReceiver extends BroadcastReceiver {

    private static final String TAG = "DragSharePortal";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "PortalReceiver action=" + (intent == null ? "null" : intent.getAction()));
        Intent service = new Intent(context, PortalService.class);
        if (intent != null) {
            service.putExtras(intent);
        }
        service.removeExtra("keepAlive");
        PortalService running = PortalService.getInstance();
        if (running != null) {
            // Same process: deliver directly. HyperOS sometimes drops the
            // follow-up startForegroundService() from a manifest receiver,
            // which is why the portal showed an empty card.
            Log.i(TAG, "deliver directly to running service");
            running.onStartCommand(service, 0, 0);
            return;
        }
        try {
            context.startService(service);
        } catch (Throwable first) {
            Log.w(TAG, "receiver startService failed", first);
        }
    }
}
