// 根项目只声明插件，不实际应用（apply false），具体应用在 app 模块。
plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
