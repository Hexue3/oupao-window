package com.oupao.wiz

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/**
 * The Miuix-owned shell.
 *
 * Owns exactly three roots — 功能 (14) / 主页 (0) / 日志 (5) — and nothing else: every feature
 * subpage keeps its proven View implementation and is reached through the feature cards.
 * MainActivity decides which legacy page is current and calls [sync]; the three roots are
 * Compose-owned and never call pageShow().
 */
object MiuixShell {
    @JvmStatic val selectedTab = mutableIntStateOf(1)

    /** The legacy page id currently shown; the shell renders the migrated Compose version of it. */
    @JvmStatic val currentPage = mutableIntStateOf(1)

    /**
     * 深链：从别的进程（比如 ColorOS「通知与控制中心」里那行「通知收纳」）直接打开某一页。
     * 现在只用来跳到「功能」页下的某个子页（[id] 就是 MIGRATED_PAGES 的 key）。
     */
    @JvmStatic
    fun openPage(id: Int) {
        if (id <= 0) return
        selectedTab.intValue = 0          // 0 = 「功能」tab
        currentPage.intValue = id
        forwardNav.value = true
    }

    /**
     * Bumped whenever the shell's own back affordance (the page arrow) is used, so MiuixApp can run
     * the same slide-out the system gesture runs.
     */
    @JvmStatic val backRequests = mutableIntStateOf(0)

    /** True while the next page change is a forward navigation (slide the new page in). */
    @JvmStatic val forwardNav = mutableStateOf(true)

    /**
     * 当前打开的模块内弹窗（OverlayDialog）数量。
     *
     * 弹窗打开时 shell 的 [PredictiveBackHandler] 必须让位，否则侧滑返回会把整页滑走，
     * 而不是先关闭弹窗（用户报的「选择卡片呼出后侧滑直接返回页面」）。
     */
    @JvmStatic val popupDepth = mutableIntStateOf(0)
    /** 弹窗正在播放收回动画（窗口已不可触摸/聚焦，返回交给宿主）。 */
    @JvmStatic val popupClosing = mutableStateOf(false)

    /**
     * 弹窗刚关闭的时间戳（uptimeMillis）。
     *
     * 侧滑返回关闭弹窗时，弹窗会在手势过程中把 show 置 false，popupDepth 立刻回落。
     * 同一次手势如果此时被 shell 重新接管，就会把整页滑走 —— 所以弹窗关闭后的一小段
     * 时间窗内仍然拒绝 shell 的返回处理。
     */
    @JvmStatic var popupClosedAt: Long = 0L

    /** 弹窗关闭后的返回保护窗口结束时 +1，触发重组让 shell 重新接管返回。 */
    @JvmStatic val popupGuardEpoch = mutableIntStateOf(0)
    private val popupGuardHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @JvmStatic
    fun schedulePopupGuardExpiry() {
        popupGuardHandler.removeCallbacksAndMessages(null)
        popupGuardHandler.postDelayed({ popupGuardEpoch.intValue++ }, 450L)
    }

    /** 弹窗打开中，或刚被侧滑关闭（同一次手势的尾巴）：此时返回只属于弹窗。 */
    @JvmStatic
    fun popupConsumesBack(): Boolean =
        popupDepth.intValue > 0 ||
            android.os.SystemClock.uptimeMillis() - popupClosedAt < 350L

    /** 真正需要拦截页面返回的阶段：弹窗还开着且不在收回动画中。 */
    @JvmStatic
    fun popupBlocksPageBack(): Boolean = popupDepth.intValue > 0 && !popupClosing.value

    internal const val FEATURES_PAGE = 14
    internal const val HOME_PAGE = 0
    internal const val LOG_PAGE = 5

    /**
     * Pages the Miuix shell owns. MainActivity.show() asks this instead of hard-coding the three
     * roots, so a page becomes Compose-owned by being added to [MIGRATED_PAGES] — no View edit.
     */
    @JvmStatic fun ownsPage(page: Int): Boolean =
        page == HOME_PAGE || page == LOG_PAGE || page == FEATURES_PAGE ||
            MIGRATED_PAGES.containsKey(page) || CUSTOM_PAGES.contains(page)

    @JvmStatic fun requestBack() {
        forwardNav.value = false
        backRequests.intValue++
    }

    /**
     * 打开模块界面时申请「修改系统设置」权限（旧版是日志页里一个需要手动点的按钮）。
     *
     * 只要还没授权，每次冷启动都会再弹一次（用户要求「直到授权成功」）；同一个进程里只弹一次，
     * 避免 Compose 重组/配置变化时反复跳系统设置。
     */
    @JvmStatic
    fun requestWriteSettings(activity: android.app.Activity) {
        runCatching {
            if (android.provider.Settings.System.canWrite(activity)) return
            if (permAskedThisProcess) return
            permAskedThisProcess = true
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = android.net.Uri.parse("package:" + activity.packageName)
            activity.startActivity(intent)
        }
    }

    /** 每次冷启动重置（进程级），所以没授权时每次开界面都会再问一次。 */
    @JvmStatic
    var permAskedThisProcess = false

    @JvmStatic fun sync() {
        val legacyPage = MainActivity.cur
        val previous = currentPage.intValue
        // Forward pushes slide in from the right; returning to the feature directory is a "back"
        // move and must not. Set it BOTH ways — previously only false was ever written, so a single
        // back disabled the forward slide-in for the rest of the process ("进入子页没动画").
        if (previous != legacyPage) forwardNav.value = legacyPage != FEATURES_PAGE
        currentPage.intValue = legacyPage
        selectedTab.intValue = when {
            legacyPage == HOME_PAGE -> 1
            legacyPage == FEATURES_PAGE -> 0
            legacyPage == LOG_PAGE -> 2
            else -> 1
        }
        val shell = ownsPage(legacyPage)
        val host = MainActivity.pageBox?.findViewById<android.view.View>(MIUIX_HOST_ID)
        if (host != null) {
            host.visibility = if (shell) android.view.View.VISIBLE else android.view.View.GONE
            if (shell) host.bringToFront()
        }
        // Back ownership follows the visible layer: while a Compose page is shown the shell drives
        // the predictive-back animation itself, so the legacy raw OnBackInvokedCallback (which
        // animates View arrays that no longer exist for these pages) must stay unregistered.
        val act = MainActivity.inst
        if (act != null) {
            runCatching {
                if (shell) MainActivity.unregBack(act) else MainActivity.regBack(act)
            }
        }
    }

    private const val MIUIX_HOST_ID = 0x4f555041

    @JvmStatic fun create(activity: MainActivity): android.view.View =
        ComposeView(activity).apply {
            id = MIUIX_HOST_ID
            layoutParams = android.widget.FrameLayout.LayoutParams(-1, -1)
            setContent { MiuixApp(activity) }
        }

    @JvmStatic fun onLogVisibility(visible: Boolean, center: LogCenter?) {
        if (center == null) return
        if (visible) center.attach() else center.detach()
    }

    @JvmStatic fun selectTab(tab: Int) {
        when (tab) {
            0 -> MainActivity.navTo(FEATURES_PAGE)
            1 -> MainActivity.navTo(HOME_PAGE)
            2 -> MainActivity.navTo(LOG_PAGE)
        }
    }
}

/**
 * 每层页面都铺一层**不透明**的页面底色（底色 + 顶部那点 key-color 渐变），
 * 这样切页/拖动时不会互相透出来（旧 UI 每页都是 pageBg() 实底）。
 */
/** 底部 dock 的高度占位（8 + 54 + 12 + 导航栏），页面内容的底部留白用它。 */
@Composable
private fun dockSpace(): androidx.compose.ui.unit.Dp =
    64.dp + 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
@Composable
private fun Modifier.pageSurface(): Modifier {
    val scheme = MiuixTheme.colorScheme
    return this
        .background(scheme.surface)
        .background(
            Brush.verticalGradient(
                0f to scheme.primary.copy(alpha = 0.10f),
                0.28f to scheme.primary.copy(alpha = 0.03f),
                0.55f to Color.Transparent,
                1f to Color.Transparent,
            )
        )
}
/** One level of visual depth behind the glass bar: a soft key-color wash over the page canvas. */
@Composable
private fun PageBackdrop(content: @Composable BoxScope.() -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to scheme.primary.copy(alpha = 0.10f),
                0.28f to scheme.primary.copy(alpha = 0.03f),
                0.55f to scheme.surface,
                1f to scheme.surface,
            )
        ),
        content = content
    )
}

@Composable
private fun MiuixApp(activity: MainActivity) {
    val dark = (LocalContext.current.resources.configuration.uiMode and 48) == 32
    val controller = remember(dark) {
        ThemeController(ColorSchemeMode.System, keyColor = Color(0xFF3482FF))
    }
    // The host is a hand-built ComposeView inside a legacy layout, so the owners that
    // ComponentActivity.setContent() would install are missing. Miuix's dialogs (OverlayDialog et al)
    // read LocalNavigationEventDispatcherOwner directly and throw
    // "No NavigationEventDispatcher was provided" without it — provide both here once.
    // parent = null: this dictating shell is the ROOT dispatcher. The default parent reads
    // LocalNavigationEventDispatcherOwner.current and throws when nobody provided it.
    val navOwner = androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner(parent = null)
    CompositionLocalProvider(
        androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner provides navOwner,
        androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides activity,
    ) {
        MiuixTheme(controller = controller) {
            PageBackdrop {
            // 没授权就弹「修改系统设置」权限页；授权前每次冷启动都会弹一次。
            LaunchedEffect(Unit) {
                MiuixShell.requestWriteSettings(activity)
                // 每次进入模块界面自动跑一次诊断，日志页不再需要手动点。
                runCatching {
                    MainActivity.logCenter?.let {
                        it.attach()
                        it.runDiagnostic()
                    }
                }
            }
            val pager = rememberPagerState(initialPage = MiuixShell.selectedTab.intValue, pageCount = { 3 })
            val lastTabSwitchAt = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            LaunchedEffect(MiuixShell.selectedTab.intValue, MiuixShell.currentPage.intValue) {
                val tab = MiuixShell.selectedTab.intValue
                if (pager.currentPage != tab || pager.currentPageOffsetFraction != 0f) {
                    // ★ 连续快速点按时：离上一次切页不到 280ms 就直接**跳**过去，
                    //   不然翻页动画会排队，看起来就是"点了没反应 / 药丸跟不上页面"。
                    val now = android.os.SystemClock.uptimeMillis()
                    val rapid = now - lastTabSwitchAt.longValue < 280L
                    lastTabSwitchAt.longValue = now
                    if (Cfg.getProp("persist.oupao.dock_dbg", "0") == "1") {
                        android.util.Log.i("oupao", "DockPage t=$now tab=$tab rapid=$rapid")
                    }
                    if (rapid) pager.scrollToPage(tab) else pager.animateScrollToPage(tab)
                }
            }
            // Insets are owned here and only here. Scaffold must not consume the system bars
            // (contentWindowInsets = 0): the legacy activity window is not inset-aware, so the
            // status-bar height is applied explicitly to the content, and the glass bar keeps its own
            // navigationBarsPadding(). Pages below therefore add no insets of their own.
            val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            // 底部 dock 的占位（8 top + 64 胶囊 + 14 bottom + 导航栏 inset）；
            // 以前它是 Scaffold 的 bottomBar，切页时不会跟着页面滑，返回时就是「直接闪出来」。
            // ---- 旧版那套「可预测返回」的 Compose 版 ------------------------------------
            // 旧 UI 的观感：前进时新页从右侧滑入；返回时当前页向右滑走（400ms、
            // PathInterpolator(0.4,0,0.2,1)）；手势返回时页面跟手、松手完成或弹回。
            //
            // 关键点：顶层渲染的是 shownPage，而不是 MainActivity 的 currentPage。
            // 返回动画结束时「offset 归零」和「换页」必须在同一帧完成，否则会看到
            // 新页在屏幕外一帧、或者旧页瞬移回来 —— 那就是用户报的「返回功能页闪一下」。
            val density = LocalDensity.current
            val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
            val slideEasing = remember { CubicBezierEasing(0.4f, 0f, 0.2f, 1f) }
            val offset = remember { Animatable(0f) }
            val shownPage = remember { mutableIntStateOf(MiuixShell.currentPage.intValue) }
            val shownSpec = MIGRATED_PAGES[shownPage.intValue]

            // 跟着滑的「下层页面」：返回时是返回目标页，前进时是刚才那一页。
            val behindPage = remember { mutableIntStateOf(-1) }
            val gestureScope = rememberCoroutineScope()

            // 前进 / 外部切页（含从旧 View 页切回来）：新页从右侧滑入
            fun isRoot(p: Int) =
                p == MiuixShell.FEATURES_PAGE || p == MiuixShell.HOME_PAGE || p == MiuixShell.LOG_PAGE

            LaunchedEffect(MiuixShell.currentPage.intValue) {
                val now = MiuixShell.currentPage.intValue
                if (now == shownPage.intValue) return@LaunchedEffect
                // 三个根页面之间切换（点 dock / 拖动 dock）：整层不滑，改由 pager 平移过去，
                // 这样点 tab 也是「平移切页」而不是整页从右边推进来。
                if (isRoot(shownPage.intValue) && isRoot(now)) {
                    shownPage.intValue = now
                    return@LaunchedEffect
                }
                SLog.i("shell: page=$now shown=${shownPage.intValue} forward=${MiuixShell.forwardNav.value}")
                if (MiuixShell.forwardNav.value) {
                    behindPage.intValue = shownPage.intValue
                    shownPage.intValue = now
                    offset.snapTo(widthPx)
                    offset.animateTo(0f, tween(durationMillis = 400, easing = slideEasing))
                } else {
                    // 返回路径由 runBack() 自己完成（同步换页 + 偏移归零），这里只兜底
                    shownPage.intValue = now
                    offset.snapTo(0f)
                }
            }

            suspend fun runBack() {
                // 气泡位置编辑层是 View overlay：返回应先关闭它，不能把整页滑走。
                if (MainActivity.posOn()) {
                    MainActivity.closePosEditor()
                    return
                }
                // 弹窗还开着 / 刚被侧滑关闭：这次返回属于弹窗，不要动整页。
                // 只有弹窗仍打开时才拦截；grace 窗口的返回是要真正执行的。
                if (MiuixShell.popupBlocksPageBack()) return
                val target = MainActivity.backTarget(shownPage.intValue)
                behindPage.intValue = target
                offset.animateTo(widthPx, tween(durationMillis = 260, easing = slideEasing))
                MiuixShell.forwardNav.value = false
                if (target < 0) {
                    // 根页返回＝退出界面：保持滑出的样子直接 finish，别再 snap 回来闪一帧
                    MainActivity.finishNow()
                    return
                }
                // 顺序很重要：先把 pager 挪到目标 tab、再换页、最后偏移归零。
                // 旧代码换页时 selectedTab 还停在子页的值（sync() 对子页一律给 1=主页），
                // 于是顶层会先画出一帧「主页」，再跳到功能页 —— 那就是返回时闪一下的真凶。
                when (target) {
                    MiuixShell.FEATURES_PAGE -> { MiuixShell.selectedTab.intValue = 0; pager.scrollToPage(0) }
                    MiuixShell.HOME_PAGE -> { MiuixShell.selectedTab.intValue = 1; pager.scrollToPage(1) }
                    MiuixShell.LOG_PAGE -> { MiuixShell.selectedTab.intValue = 2; pager.scrollToPage(2) }
                }
                // 顶层换成目标页（此刻它在屏幕外 widthPx 处，看不见），再把偏移归零。
                shownPage.intValue = target
                offset.snapTo(0f)
                behindPage.intValue = -1
                MainActivity.navTo(target)
            }

            // backRequests 是进程级计数器：启动时可能有上一轮残留值，
            // 必须先记下当前值，只处理之后新增的请求，否则一打开界面就会被旧返回关掉。
            var handledBackReq by remember {
                mutableIntStateOf(MiuixShell.backRequests.intValue)
            }
            LaunchedEffect(MiuixShell.backRequests.intValue) {
                val req = MiuixShell.backRequests.intValue
                if (req > handledBackReq) {
                    handledBackReq = req
                    runBack()
                }
            }

            // 根页面按返回 = 退出到桌面（backTarget < 0）。这种情况不能自己画「下层页」：
            // 以前兜底画功能目录，手势拖动时就变成「功能页 → 功能页」，看不到桌面。
            // 交给系统原生 predictive back（此时 legacy raw 回调已 unregBack），
            // 由系统做退出过渡，下层就是真实的桌面。
            val shownExitsApp = remember(shownPage.intValue) {
                MainActivity.backTarget(shownPage.intValue) < 0
            }
            // 返回处理单独成子组合：弹窗开关只重组这一小块，不再连带重算底栏药丸。
            ShellBackHandler(
                // 根页面交给系统原生 predictive back（真实回桌面动画）。
                enabledBase = MiuixShell.ownsPage(shownPage.intValue) && !shownExitsApp,
                exitAction = null,
                onStart = {
                    behindPage.intValue = MainActivity.backTarget(shownPage.intValue)
                    SLog.i("shell: back start from=" + shownPage.intValue + " to=" + behindPage.intValue)
                },
                onProgress = { p -> offset.snapTo(widthPx * p) },
                onBack = { runBack() },
                onCancel = {
                    gestureScope.launch {
                        offset.animateTo(0f, tween(durationMillis = 200, easing = slideEasing))
                    }
                },
            )

            // 液态玻璃采样源：库的 layerBackdrop（页面内容自动被记录，dock 直接对它做 blur/lens）
            val pageBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()

            Scaffold(
                containerColor = MiuixTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { _ ->
                Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
                    // ---- 下层：返回目标页 / 来源页（跟手时露出来）----
                    val frac = (offset.value / widthPx).coerceIn(0f, 1f)
                    if (frac > 0.005f) {
                        // 只有「返回目标仍在本模块内」时才画下层页；目标 < 0 = 退出到桌面，
                        // 那种情况已交给系统原生的退出动画（见上面的 shownExitsApp），
                        // 不能再兜底画一页功能目录，否则看起来像在页内返回。
                        val bp = behindPage.intValue
                        Box(
                            Modifier.fillMaxSize().pageSurface()
                        ) {
                            if (bp >= 0) {
                                BehindPage(
                                    page = bp,
                                    statusBar = statusBar,
                                    bottom = 0.dp,
                                )
                            }
                            // ★ 1217 老版的「返回时上一页压暗」：scrim alpha = (1-progress) * 0.3
                            //   （原实现见 MainActivity.animProgF 里 scrim.setAlpha(...)）。
                            //   跟手越深越亮（往回返、露出越多 → 目标页逐渐恢复亮度）。
                            Box(
                                Modifier.fillMaxSize().background(
                                    Color.Black.copy(alpha = 0.3f * (1f - frac))
                                )
                            )
                        }
                    }
                    // ---- 上层：当前页（跟手右移 + 左缘阴影）----
                    Box(
                        Modifier.fillMaxSize()
                            // ★ 1217 老版：跟手拖动时页面被剪成圆角卡片（rdDraw 圆角 42dp、
                            //   底色 = 页面底色 + setClipToOutline）。这里用 graphicsLayer 的
                            //   clip/shape 实现（比 clip() 修饰符更稳：它同时裁掉背景和内容）。
                            .graphicsLayer {
                                translationX = offset.value
                                if (frac > 0.001f) {
                                    clip = true
                                    shape = RoundedCornerShape(42.dp)
                                } else {
                                    clip = false
                                }
                            }
                            .background(MiuixTheme.colorScheme.background)
                            .pageSurface()
                    ) {
                        if (shownSpec != null) {
                            // Migrated feature page: its own arrow runs the same slide-out as the system
                            // gesture, exactly where the legacy pages' arrow button went.
                            Box(Modifier.fillMaxSize().padding(top = statusBar)) {
                                SettingsPageScreen(shownSpec, onBack = { MiuixShell.requestBack() })
                            }
                        } else if (CUSTOM_PAGES.contains(shownPage.intValue)) {
                            // App-list pages: 生效应用名单 (6) / 选择音乐 App (3).
                            // page 9「更多应用」保持原来的 View 页（用户要求沿用原样式），不在此分支。
                            Box(Modifier.fillMaxSize().padding(top = statusBar)) {
                                when (shownPage.intValue) {
                                    3 -> MusicPickerPage(onBack = { MiuixShell.requestBack() })
                                    else -> AppWhitelistPage(onBack = { MiuixShell.requestBack() })
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize()
                                .layerBackdrop(pageBackdrop)) {
                            HorizontalPager(
                                state = pager,
                                modifier = Modifier.fillMaxSize()
                                    .padding(top = statusBar),
                                userScrollEnabled = false,
                            ) { tab ->
                                when (tab) {
                                    0 -> FeatureDirectory()
                                    1 -> HomePage()
                                    else -> LogPage()
                                }
                            }
                            }
                            // 底部 dock 放进页面图层里一起滑：返回功能页时它是「跟着页面上来」，
                            // 而不是像以前挂在 Scaffold.bottomBar 上直接闪出来。
                            Box(Modifier.align(Alignment.BottomCenter)) {
                                GlassTabBar(pager = pager, backdrop = pageBackdrop)
                            }
                        }
                        // 拖动时给下层压一条左缘阴影，层次感接近旧版
                        if (offset.value > 0.5f) {
                            Box(
                                Modifier.align(Alignment.CenterStart).width(10.dp).fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.18f * (1f - frac)),
                                                Color.Transparent,
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
}


/**
 * 返回过程中露在最下层的页面（Back 的目标页 / 前进时的来源页）。
 * 旧 View 页（例如 9「更多应用」）没办法在这里预览，就留空（只有底色）。
 */
@Composable
private fun ShellBackHandler(
    enabledBase: Boolean,
    exitAction: (() -> Unit)? = null,
    onStart: () -> Unit,
    onProgress: suspend (Float) -> Unit,
    onBack: suspend () -> Unit,
    onCancel: () -> Unit,
) {
    val popupOpen = MiuixShell.popupDepth.intValue > 0
    val popupClosing = MiuixShell.popupClosing.value
    val guardEpoch = MiuixShell.popupGuardEpoch.intValue
    val popupGrace = !popupOpen && MiuixShell.popupConsumesBack()
    key(popupOpen, popupClosing, popupGrace, guardEpoch) {
        when {
            // 收回动画期间窗口已不可触摸/聚焦：返回直接作用到页面。
            popupClosing -> BackHandler { MiuixShell.requestBack() }
            popupGrace -> {
                // 弹窗刚关闭：这次返回直接交给页面，不能吞掉（否则用户会觉得“返回没反应/回不到桌面”）。
                BackHandler { MiuixShell.requestBack() }
            }
            exitAction != null -> {
                // 根页面：手势返回/按键返回都明确回桌面。
                BackHandler { exitAction() }
            }
            else -> {
                PredictiveBackHandler(enabled = enabledBase && !popupOpen) { progress ->
                // 气泡位置编辑器打开时，返回只关闭编辑器。
                if (MainActivity.posOn()) {
                    MainActivity.closePosEditor()
                    return@PredictiveBackHandler
                }
                // 弹窗刚被侧滑关闭：同一次手势不能再让整页跟着返回。
                if (MiuixShell.popupConsumesBack()) {
                    SLog.i("shell: popup closing, skip page back")
                    return@PredictiveBackHandler
                }
                onStart()
                try {
                    progress.collect { event -> onProgress(event.progress) }
                    onBack()
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    // 手势取消时协程已取消，用独立 scope 弹回。
                    onCancel()
                    throw cancel
                }
            }
            }
        }
    }
}

@Composable
private fun BehindPage(page: Int, statusBar: androidx.compose.ui.unit.Dp, bottom: androidx.compose.ui.unit.Dp) {
    val isRoot = page == MiuixShell.FEATURES_PAGE || page == MiuixShell.HOME_PAGE || page == MiuixShell.LOG_PAGE
    // 转场中的下层页也要有真实的玻璃采样源，否则进退子页时底栏模糊会消失。
    val behindBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(behindBackdrop)) {
        when {
            page == MiuixShell.FEATURES_PAGE ->
                Box(Modifier.fillMaxSize().padding(top = statusBar, bottom = bottom)) { FeatureDirectory() }
            page == MiuixShell.HOME_PAGE ->
                Box(Modifier.fillMaxSize().padding(top = statusBar, bottom = bottom)) { HomePage() }
            page == MiuixShell.LOG_PAGE ->
                Box(Modifier.fillMaxSize().padding(top = statusBar, bottom = bottom)) { LogPage() }
            MIGRATED_PAGES.containsKey(page) ->
                Box(Modifier.fillMaxSize().padding(top = statusBar)) {
                    SettingsPageScreen(
                        MIGRATED_PAGES.getValue(page),
                        onBack = { MiuixShell.backRequests.intValue++ },
                    )
                }
            CUSTOM_PAGES.contains(page) ->
                Box(Modifier.fillMaxSize().padding(top = statusBar)) {
                    when (page) {
                        3 -> MusicPickerPage(onBack = {})
                        else -> AppWhitelistPage(onBack = {})
                    }
                }
            else -> Unit
        }
        }
        // 根页面才有底部 dock；它也在这一层里，所以拖动时下层是「连 dock 一起」露出来
        if (isRoot) {
            val tab = when (page) {
                MiuixShell.FEATURES_PAGE -> 0
                MiuixShell.HOME_PAGE -> 1
                else -> 2
            }
            Box(Modifier.align(Alignment.BottomCenter)) {
                GlassTabBar(activeTabOverride = tab, backdrop = behindBackdrop)
            }
        }
    }
}
// ---------------------------------------------------------------- page scaffolding

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = if (maxWidth > 680.dp) ((maxWidth - 640.dp) / 2).coerceAtLeast(20.dp) else 18.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = horizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, top = 18.dp, end = 4.dp, bottom = 16.dp)) {
        MiuixText(title, style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        MiuixText(subtitle, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

// ---------------------------------------------------------------- 主页

@Composable
private fun HomePage() {
    // 「隐藏桌面图标」从诊断页挪到主页（用户要求）：开关语义/写法保持原样。
    var hideIcon by remember { mutableStateOf(Cfg.hideIcon()) }
    var powerSoftReboot by remember { mutableStateOf(Cfg.powerSoftReboot()) }
    // 3121：主页两个开关都走能力检测；读不到能力位时保持可用（与 Caps 的“未知=放行”一致）
    val hideCap = remember { Caps.capOf(Cfg.K_HIDE_ICON) }
    val hideSupported = remember(hideCap) {
        if (hideCap == null) true else Caps.supported(hideCap) == true
    }
    val powerCap = remember { Caps.capOf(Cfg.K_POWER_SOFT_REBOOT) }
    val powerSupported = remember(powerCap) {
        if (powerCap == null) true else Caps.supported(powerCap) == true
    }
    ContentWidth {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(bottom = dockSpace()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.mipmap.ic_launcher),
                    "模块图标",
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    MiuixText(
                        "欧泡 · 小窗优化",
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    MiuixText(
                        "喛雪",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Spacer(Modifier.height(52.dp))
            MiuixText(
                "为欧泡带来\nFlyme 操作逻辑",
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(28.dp))
            val diagRev = MainActivity.logCenter?.revision?.intValue ?: 0
            var lspInfo by remember { mutableStateOf(true to "检测中…") }
            var rootInfo by remember { mutableStateOf(false to "检测中…") }
            LaunchedEffect(diagRev) {
                lspInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    EnvCheck.lsp()
                }
                rootInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    EnvCheck.root()
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                EnvStatusRow(
                    title = "LSP 框架",
                    detail = lspInfo.second,
                    ok = lspInfo.first,
                    okText = "已启用",
                    badText = "未启用",
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                EnvStatusRow(
                    title = "Root 权限",
                    detail = rootInfo.second,
                    ok = rootInfo.first,
                    okText = "已授权",
                    badText = "未授权",
                )
            }
            Spacer(Modifier.height(22.dp))
            SurfaceCard(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = "隐藏桌面图标",
                    summary = if (!hideSupported) Caps.NO_SUPPORT else null,
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    endActions = {
                        Switch(
                            checked = hideIcon && hideSupported,
                            onCheckedChange = {
                                if (hideSupported) {
                                    hideIcon = it
                                    Cfg.applyHideIcon(it)
                                } else {
                                    android.widget.Toast.makeText(
                                        Ui.appCtx, Caps.NO_SUPPORT, android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = hideSupported,
                        )
                    },
                    onClick = {
                        if (!hideSupported) {
                            android.widget.Toast.makeText(
                                Ui.appCtx, Caps.NO_SUPPORT, android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val next = !hideIcon
                            hideIcon = next
                            Cfg.applyHideIcon(next)
                        }
                    },
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                BasicComponent(
                    title = "电源菜单软重启",
                    summary = if (!powerSupported) Caps.NO_SUPPORT
                    else "长按电源键的关机界面显示「软重启」按钮",
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    endActions = {
                        Switch(
                            checked = powerSoftReboot && powerSupported,
                            onCheckedChange = {
                                if (powerSupported) {
                                    powerSoftReboot = it
                                    Cfg.save(Cfg.K_POWER_SOFT_REBOOT, it)
                                } else {
                                    android.widget.Toast.makeText(
                                        Ui.appCtx, Caps.NO_SUPPORT, android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = powerSupported,
                        )
                    },
                )
            }
            Spacer(Modifier.height(34.dp))
            MiuixText(
                "版本号：" + BuildConfig.VERSION_NAME,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

// ---------------------------------------------------------------- 功能

private data class Feature(
    val title: String,
    val subtitle: String,
    val page: Int,
    val icon: ImageVector,
)


@Composable
private fun FeatureDirectory() {
    // 四个功能分组，每组一张卡片（用户指定）。呼出菜单相关设置不放进模块界面。
    val groups = remember {
        listOf(
            listOf(
                Feature("Flyme 小窗", "手势、缩放与跳转小窗", 1, MiuixIcons.VerticalSplit),
                Feature("Flyme 菜单", "扇形菜单、呼出与触感", 8, MiuixIcons.Tune),
                Feature("FlymeUI", "界面细节与侧边栏", 11, MiuixIcons.Theme),
            ),
            listOf(
                Feature("Aicy 建议", "链接、音乐与场景建议", 2, MiuixIcons.Notes),
                Feature("消息提醒", "气泡、通知与应用名单", 12, MiuixIcons.Messages),
            ),
            listOf(
                Feature("字母引索", "边缘字母与快速定位", 13, MiuixIcons.Sort),
            ),
            listOf(
                Feature("游戏模式", "横屏体验与快捷操作", 10, MiuixIcons.Play),
            ),
        )
    }
    ContentWidth {
        Column(Modifier.fillMaxSize()) {
            PageHeader("功能", "每一种增强，都恰到好处", large = true)
            Box(Modifier.weight(1f)) {
                val listState = remember { PageListScroll.of("features") }
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = dockSpace() + 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    groups.forEach { group ->
                        item {
                            SurfaceCard(Modifier.fillMaxWidth()) {
                                group.forEachIndexed { index, feature ->
                                    FeatureRow(feature.title, feature.subtitle, feature.icon) {
                                        MainActivity.navTo(feature.page)
                                    }
                                    if (index < group.lastIndex) {
                                        HorizontalDivider(Modifier.padding(start = 74.dp, end = 18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 日志

@Composable
private fun LogPage() {
    // 版式回到原版：诊断一行（点了跑自检，结果就地显示）+ 日志级别一行（点开选择，样式同「斜滑呼出位置」）
    // + 隐藏桌面图标一行。实时日志流已废弃。
    val center = MainActivity.logCenter
    val diag = center?.diagnostic?.value ?: "尚未运行诊断"
    val running = center?.running?.value == true
    var levelOpen by remember { mutableStateOf(false) }
    // 3280：一键导出日志的状态文案
    var exportMsg by remember { mutableStateOf("打包 oupao.log + LSPosed 日志并分享") }
    var exporting by remember { mutableStateOf(false) }
    // 3282：onClick 里不能调 @Composable，这里先取出来
    val appCtx = LocalContext.current.applicationContext
    val level = center?.mode ?: 0
    // 3278：两档 —— 详细（默认）/ 关闭（用户要求移除「常规」）
    val levelNames = listOf("详细", "关闭")
    val levelSummaries = listOf("记录全部过程日志（默认）", "不记录任何日志")
    val levelIdx = if (level == 2) 1 else 0

    ContentWidth {
        Column(Modifier.fillMaxSize()) {
        PageHeader("诊断与日志", "让每一次运行，都清晰可见")
        // 诊断展开后内容会变长，整页可上下滑动，下面的卡片才够得到
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = dockSpace())
        ) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = "运行诊断",
                    summary = if (running) "正在读取运行状态…" else "进入模块时已自动运行",
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    endActions = {
                        MiuixText(
                            if (running) "诊断中…" else "已完成",
                            style = MiuixTheme.textStyles.body2,
                            color = if (running) MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    else MiuixTheme.colorScheme.primary,
                        )
                    },
                )
                // 诊断结果常显，不再需要点开。
                HorizontalDivider(Modifier.padding(start = 20.dp))
                MiuixText(
                    diag,
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            Spacer(Modifier.height(12.dp))
            SurfaceCard(Modifier.fillMaxWidth()) {
                // 详细 / 常规 / 关闭 合成一个选项，点开是单选弹窗（和「斜滑呼出位置」同一种交互）
                BasicComponent(
                    title = "日志级别",
                    summary = levelSummaries.getOrElse(levelIdx) { "记录全部过程日志（默认）" },
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    endActions = {
                        MiuixText(
                            levelNames.getOrElse(levelIdx) { "详细" },
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    },
                    onClick = { levelOpen = true },
                )
            }
            Spacer(Modifier.height(12.dp))
            SurfaceCard(Modifier.fillMaxWidth()) {
                BasicComponent(
                    title = "一键导出日志",
                    summary = exportMsg,
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    endActions = {
                        MiuixText(
                            if (exporting) "导出中…" else "分享",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    },
                    onClick = {
                        if (!exporting) {
                            exporting = true
                            exportMsg = "正在导出并打包…"
                            Thread {
                                val r = com.oupao.wiz.LogExport.exportAndShare(appCtx)
                                exportMsg = r
                                exporting = false
                            }.start()
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        }
    }
    ModuleOverlayDialog(
        show = levelOpen,
        title = "日志级别",
        onDismissRequest = { levelOpen = false },
    ) {
        Column(Modifier.fillMaxWidth()) {
            levelNames.forEachIndexed { index, label ->
                BasicComponent(
                    title = label,
                    summary = levelSummaries[index],
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    endActions = {
                        RadioButton(selected = levelIdx == index, onClick = {
                            center?.setMode(if (index == 0) 0 else 2)
                            levelOpen = false
                        })
                    },
                    onClick = {
                        center?.setMode(if (index == 0) 0 else 2)
                        levelOpen = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 液态玻璃底栏

/**
 * Liquid-glass dock.
 *
 * Deliberately hand-built instead of [FloatingNavigationBar] + [FloatingNavigationBarItem]: the
 * floating item renders an icon only (no label slot, no selection surface), while this dock needs
 * three labelled tabs, an even distribution and a glass capsule that the component's single-colour
 * squircle background cannot express. Geometry and semantics still come from Miuix (colorScheme,
 * NavigationBarDefaults dimensions, selectable + Role.Tab).
 *
 * Real backdrop blur stays off on purpose: miuix-blur's layerBackdrop/drawBackdrop path SIGSEGVs
 * this device's RenderThread (512-frame recursion, see HANDOFF), so depth is built from
 * translucency, a specular edge and a drop shadow instead.
 */
@Composable
private fun GlassTabBar(
    pager: androidx.compose.foundation.pager.PagerState? = null,
    activeTabOverride: Int? = null,
    backdrop: com.kyant.backdrop.Backdrop,
) {
    // 按 Kyant0/AndroidLiquidGlass 的写法：BoxWithConstraints 拿真实宽度，
    // drawBackdrop + vibrancy/blur/lens + highlight/shadow/innerShadow。
    val scheme = MiuixTheme.colorScheme
    val dark = (LocalContext.current.resources.configuration.uiMode and 48) == 32
    // 诊断用：persist.oupao.dock_dbg=1 时给栏体/药丸叠一层半透明色，方便量它们的边界
    // （白玻璃叠在白色页面上时，肉眼和普通 diff 都分不清边界）
    val dockDbg = Cfg.getProp("persist.oupao.dock_dbg", "0") == "1"
    val density = LocalDensity.current
    // 拖动中：位置用 dragFrac，另用 dragActive 单独表示「是否在拖」。
    // ★ 不能再用「dragFrac < 0」当哨兵：拖到两端时 frac 本身就是负数（橡皮筋），
    //   那样 dragging 会瞬间变 false → 药丸缩回静止态，表现就是「最左边拖不动/不跟手」。
    var dragFrac by remember { mutableFloatStateOf(0f) }
    var dragActive by remember { mutableStateOf(false) }
    var dragAnchorPx by remember { mutableFloatStateOf(0f) }   // 按下瞬间「手指 - 药丸中心」的偏移
    var pressedTab by remember { mutableIntStateOf(-1) }
    // 栏体当前的放大倍数（组合期写入、手势里读）：拖动的坐标换算必须用同一套几何，
    // 否则按压时栏体/槽位整体放大 5%，药丸会和手指错开最多十几像素。
    val barGrowState = remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    val tabsBackdrop = rememberLayerBackdrop()
    val pillBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    // px 预算（effects/layerBlock 里没有 Density 接收者）
    val blurPx = with(density) { 8.dp.toPx() }
    val lensBarPx = with(density) { 24.dp.toPx() }
    val lensPillA = with(density) { 9.dp.toPx() }
    val lensPillB = with(density) { 13.dp.toPx() }
    val innerShadowR = 8.dp

    val barH = 64.dp
    val pillH = 52.dp
    val pillPad = 6.dp
    // 按住时：栏体整体放大 barGrowMax，药丸比（放大后的）栏体四边各多出 pillOverflow
    // —— 上下左右溢出量完全相等，由几何算出来，不再写死两个缩放比例。
    val barGrowMax = 0.05f
    val pillOverflow = 6.5.dp
    // 栏体自身的底色：不透明度调高一点，这样不管背后滚过什么（灰卡/文字），
    // 观感都接近一致（用户要求「回到 2599 那种通透但不被内容影响的观感」）。
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.42f)
        else Color(0xFFFAFAFA).copy(alpha = 0.48f)

    // 拖动时页面跟着药丸**平移**：把 dragFrac 的小数部分作为 pager 的页内偏移。
    // snapshotFlow 会把高频 MOVE 合并到一帧一次，不会一事件一重排。
    LaunchedEffect(pager) {
        val state = pager ?: return@LaunchedEffect
        snapshotFlow { if (dragActive) dragFrac else -1f }.collect { f ->
            if (f < 0f) return@collect
            val p = f.coerceIn(0f, 2f)          // 拉出到两端时页面就停在第一/最后一页
            var page = p.toInt()
            var off = p - page
            if (off > 0.5f) { page += 1; off -= 1f }
            if (page > 2) { page = 2; off = 0f }
            state.scrollToPage(page, off.coerceIn(-0.5f, 0.5f))
        }
    }
    val activeTab = when {
        dragActive -> Math.round(dragFrac).toInt().coerceIn(0, 2)
        activeTabOverride != null -> activeTabOverride
        else -> MiuixShell.selectedTab.intValue
    }
    val dragging = dragActive

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 40.dp, end = 40.dp, bottom = 18.dp)
            .height(barH)
            .then(
                // 自己处理手势：不等待触摸阈值，从第一个像素就 1:1 跟手（避免起手"顿一下"）
                if (pager == null) Modifier else Modifier.pointerInput(Unit) {
                    val slotPx = size.width / 3f
                    val dockPx = size.width.toFloat()
                    // 槽中心（窗口坐标）：栏体放大 g 倍时，槽位是绕栏体中心一起外移的
                    fun centerOf(f: Float, g: Float) =
                        dockPx * (1f - g) / 2f + (f + 0.5f) * slotPx * g

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val curFrac = MiuixShell.selectedTab.intValue.toFloat()
                        // 药丸中心 = centerOf(frac, g)。按下时记「药丸中心 - 手指」的差值，
                        // 之后 center = x + offsetCenter，反解 frac。
                        // ★ 旧写法多了 /少了半槽：药丸整体偏右半个槽，在**最左边**时被
                        //   coerceIn 夹在 0 上 → 按住一动就顿一下、还不跟手；最右边恰好夹在
                        //   2 上所以看不出来。
                        val g0 = barGrowState.floatValue
                        val centerOffset = centerOf(curFrac, g0) - down.position.x
                        var started = false
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) {
                                if (started) {
                                    val target = Math.round(dragFrac).toInt().coerceIn(0, 2)
                                    dragActive = false
                                    scope.launch { pager.animateScrollToPage(target) }
                                    MiuixShell.selectTab(target)
                                }
                                break
                            }
                            val x = ch.position.x
                            if (!started && kotlin.math.abs(x - down.position.x) > 4f) {
                                started = true
                                dragActive = true
                            }
                            if (started) {
                                val g = barGrowState.floatValue
                                val center = x + centerOffset
                                // 反解 frac：药丸中心 = dockW/2 + (slotPx*(f+0.5) - dockW/2)*g
                                val raw = 1f + (center - dockPx / 2f) / (slotPx * g)
                                // 两端允许少量越界（栏体被"拉"出去），药丸本身 1:1 跟手、绝不做阻尼
                                dragFrac = raw.coerceIn(-DRAG_OVER, 2f + DRAG_OVER)
                                ch.consume()
                            }
                        }
                    }
                }
            )
    ) {
        val dockW = constraints.maxWidth.toFloat()
        // Row 有 horizontal = 4dp 内边距，真实槽宽要扣掉它，否则药丸会随索引偏移
        val rowPadPx = 0f   // 行内距归零：药丸到左右边缘的间距就等于 pillPad（与上下 6dp 一致）
        val slotW = (dockW - rowPadPx * 2f) / 3f
        val pillPadPx = with(density) { pillPad.toPx() }
        // 药丸宽 = 槽宽 - 左右各 pillPad（静止时比槽短，且始终居中）
        val pillWpx = (slotW - pillPadPx * 2f).coerceAtLeast(1f)

        val previewFrac = when {
            dragging -> dragFrac
            pressedTab >= 0 -> pressedTab.toFloat()
            else -> activeTab.toFloat()
        }
        // 槽内居中：给定 frac（0/1/2 或拖动中的小数）算药丸起始 x
        fun pillStartX(frac: Float) = rowPadPx + frac * slotW + (slotW - pillWpx) / 2f

        // 底栏重新进入组合（进入/退出子页）时，药丸必须直接出现在当前选中项上。
        // 以前固定从 0 开始再 animatTo，会看到「抖一下/从左边滑过来」。
        val initialFrac = when {
            activeTabOverride != null -> activeTabOverride.toFloat()
            dragActive -> dragFrac
            pressedTab >= 0 -> pressedTab.toFloat()
            else -> MiuixShell.selectedTab.intValue.toFloat()
        }
        val pillXAnim = remember {
            androidx.compose.animation.core.Animatable(
                rowPadPx + initialFrac * slotW + (slotW - pillWpx) / 2f
            )
        }
        // 拖动时 snap（完全跟手、不跳），松手才 spring 回落。
        // 注意：必须在 snapshotFlow 里直接读 State（dragFrac / pressedTab / selectedTab），
        // 不能读组合期算好的普通变量，否则值永远是第一次组合时的旧值 → 药丸拖不动。
        LaunchedEffect(slotW, pillWpx, activeTabOverride) {
            snapshotFlow {
                val f = when {
                    dragActive -> dragFrac
                    pressedTab >= 0 -> pressedTab.toFloat()
                    activeTabOverride != null -> activeTabOverride.toFloat()
                    else -> MiuixShell.selectedTab.intValue.toFloat()
                }
                f to dragActive
            }.collect { (f, drag) ->
                val target = rowPadPx + f * slotW + (slotW - pillWpx) / 2f
                if (drag) {
                    pillXAnim.snapTo(target)
                } else {
                    // 点选项：立刻起跑、约 120ms 到位的快弹簧。
                    // 不能 snap（那样看不出动效），也不能用软弹簧（那样显得"慢半拍"）；
                    // 连点时 animateTo 会自己改目标，动画连续不丢。
                    pillXAnim.animateTo(
                        target,
                        androidx.compose.animation.core.spring(dampingRatio = 0.90f, stiffness = 2200f),
                    )
                }
            }
        }
        val pillX = pillXAnim.value
        val press by animateFloatAsState(
            targetValue = if (dragging || pressedTab >= 0) 1f else 0f,
            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 3000f),
            label = "dockPress",
        )
        // ---- 按压几何（全部按「尺寸」算，绝不用 graphicsLayer.scale）----
        // 库是按本节点的 LayoutCoordinates 采样背景的，加 scale 会让采样区和可见区错位
        // （溢出那一圈会采成栏体，看起来就不透页面）。
        val barHpx0 = with(density) { barH.toPx() }
        val pillHStatic = with(density) { pillH.toPx() }
        val ovBase = with(density) { pillOverflow.toPx() }
        val barGrow = 1f + barGrowMax * press        // 栏体按压时整体放大
        // 拖到两端时栏体被"拉"一点：只让被拉的那一端伸长，另一端保持不动 → 拉扯感
        val dragOver = if (dragging) {
            when {
                previewFrac < 0f -> previewFrac
                previewFrac > 2f -> previewFrac - 2f
                else -> 0f
            }
        } else 0f
        // 让被拉的那一端**跟着药丸走**（药丸越界多少，栏体这一端就拉出多少），
        // 另一端不动 → 视觉上就是栏体被拉长，而且四边的间隙保持不变
        val stretchPx = dragOver * slotW * barGrow
        val restLeft = (dockW - dockW * barGrow) / 2f
        val restRight = dockW - restLeft
        // ★ 只动被拉的那一端：往左拉 → 左边界左移；往右拉 → 右边界右移。另一端留在原地。
        // （之前写成 `restLeft - stretch`，负值负负得正 → 反而把栏体缩短并右移了。）
        val barLeft = if (stretchPx < 0f) restLeft + stretchPx else restLeft
        val barRight = if (stretchPx > 0f) restRight + stretchPx else restRight
        val barWpx = barRight - barLeft
        val barOffX = barLeft
        val barHpx = barHpx0 * barGrow
        val barOffY = (barHpx0 - barHpx) / 2f
        androidx.compose.runtime.SideEffect { barGrowState.floatValue = barGrow }
        if (dockDbg) {
            android.util.Log.i(
                "oupao",
                "DockBar frac=$previewFrac dragOver=$dragOver stretch=$stretchPx barW=$barWpx offX=$barOffX grow=$barGrow"
            )
        }
        // 药丸：按住后 = 放大后的槽宽/栏高 + 两侧各 pillOverflow → 四边等距
        val ovNow = ovBase * press
        val pillWpxNow = pillWpx + (slotW * barGrow - pillWpx) * press + 2f * ovNow
        val pillHpxNow = pillHStatic + (barHpx0 * barGrow - pillHStatic) * press + 2f * ovNow
        // 落位脉冲：只在「拖动中真正跨到另一个槽」时弹一下。
        // 不能在拖动刚开始时弹（那会让药丸在按压态基础上又突然放大 10%，看着就是"顿一下"）。
        val bump = remember { androidx.compose.animation.core.Animatable(1f) }
        var prevTab by remember { mutableIntStateOf(activeTab) }
        LaunchedEffect(activeTab, dragging) {
            if (dragging && activeTab != prevTab) {
                bump.snapTo(1.08f)
                bump.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 800f))
            }
            prevTab = activeTab
        }

        // ---------- 宿主：与栏体容器等大、不裁剪 ----------
        // 放大后的栏体/药丸都比容器大，这里给它们一个与容器同尺寸的宿主，
        // 子节点用 placeExactly 精确定位（尺寸/位置单一来源，不会被算两遍）。
        Box(Modifier.matchParentSize()) {
        // ---------- 栏体（玻璃 + items）整体录进 tabsBackdrop ----------
        // 药丸在栏体范围内采到的是「栏体本身」（不透页面），溢出栏体的部分才透页面。
        // 按压时栏体也放大：用尺寸（不能用 scale，理由同上）。
        Box(
            Modifier
                .placeExactly(
                    barWpx.toInt(), barHpx.toInt(),
                    barOffX.toInt(), barOffY.toInt(),
                )
                .layerBackdrop(tabsBackdrop)
        ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(blurPx)
                        lens(lensBarPx, lensBarPx)
                    },
                    layerBlock = null,
                    shadow = null,
                    onDrawSurface = {
                        drawRect(containerColor)
                        if (dockDbg) drawRect(Color(0xFF00E5FF).copy(alpha = 0.55f))
                    },
                )
        )
        // ---------- items 行：只把它录进 tabsBackdrop（药丸里的"透过玻璃看到 tab"靠它）----------
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassTabItem("功能", MiuixIcons.GridView, activeTab == 0, Modifier.weight(1f),
                boost = if (activeTab == 0) press else 0f, onPressChange = { pressedTab = if (it) 0 else -1 }) { MiuixShell.selectTab(0) }
            GlassTabItem("主页", MiuixIcons.Home, activeTab == 1, Modifier.weight(1f),
                boost = if (activeTab == 1) press else 0f, onPressChange = { pressedTab = if (it) 1 else -1 }) { MiuixShell.selectTab(1) }
            GlassTabItem("日志", MiuixIcons.ListView, activeTab == 2, Modifier.weight(1f),
                boost = if (activeTab == 2) press else 0f, onPressChange = { pressedTab = if (it) 2 else -1 }) { MiuixShell.selectTab(2) }
        }
        }   // end tabsBackdrop container

        // ---------- 药丸：绕槽中心 / 栏体中线下对称放大 ----------
        // 位置必须现算「中心 - 当前宽/2」，不能拿静止时的左边界当锚点，
        // 否则尺寸一变大药丸只会往右长，左右溢出就不对称了。
        Box(
            Modifier
                .placeExactly(
                    (pillWpxNow * bump.value).toInt(),
                    (pillHpxNow * bump.value).toInt(),
                    // 横向：绕放大后的槽中心；纵向：绕栏体中线 → 四边溢出量相等
                    run {
                        val w = pillWpxNow * bump.value
                        // 拖动中直接读 dragFrac（不经过 Animatable / snapshotFlow，少一两帧延迟）
                        val cxStatic =
                            if (dragActive) slotW * (dragFrac + 0.5f)
                            else pillX + pillWpx / 2f
                        val cxNow = dockW / 2f + (cxStatic - dockW / 2f) * barGrow
                        (cxNow - w / 2f).toInt()
                    },
                    ((barHpx0 - pillHpxNow * bump.value) / 2f).toInt(),
                )
                .drawBackdrop(
                    backdrop = pillBackdrop,
                    shape = { Capsule() },
                    effects = {
                        // 折射做得很轻：透镜的折射带会把边缘的采样往形状内部拉，
                        // 药丸溢出栏体的那一圈本来就贴着边缘，折射一大就全采成栏体 → 看着不透。
                        lens(
                            lensPillA * press,
                            lensPillB * press,
                            depthEffect = false,
                            chromaticAberration = true,
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.55f * press) },
                    // 不要投影：溢出栏体的那一圈正好落在投影环上，会变成一圈灰边
                    shadow = null,
                    innerShadow = { InnerShadow(radius = innerShadowR, alpha = 0.10f * press) },
                    layerBlock = null,
                    onDrawSurface = {
                        // 静止时的极淡一层（让药丸看得出是个实体）；按住后完全不涂，
                        // 全靠采样 → 溢出栏体的部分就是页面本身。
                        drawRect(
                            if (dark) Color.White.copy(0.06f) else Color.Black.copy(0.06f),
                            alpha = 1f - press,
                        )
                        if (dockDbg) drawRect(Color(0xFFFF00FF).copy(alpha = 0.55f))
                    },
                )
        )

        // ---------- 栏体边缘：画在药丸之上，药丸放大压过边缘时栏体轮廓仍然可见 ----------
        // 尺寸/位置跟玻璃层完全一致（同一套 barWpx/barHpx/barOff*），否则会看到"两层栏体"
        Box(
            Modifier
                .placeExactly(
                    barWpx.toInt(), barHpx.toInt(),
                    barOffX.toInt(), barOffY.toInt(),
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (dark) 0.40f else 1.0f),
                            Color(0xFFB9C2CE).copy(alpha = if (dark) 0.35f else 0.55f),
                            Color.White.copy(alpha = if (dark) 0.30f else 0.90f),
                        )
                    ),
                    Capsule(),
                )
        )
        }   // end host
    }
}

/** 药丸最多能被拖出首/末槽多少（槽位单位）—— 留一点橡皮筋给"拉扯感"。 */
private const val DRAG_OVER = 0.10f

/**
 * 按精确尺寸测量、按精确坐标摆放，**允许超出父布局**。
 *
 * 为什么不用 `requiredHeight + offset`：那套组合在子节点比父容器大时，摆放会被算两次
 * （alignment 的居中补偿 + offset 各算一遍），实测药丸会整体偏高半个溢出量。
 * 这里自己 measure/place，尺寸和位置都只有一处来源，配合 drawBackdrop 采样也不会错位。
 */
private fun Modifier.placeExactly(wPx: Int, hPx: Int, xPx: Int, yPx: Int): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(
            androidx.compose.ui.unit.Constraints.fixed(
                wPx.coerceAtLeast(1),
                hPx.coerceAtLeast(1),
            )
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(xPx, yPx)
        }
    }
)

@Composable
private fun GlassTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    boost: Float = 0f,                 // 0=正常 1=被药丸罩住（液态玻璃放大）
    onPressChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
) {
    // 酷安那种竖排 tab：图标在上、小字在下；选中＝主色，未选中＝次要色；按下图标弹一下。
    val scheme = MiuixTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { onPressChange(pressed) }
    val content by animateColorAsState(
        if (selected) scheme.primary else scheme.onSurfaceVariantSummary,
        animationSpec = tween(160),
        label = "tabContent"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 1.16f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.34f, stiffness = 750f),
        label = "tabIconScale",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, Modifier.size(17.dp).scale(iconScale * (1f + 0.20f * boost)), tint = content)
        Spacer(Modifier.height(3.dp))
        MiuixText(
            label,
            modifier = Modifier.scale(1f + 0.08f * boost),
            style = MiuixTheme.textStyles.footnote2,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = content,
            maxLines = 1
        )
    }
}
