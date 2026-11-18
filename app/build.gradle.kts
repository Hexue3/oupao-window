plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.oupao.wiz"
    // Miuix 0.9.4 requires API 37; runtime compatibility remains 28/29 below.
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.oupao.wiz"
        minSdk = 28
        targetSdk = 29
        versionCode = 3520
        versionName = "4.0-" + versionCode
        // 3317：只保留手机架构。依赖（androidx.graphics.path）自�?x86/x86_64�?
        // 目标设备全是 arm64（ColorOS 手机），其余架构纯属占体积�?
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            // Shipping artifact: R8 shrink + obfuscate + resource shrinking. Signed with the debug
            // key so it installs over the previous debug builds and stays sidegradable.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // The shipped module ships as a debug build; R8 shrinks the Compose/Miuix dex
            // (~31 MB of libraries) while proguard-rules.pro keeps our own package intact.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-jvm-default=enable", "-opt-in=kotlin.RequiresOptIn")
        }
    }

    packaging {
        resources {
            // xposed metadata must be packaged as-is
            excludes += setOf(
                "META-INF/*.version",
                // 3317：依赖自带的许可�?构建元数据，运行时用不到
                "META-INF/androidx/**/LICENSE.txt",
                "META-INF/com/android/build/gradle/**",
                "META-INF/version-control-info*",
                // coroutines 的调试探针（只有开 DebugProbes 才用�?
                "DebugProbesKt.bin"
            )
            merges += setOf("META-INF/xposed/*")
        }
        jniLibs.useLegacyPackaging = false
    }

    // show every javac error while porting
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000", "-Xmaxwarns", "50"))
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Shipping builds are produced offline on this machine; the vital-lint gate needs artifacts
    // that are not in the offline cache and has never been part of this project's checks.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    // 3146: App does not create its own SharedPreferences; the hook process reads
    // remote prefs provided by the framework, and writes go through the service channel.
    // XposedServiceHelper -> XposedService.getRemotePreferences(group)
    implementation("io.github.libxposed:service:102.0.0")

    // The three-page shell uses Miuix stable 0.9.4. Existing feature pages retain
    // their tested View/configuration logic and are embedded without duplication.
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    // Miuix 0.9.4's transitive Compose 1.12 libraries must stay on one release;
    // compileSdk 37 above is the matching build target.
    configurations.all {
        resolutionStrategy {
            force("androidx.compose.material3:material3-window-size-class:1.4.0")
        }
    }
    implementation("androidx.activity:activity-compose:1.10.1")
    // 闁氨鐓￠弨鍓佹捈閻ㄥ嫨鈧矁顔曠純顕€銆夌€涙劙銆夐妴宥堫洣鐠烘垵婀?ColorOS 鐠佸墽鐤嗘潻娑氣柤闁插苯缍嬮惇鐔割劀�?Preference �?�?闂団偓鐟?androidx.preference
    // �?韫囧懘銆?compileOnly閿涙俺绻栨稉?Fragment 閸欘亜婀?*閻╊喗鐖ｆ潻娑氣�?*閿涘湑olorOS 鐠佸墽鐤嗛敍澶愬櫡閻㈩煉绱濇潻鎰攽閺冩儼顩︾憴锝嗙€介�?
    //   閻╊喗鐖?App 閼奉亜鐢惃鍕亝娴?androidx閿涘苯鎯侀崚娆愬ⅵ鏉╂稒鍨滄禒?dex 閸氬簼绱伴崣妯诲�?娑撱倓閲滄稉宥呮倱閻?FragmentActivity �?
    //   �?is 閸掋倖鏌囨径杈Е閵嗕笚ragmentManager 娑旂喍绱伴幏鎺撴暪閵?
    compileOnly("androidx.preference:preference:1.2.1")
    // Miuix dialogs read LocalNavigationEventDispatcherOwner directly. A hand-built ComposeView does
    // not install it (only ComponentActivity.setContent does), so the shell provides it itself and
    // needs the API on the compile classpath (it arrives transitively at runtime via miuix-ui).
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.runtime:runtime")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
    // 缁绢垶娼ら幀浣哄箵閻犲喛绱欓惇鐔风杽閹舵ê�?閼瑰弶鏆庨敍瀛穒ew 缂佸嫪娆㈤敍宀冭泲 AndroidView 濡椼儲甯撮敍?
    implementation("com.qmdeve.liquidglass:core:1.1.0")
    // Kyant0/AndroidLiquidGlass: Compose Multiplatform 濞戝弶鈧胶骞撻悹鍐╂櫏閺嬫粌绨?
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
    // miuix-blur is intentionally absent: real backdrop blur SIGSEGVs this device's RenderThread,
    // so the glass dock is drawn from gradients/shadows and the module must not carry the dep.
}

// The Compose "group mapping" producer is optional tooling and its artifact is not in the offline
// cache; a shipping module does not need it, so skipping it keeps release builds reproducible here.
tasks.matching { it.name.contains("ComposeMapping") }
    .configureEach { enabled = false }
