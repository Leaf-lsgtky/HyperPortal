package com.github.leaf.hyperportal.system;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Modern LSPosed API 102 Module Entry Point for HyperPortal.
 *
 * Configured in module.prop with:
 *   minApiVersion=102
 *   targetApiVersion=102
 *   staticScope=true
 *   autoHotReload=true
 *   exceptionMode=protective
 *
 * Scopes:
 *   - system (system_server): 3D spatial tilt transformation (PortalSystemModule)
 *   - com.android.systemui: Aurora Glow & Bottom Target Shelf (PortalSystemUiModule)
 *   - com.xiaomi.aicr: reserved for the HyperOS AI engine OCR integration
 */
public final class HyperPortalEntry extends XposedModule {

    private static final String TAG = "HyperPortal";
    /**
     * Hot-reload breadcrumbs must be readable from logcat: {@code log()} routes
     * through libxposed, which may only reach the manager, and whether a reload
     * actually happened is exactly what has to be observable while debugging.
     */
    private static final String RELOAD_TAG = "HyperPortalHotReload";

    private static void trace(String message) {
        Log.println(Log.INFO, RELOAD_TAG, message);
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        super.onModuleLoaded(param);
        trace("onModuleLoaded process=" + param.getProcessName()
                + " isSystemServer=" + param.isSystemServer());
        log(Log.INFO, TAG, "onModuleLoaded process=" + param.getProcessName()
                + " isSystemServer=" + param.isSystemServer());
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        super.onSystemServerStarting(param);
        log(Log.INFO, TAG, "onSystemServerStarting: injecting 3D tilt hook into system_server");
        PortalSystemModule.initSystemServer(this, param);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if ("com.android.systemui".equals(param.getPackageName())) {
            log(Log.INFO, TAG, "onPackageLoaded: injecting Aurora Glow & Bottom Shelf into SystemUI");
            PortalSystemUiModule.initSystemUi(this, param);
        } else if ("com.xiaomi.aicr".equals(param.getPackageName())) {
            log(Log.INFO, TAG, "onPackageLoaded: allowing HyperPortal OCR in AICR");
            AicrAccessHook.init(this, param);
        }
    }

    @Override
    public boolean onHotReloading(@NonNull HotReloadingParam param) {
        trace("onHotReloading extras=" + param.getExtras());
        log(Log.INFO, TAG, "onHotReloading: hot reload requested for HyperPortal");
        return true;
    }

    @Override
    public void onHotReloaded(@NonNull HotReloadedParam param) {
        super.onHotReloaded(param);
        int oldHandles = 0;
        try {
            java.util.List<XposedInterface.HookHandle> handles = param.getOldHookHandles();
            if (handles != null) {
                oldHandles = handles.size();
                for (XposedInterface.HookHandle handle : handles) {
                    try {
                        // The previous classloader's hooks must not survive the
                        // swap, or every interceptor would run twice.
                        handle.unhook();
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable throwable) {
            trace("onHotReloaded unhook failed: " + throwable);
        }
        trace("onHotReloaded process=" + param.getProcessName()
                + " isSystemServer=" + param.isSystemServer()
                + " oldHandles=" + oldHandles);
        log(Log.INFO, TAG, "onHotReloaded process=" + param.getProcessName()
                + " isSystemServer=" + param.isSystemServer()
                + " oldHandles=" + oldHandles);
        if (param.isSystemServer()) {
            PortalSystemModule.initSystemServer(this, null);
        } else if ("com.android.systemui".equals(param.getProcessName())) {
            PortalSystemUiModule.initSystemUi(this, null);
        }
        trace("onHotReloaded hooks reinstalled");
    }
}
