# ---- libxposed 入口保留（官方要求） ----
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# ---- 模块入口类 ----
-keep class com.oupao.wiz.XInit { *; }

# ---- 通过反射查找的类/方法，禁止重命名 ----
-keepclassmembers class * {
    @io.github.libxposed.annotation.* <methods>;
}

# ---- 序列化 / 配置键为字符串常量，禁止内联移除 ----
-keepclassmembers class com.oupao.wiz.Cfg {
    public static final java.lang.String *;
}

# ---- 广播接收器（AndroidManifest 声明） ----
-keep class com.oupao.wiz.WarmK { *; }
-keep class com.oupao.wiz.Boot { *; }

# ---- 日志落盘需要完整堆栈 ----
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# ---- 本模块自身的代码全部保留 ----
# 体积来自 Compose/Miuix 依赖（约 30MB dex），本包只有约 1MB：
# 保住 com.oupao.wiz.** 换来零行为风险，缩小的收益全在第三方库上。
-keep class com.oupao.wiz.** { *; }
-keep class io.github.libxposed.** { *; }

# 依赖里只有可选/注解类缺失，不影响运行
-dontwarn **

