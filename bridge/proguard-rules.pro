# ---------------------------------------------------------------------------
# HyperPortal R8 keep rules
#
# The companion APK has several entry points that are resolved by name at
# runtime, plus JNI and manifest components. R8 must not rename or remove them.
# ---------------------------------------------------------------------------

# LSPosed/libxposed entry point. The class name is listed in
# META-INF/xposed/java_init.list and is instantiated reflectively by LSPosed.
-keep class com.github.leaf.hyperportal.system.HyperPortalEntry { *; }

# Android components resolved by name from the merged manifest or from explicit
# ComponentName strings (PortalShareContract / BigBangContract).
-keep class com.github.leaf.hyperportal.HyperPortalApplication { *; }
-keep class com.github.leaf.hyperportal.PortalService { *; }
-keep class com.github.leaf.hyperportal.PortalReceiver { *; }
-keep class com.github.leaf.hyperportal.PortalFileProvider { *; }
-keep class com.github.leaf.hyperportal.ShareProxyActivity { *; }
-keep class com.github.leaf.hyperportal.ui.MainActivity { *; }
-keep class com.github.leaf.hyperportal.ui.BlacklistActivity { *; }
-keep class com.github.leaf.hyperportal.ui.TargetSettingsActivity { *; }
-keep class com.github.leaf.hyperportal.bigbang.TextSegmentationActivity { *; }

# Custom views inflated from res/layout/boom_activity_layout.xml by class name.
-keep class com.github.leaf.hyperportal.bigbang.CustomScrollView { *; }
-keep class com.github.leaf.hyperportal.bigbang.SwipeSelectView { *; }

# cppjieba registers TextSegmenter's private static native methods by class and
# method name in JNI_OnLoad. Keeping the whole class also pins the JNI class name.
-keep class com.github.leaf.hyperportal.bigbang.TextSegmenter { *; }

# The libxposed service AAR contributes this ContentProvider to the merged
# manifest. Keep it and its transitive service-helper classes.
-keep class io.github.libxposed.service.XposedProvider { *; }

# Keep line numbers and generic signatures so R8 crash reports and framework
# reflection still produce useful stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod
