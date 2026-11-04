// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * AICR 4.13.2 added an AccessGuardUtils UID/signature check in
 * AiCrCoreService$2.onTransact.  The ordinary HyperPortal UID is rejected with
 * "onTransact denied for uid".  The module is deliberately scoped to
 * com.xiaomi.aicr; allow only HyperPortal (and system) through while leaving
 * every other caller on the original vendor check.
 */
public final class AicrAccessHook {

    private static final String TAG = "HyperPortal/AicrAccess";
    private static volatile XposedInterface sXposed;

    private AicrAccessHook() {
    }

    public static void init(XposedInterface xposed, XposedModuleInterface.PackageLoadedParam param) {
        sXposed = xposed;
        try {
            ClassLoader loader = param.getDefaultClassLoader();
            Class<?> guard = Class.forName("u8", false, loader);
            for (Method method : guard.getDeclaredMethods()) {
                if (!"a".equals(method.getName())) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 1 && types[0] == Context.class
                        && method.getReturnType() == boolean.class) {
                    xposed.hook(method)
                            .setId("aicr-access-guard")
                            .intercept(AicrAccessHook::onAccessGuard);
                    Log.i(TAG, "hooked AICR AccessGuardUtils: " + method);
                    return;
                }
            }
            Log.w(TAG, "AICR AccessGuardUtils method not found");
        } catch (Throwable t) {
            Log.w(TAG, "install AICR access hook failed", t);
        }
    }

    private static Object onAccessGuard(XposedInterface.Chain chain) throws Throwable {
        try {
            List<Object> args = chain.getArgs();
            Context context = null;
            if (args != null && !args.isEmpty() && args.get(0) instanceof Context) {
                context = (Context) args.get(0);
            }
            int callingUid = Binder.getCallingUid();
            if (callingUid == Process.SYSTEM_UID || isAllowedUid(context, callingUid)) {
                Log.i(TAG, "allow AICR OCR caller uid=" + callingUid);
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "AICR access guard inspect failed", t);
        }
        return chain.proceed();
    }

    private static boolean isAllowedUid(Context context, int uid) {
        if (context == null) {
            return false;
        }
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageUid("com.github.leaf.hyperportal", 0) == uid
                    || pm.getPackageUid("com.android.systemui", 0) == uid;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return false;
        }
    }
}
