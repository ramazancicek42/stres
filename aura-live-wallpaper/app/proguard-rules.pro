# Add project specific ProGuard rules here.
-keep class com.aura.livewallpaper.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
