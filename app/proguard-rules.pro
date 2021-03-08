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

-optimizationpasses 3 # why previous value is 2?
-allowaccessmodification
-useuniqueclassmembernames
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizations !class/unboxing/enum # https://stackoverflow.com/questions/32185060/android-proguard-failing-with-value-i-is-not-a-reference-value/32615580
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# 测试路径: /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music
-applymapping mapping.txt # todo 打补丁时需要，用于保持混淆一致
-ignorewarnings

-keep class * extends java.lang.annotation.Annotation
#-keep class com.habbyge.iwatch.** { *; }

#（Basic 包名不混合大小写
-dontusemixedcaseclassnames
#（Basic）不忽略非公共的库类
-dontskipnonpubliclibraryclasses
#（Basic）输出混淆日志
-verbose

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
#（Basic）不进行优化
#-dontoptimize
#（Basic）不进行预检验
#-dontpreverify
# Note that if you want to enable optimization, you cannot just
# include optimization flags in your own project configuration file;
# instead you will need to point to the
# "proguard-android-optimize.txt" file instead of this one from your
# project.properties file.

# 混淆注意事项第一条，保留四大组件及Android的其它组件
-keep public class * extends android.app.Activity
#（Basic）
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-keep class androidx.**{*;}
-keep class com.google.**{*;}

#（Basic）混淆注意事项第二条，保持 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}
# 混淆注意事项第四条，保持WebView中JavaScript调用的方法
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# 混淆注意事项第五条 自定义View （Basic）
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}
# （Basic）混淆注意事项第七条，保持 Parcelable 不被混淆
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}
#（Basic） 混淆注意事项第八条，保持枚举 enum 类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
#（Basic）
-keepclassmembers class **.R$* {
    public static <fields>;
}
#（Basic）保留注解
-keepattributes *Annotation*
# （Basic）排除警告
-dontwarn android.support.**
# Understand the @Keep support annotation.
# （Basic）不混淆指定的类及其类成员
#-keep class android.support.annotation.Keep
## （Basic）不混淆使用注解的类及其类成员
#-keep @android.support.annotation.Keep class * {*;}
## （Basic）不混淆所有类及其类成员中的使用注解的方法
#-keepclasseswithmembers class * {
#    @android.support.annotation.Keep <methods>;
#}
## （Basic）不混淆所有类及其类成员中的使用注解的字段
#-keepclasseswithmembers class * {
#    @android.support.annotation.Keep <fields>;
#}
## 不混淆所有类及其类成员中的使用注解的初始化方法
#-keepclasseswithmembers class * {
#    @android.support.annotation.Keep <init>(...);
#}
#保留源文件以及行号 方便查看具体的崩溃信息
-keepattributes SourceFile,LineNumberTable
