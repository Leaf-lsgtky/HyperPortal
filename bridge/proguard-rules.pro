# cppjieba registers TextSegmenter's native methods by class/method name in JNI_OnLoad.
-keep class com.github.leaf.hyperportal.bigbang.TextSegmenter {
    native <methods>;
}
