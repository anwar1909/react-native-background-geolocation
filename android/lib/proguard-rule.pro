# Keep all classes in your React Native module
-keep class com.marianhello.bgloc.react.** { *; }

# Preserve specific important classes (explicit)
-keep class com.marianhello.bgloc.react.BackgroundGeolocationModule { *; }
-keep class com.marianhello.bgloc.react.BackgroundGeolocationPackage { *; }
-keep class com.marianhello.bgloc.react.ConfigMapper { *; }

-keep class com.marianhello.bgloc.react.data.LocationMapper { *; }

-keep class com.marianhello.bgloc.react.headless.HeadlessService { *; }
-keep class com.marianhello.bgloc.react.headless.HeadlessTaskRunner { *; }

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
