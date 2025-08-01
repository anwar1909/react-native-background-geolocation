# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/finch/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep all classes and members for location and sync
-keep class com.marianhello.bgloc.** { *; }
-keep class com.marianhello.bgloc.data.** { *; }
-keep class com.marianhello.bgloc.sync.** { *; }
-keep class android.accounts.** { *; }
-keep class org.json.** { *; }

# Prevent obfuscation for any class accessed via reflection
-keepnames class * {
    public <init>(...);
}
-keepattributes Signature
-keepattributes *Annotation*