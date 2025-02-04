# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class im.vector.app.features.** { *; }

## print all the rules in a file
# -printconfiguration ../proguard_files/full-r8-config.txt

# WebRTC

-keep class org.webrtc.** { *; }
-dontwarn org.chromium.build.BuildHooksAndroid

# Jitsi (else callbacks are not called)

-keep class org.jitsi.meet.** { *; }
-keep class org.jitsi.meet.sdk.** { *; }

# React Native

# Keep our interfaces so they can be used by other ProGuard rules.
# See http://sourceforge.net/p/proguard/bugs/466/
-keep,allowobfuscation @interface com.facebook.proguard.annotations.DoNotStrip
-keep,allowobfuscation @interface com.facebook.proguard.annotations.KeepGettersAndSetters
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip

# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.proguard.annotations.DoNotStrip class *
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.proguard.annotations.DoNotStrip *;
    @com.facebook.common.internal.DoNotStrip *;
}

-keepclassmembers @com.facebook.proguard.annotations.KeepGettersAndSetters class * {
  void set*(***);
  *** get*();
}

-keep class * extends com.facebook.react.bridge.JavaScriptModule { *; }
-keep class * extends com.facebook.react.bridge.NativeModule { *; }
-keepclassmembers,includedescriptorclasses class * { native <methods>; }
-keepclassmembers class *  { @com.facebook.react.uimanager.UIProp <fields>; }
-keepclassmembers class *  { @com.facebook.react.uimanager.annotations.ReactProp <methods>; }
-keepclassmembers class *  { @com.facebook.react.uimanager.annotations.ReactPropGroup <methods>; }

-dontwarn com.facebook.react.**
-keep,includedescriptorclasses class com.facebook.react.bridge.** { *; }

-keepattributes InnerClasses

# JWT dependencies
-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }

-keep class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# JNA
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# New
-dontwarn com.google.appengine.api.urlfetch.**
-dontwarn com.google.common.io.LimitInputStream
-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector
-dontwarn com.google.javascript.jscomp.**
-dontwarn com.likethesalad.android.templates.provider.api.TemplatesProvider
-dontwarn com.yahoo.platform.yui.compressor.**
-dontwarn java.awt.**
-dontwarn org.apache.velocity.**
-dontwarn org.commonmark.ext.gfm.strikethrough.Strikethrough
-dontwarn org.mozilla.javascript.**
-dontwarn org.slf4j.**
-dontwarn org.jspecify.annotations.NullMarked
