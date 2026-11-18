package com.oupao.wiz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/** Shared layout constants. Content and the floating glass bar deliberately overlap. */
internal val PageHorizontal = 20.dp
internal val PageTop = 22.dp
internal val CardShape = RoundedCornerShape(24.dp)

@Composable
internal fun StatusBadge(text: String, ok: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(
                if (ok) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MiuixTheme.colorScheme.surfaceContainerHigh
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        MiuixText(
            text,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Medium,
            color = if (ok) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
internal fun EnvStatusRow(
    title: String,
    detail: String,
    ok: Boolean,
    okText: String,
    badText: String,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            MiuixText(title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            MiuixText(
                detail,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        StatusBadge(if (ok) okText else badText, ok)
    }
}

/** 每个设置页独立的滚动状态：弹窗开关、重组或短暂离开组合都不丢位置。 */
internal object PageScroll {
    private val states = mutableMapOf<String, ScrollState>()
    fun of(key: String): ScrollState = states.getOrPut(key) { ScrollState(0) }
}

/** 功能目录等 LazyColumn 的列表位置同样跨子页进出保持。 */
internal object PageListScroll {
    private val states = mutableMapOf<String, LazyListState>()
    fun of(key: String): LazyListState = states.getOrPut(key) { LazyListState() }
}

@Composable
internal fun ContentWidth(content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = if (maxWidth > 680.dp) ((maxWidth - 640.dp) / 2).coerceAtLeast(20.dp) else PageHorizontal
        Box(Modifier.fillMaxSize().padding(horizontal = side), content = content)
    }
}

@Composable
internal fun PageHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = PageTop, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 6.dp)) {
                Icon(MiuixIcons.Back, "返回", Modifier.size(22.dp), tint = MiuixTheme.colorScheme.onBackground)
            }
        }
        Column(Modifier.weight(1f).padding(start = if (onBack == null) 2.dp else 0.dp)) {
            MiuixText(
                title,
                style = if (large) MiuixTheme.textStyles.title1 else MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.SemiBold,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))
                MiuixText(
                    subtitle,
                    style = if (large) MiuixTheme.textStyles.body1 else MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
internal fun SectionLabel(text: String) {
    MiuixText(
        text,
        Modifier.padding(start = 4.dp, bottom = 10.dp),
        style = MiuixTheme.textStyles.footnote1,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
internal fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.border(
            0.6.dp,
            MiuixTheme.colorScheme.outline.copy(alpha = 0.45f),
            CardShape,
        ),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(),
        onClick = onClick,
        content = content,
    )
}

@Composable
internal fun FeatureIcon(icon: ImageVector, large: Boolean = false) {
    // 与主页「消息与气泡」快捷卡一致：只画图标本身，不要蓝色圆角底。
    // 外层仍占原位，保证列表里图标列和文字起点对齐。
    Box(
        Modifier.size(if (large) 48.dp else 42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(if (large) 28.dp else 25.dp), tint = MiuixTheme.colorScheme.primary)
    }
}

@Composable
internal fun FeatureRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeatureIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            MiuixText(title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            MiuixText(
                subtitle,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            MiuixIcons.Basic.ArrowRight,
            null,
            Modifier.size(17.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
    }
}

/**
 * 模块内统一样式的弹窗（自定义实现 OverlayDialog）。
 *
 * 为什么不用 OverlayDialog：它的展开是 spring、收回是固定 260ms tween，
 * 用户明确要求「收回和展开速度一致」；这里用同一个 spring 双向动画。
 * 同时弹窗打开期间把 [MiuixShell.popupDepth] +1，让 shell 的返回手势让位，
 * 返回只关闭弹窗。
 */
@Composable
internal fun ModuleOverlayDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(show) }
    var cardHeight by remember { mutableIntStateOf(0) }
    val scheme = MiuixTheme.colorScheme
    val progress = remember { Animatable(if (show) 1f else 0f) }
    val dialogSpring = spring<Float>(dampingRatio = 0.88f, stiffness = 450f)

    LaunchedEffect(show) {
        if (show) {
            visible = true
            MiuixShell.popupClosing.value = false
            progress.animateTo(1f, dialogSpring)
        } else if (visible) {
            MiuixShell.popupClosing.value = true
            progress.animateTo(0f, dialogSpring)
            visible = false
            MiuixShell.popupClosing.value = false
        }
    }

    if (!visible) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        DisposableEffect(Unit) {
            MiuixShell.popupDepth.intValue++
            onDispose {
                MiuixShell.popupDepth.intValue--
                MiuixShell.popupClosing.value = false
                MiuixShell.popupClosedAt = android.os.SystemClock.uptimeMillis()
                MiuixShell.schedulePopupGuardExpiry()
            }
        }
        // 关闭动画期间窗口还在，必须让它不可触摸，否则会吞掉紧接着的点击/侧滑。
        val dialogView = LocalView.current
        LaunchedEffect(show) {
            val window = generateSequence(dialogView.parent) { it.parent }
                .filterIsInstance<DialogWindowProvider>()
                .firstOrNull()?.window
            if (window != null) {
                if (show) {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                } else {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                }
            }
        }
        BackHandler(enabled = show) { onDismissRequest() }
        val interaction = remember { MutableInteractionSource() }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .background(scheme.windowDimming.copy(alpha = scheme.windowDimming.alpha * progress.value))
                    .clickable(interactionSource = interaction, indication = null) { onDismissRequest() }
            )
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .widthIn(max = 420.dp)
                    .onGloballyPositioned { cardHeight = it.size.height }
                    .graphicsLayer {
                        translationY = (1f - progress.value) * cardHeight
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(scheme.background)
                    .clickable(interactionSource = interaction, indication = null) {}
                    .padding(horizontal = 24.dp, vertical = 22.dp),
            ) {
                if (!title.isNullOrBlank()) {
                    MiuixText(
                        title,
                        Modifier.fillMaxWidth().padding(bottom = if (summary.isNullOrBlank()) 14.dp else 6.dp),
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
                if (!summary.isNullOrBlank()) {
                    MiuixText(
                        summary,
                        Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        style = MiuixTheme.textStyles.body1,
                        color = scheme.onSurfaceSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                content()
            }
        }
    }
}
