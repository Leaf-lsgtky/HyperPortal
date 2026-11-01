package com.github.leaf.hyperportal.system;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 */
public final class HyperPortalEntry extends XposedModule {

    private static final String TAG = "HyperPortal";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        super.onModuleLoaded(param);
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
        }
    }

    @Override
    public boolean onHotReloading(@NonNull HotReloadingParam param) {
        log(Log.INFO, TAG, "onHotReloading: hot reload requested for HyperPortal");
        return true;
    }

    @Override
    public void onHotReloaded(@NonNull HotReloadedParam param) {
        super.onHotReloaded(param);
        log(Log.INFO, TAG, "onHotReloaded process=" + param.getProcessName()
                + " isSystemServer=" + param.isSystemServer());
        if (param.isSystemServer()) {
            PortalSystemModule.initSystemServer(this, null);
        } else if ("com.android.systemui".equals(param.getProcessName())) {
            PortalSystemUiModule.initSystemUi(this, null);
        }
    }
}
