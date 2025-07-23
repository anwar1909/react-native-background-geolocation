# Keep all classes in your React Native module
-keep class com.anwar1909.bgloc.react.** { *; }

# Preserve specific important classes (explicit)
-keep class com.anwar1909.bgloc.react.BackgroundGeolocationModule { *; }
-keep class com.anwar1909.bgloc.react.BackgroundGeolocationPackage { *; }
-keep class com.anwar1909.bgloc.react.ConfigMapper { *; }

-keep class com.anwar1909.bgloc.react.data.LocationMapper { *; }

-keep class com.anwar1909.bgloc.react.headless.HeadlessService { *; }
-keep class com.anwar1909.bgloc.react.headless.HeadlessTaskRunner { *; }

# Required to avoid stripping React Native bridge
-keepclassmembers class * extends com.facebook.react.bridge.ReactContextBaseJavaModule {
    public <init>(...);
}

-keepclassmembers class * implements com.facebook.react.bridge.NativeModule {
    public <init>(...);
}

-keepclassmembers class * implements com.facebook.react.ReactPackage {
    public <init>(...);
}

# Prevent R8 from removing entry points for JS callbacks
-keepclassmembers class * {
    @com.facebook.react.bridge.ReactMethod <methods>;
}
