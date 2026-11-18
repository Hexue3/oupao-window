package com.oupao.wiz

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/**
 * Declarative description of a migrated settings page.
 *
 * The legacy View builders (`Ui.card` / `Ui.row` / `Ui.rowNav` / `mkMaster` / `Sl.*`) always had the
 * same shape: an optional master switch, then cards of keyed rows. This model keeps that shape but
 * makes a row a value, so a page becomes data and one Miuix renderer owns the visual language.
 *
 * Behaviour contract kept 1:1 with the legacy UI:
 * - reads use the same `Cfg` getters with the same defaults,
 * - boolean writes go through `Cfg.save(key, value)` and value writes through the exact legacy
 *   three-step (`Cfg.saveStr` → `Ev.send(CFG)` → `Cfg.push()`), because `push()` alone is what
 *   reaches system_server and `save()` is what fires the `persist.oupao.*` / JumpConfirm side
 *   channels. A bare file write would silently never apply.
 */
internal enum class RowKind { SWITCH, NAV_SWITCH, NAV, SLIDER, CHOICE, ACTION }

/** Option list for the legacy popup pickers (右侧/左侧/两侧, 关闭此功能/单击/双击 …). */
internal class ChoiceSpec(
    val labels: List<String>,
    val codes: List<String>,
    /** Current code; "" means "this key was never written" (legacy renders a fallback label). */
    val current: () -> String,
    val write: (String) -> Unit,
    /** code -> trailing text shown on the row (legacy 14sp accent label). */
    val display: (String) -> String,
)

internal data class RowSpec(
    val title: String,
    val summary: String? = null,
    val kind: RowKind = RowKind.SWITCH,
    val key: String? = null,
    /** Optional second boolean key written/read together (合并开关用). */
    val key2: String? = null,
    /** Legacy default: `Ui.row` uses 1 unless the key is one of the historically-off switches. */
    val def: Int = 0,
    /** NAV target page id. */
    val to: Int = -1,
    /** NAV_SWITCH: open page 6 with this `sugFrom` (whitelist picker) instead of `to`. */
    val sugFrom: Int = -1,
    val min: Int = 0,
    val max: Int = 100,
    val step: Int = 1,
    val unit: String = "",
    /** SLIDER initial value (legacy `intOf(key, default, min, max)` default). */
    val sliderDefault: Int = 0,
    val choice: ChoiceSpec? = null,
    /** Live preview while dragging (legacy FlyArea.setLive / FlyPrev.setLive). */
    val onLive: ((Int) -> Unit)? = null,
    /** Preview cleanup after release (legacy FlyArea.clear / FlyPrev.clear). */
    val onRelease: (() -> Unit)? = null,
    /** Extra side effect the legacy listener ran (e.g. Cfg.applyHideIcon). */
    val afterSave: ((Boolean) -> Unit)? = null,
    /** ACTION rows: custom click (e.g. 气泡位置 → MainActivity.openPosEditor()). */
    val onAction: (() -> Unit)? = null,
    /** SLIDER: 拖到最右时写入的布尔键（如气泡常驻）。 */
    val persistKey: String? = null,
    /** SLIDER: 最右端的文案（如“常驻”）；设置后最右值显示该文案。 */
    val endLabel: String? = null,
)

internal data class CardSpec(val rows: List<RowSpec>)

internal data class PageSpec(
    val title: String,
    val subtitle: String? = null,
    val masterKey: String? = null,
    val masterLabel: String = "启用功能",
    val masterSummary: String? = null,
    val cards: List<CardSpec>,
    /** Legacy footer tip, rendered as plain text under the last card. */
    val footer: String? = null,
)

// ---------------------------------------------------------------- page registry

/**
 * Page id -> spec for every feature page already migrated to Compose.
 * `MiuixShell.ownsPage()` derives its answer from this map, so migrating a page is one edit.
 */
internal val MIGRATED_PAGES: Map<Int, PageSpec> = mapOf(
    // ---- page 11 · FlymeUI (FlyUI.build) ------------------------------------------------
    // Legacy: title "Flyme式UI", mkMaster(oupao_fly_ui, 启用功能), FOUR cards of one row each.
    11 to PageSpec(
        title = "Flyme式UI",
        masterKey = "oupao_fly_ui",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_fly_ui_bar",
                        title = "贴边小窗图标改竖线",
                        summary = "小窗贴边时，侧边图标显示为细竖线，刚贴边为白色、随后半透明",
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_fly_border_off",
                        title = "隐藏小窗边框",
                        summary = "去掉小窗和迷你窗四周的半透明描边",
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_no_splash",
                        title = "移除应用启动遮罩（非必要不建议）",
                        summary = "不再显示预览遮罩；代价是启动瞬间没有过渡画面，慢应用会像卡一下",
                        def = 0,
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_sidebar_noswipe",
                        title = "屏蔽侧边栏滑动",
                        summary = "从屏幕边缘滑动不再呼出侧边栏；「更多应用→工具→中转站」照常可用",
                        // 2799：默认关闭（用户要求）—— 以前 def 缺省是 1（默认开）
                        def = 0,
                    )
                )
            ),
            // 新增：「多任务」相关（判定方在桌面进程，见 RecentsHide.kt）
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_recents_hide_freeform",
                        title = "多任务隐藏迷你窗应用",
                        summary = "多任务界面里不显示正在以迷你窗运行的应用；普通小窗的卡片保留",
                        def = 0,
                    ),
                    RowSpec(
                        key = "oupao_recents_clear_boot",
                        title = "重启后清空多任务列表",
                        summary = "开机后首次打开多任务时清空最近任务列表（应用数据不受影响）；开关只影响下次重启",
                        def = 0,
                    )
                )
            ),
            // 新增：小窗数量上限（判定方在 system_server，见 FlexFix.hookMaxWinNum）
            // 3309：数量=1 → Flyme 逻辑（屏蔽自动贴边、超出直接关最早）；
            //       数量>=2 → 恢复原生逻辑（到上限自动贴边、贴边满两个关最旧），
            //       模块只负责解除数量上限。
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_zoom_max_on",
                        title = "限制小窗数量",
                        summary = "数量为 1 时用 Flyme 逻辑：不再自动贴边，超过就直接关最早的小窗；" +
                            "数量为 2 及以上恢复原生逻辑（到上限自动贴边），模块只解除数量上限",
                        def = 0,
                    ),
                    RowSpec(
                        key = "oupao_zoom_max",
                        title = "小窗数量上限",
                        kind = RowKind.SLIDER,
                        min = 1, max = 10, unit = " 个", sliderDefault = 3,
                        summary = "1 = Flyme 逻辑（不自动贴边）；2 及以上 = 原生逻辑",
                    ),
                    RowSpec(
                        key = "oupao_zoom_cascade",
                        title = "小窗叠放自动错开",
                        summary = "上限调大后，后来的小窗依次往下错开，不会全叠在一处；" +
                            "只在上面「限制小窗数量」开着时才生效，觉得位置怪可以关掉",
                    ),
                )
            ),
            // 新增：锁屏数字键盘滑动输入（判定方在 SystemUI 进程，见 KeyguardSlide.kt）
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_keyguard_slide",
                        title = "锁屏密码滑动输入",
                        summary = "锁屏输密码时可以像滑字母键盘一样，手指划过哪些数字就输入哪些；" +
                            "删除/确认键照旧点按",
                        def = 0,
                    )
                )
            ),
            // Flyme 式「通知收纳」：开关只在 ColorOS「设置 → 通知与控制中心」的注入卡片里，
            // 模块里这一行只是**跳转入口**（不再保留模块自建设置页）。
            CardSpec(
                listOf(
                    RowSpec(
                        title = "通知收纳",
                        summary = "长按通知即可收纳、可设置自动收纳静默通知；" +
                            "开关在「设置 → 通知与控制中心 → 通知收纳」，点这里直接跳过去",
                        kind = RowKind.ACTION,
                        onAction = { NotifBoxLauncher.openNotificationSettings() },
                    )
                )
            ),
        )
    ),

    // ---- page 10 · 游戏模式 (Game.build) ------------------------------------------------
    10 to PageSpec(
        title = "游戏模式",
        masterKey = "oupao_game_master",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_game_block",
                        title = "屏蔽左右下角呼出菜单",
                        summary = "游戏模式下不再从屏幕左右下角呼出扇形菜单",
                    ),
                    RowSpec(
                        key = "oupao_idx_game",
                        title = "游戏中屏蔽引索条",
                        summary = "游戏模式下不响应字母引索呼出",
                    ),
                    RowSpec(
                        key = "oupao_game_block_sug",
                        title = "屏蔽 aicy 建议气泡",
                        summary = "游戏模式下不再弹出建议气泡",
                    ),
                )
            )
        )
    ),

    // ---- page 4 · 振动效果：3273 已移除（振动完全跟随系统设置） ----------------

    // ---- page 13 · 字母引索 (IdxPage.build) --------------------------------------------
    13 to PageSpec(
        title = "字母引索",
        masterKey = "oupao_idx_master",
        masterLabel = "启用字母引索",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        title = "引索条呼出位置",
                        summary = "从屏幕哪一侧边缘竖滑呼出引索条",
                        kind = RowKind.CHOICE,
                        choice = ChoiceSpec(
                            labels = listOf("右侧", "左侧", "两侧"),
                            codes = listOf("R", "L", "B"),
                            current = { Cfg.idxSide() },
                            write = { Cfg.setIdxSide(it) },
                            display = { code ->
                                when (code) {
                                    "L" -> "左侧"
                                    "B" -> "两侧"
                                    else -> "右侧"
                                }
                            },
                        ),
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_idx_global",
                        title = "全局生效",
                        summary = "关闭后仅在桌面生效",
                    )
                )
            ),
        ),
        footer = "滑动引索条时，手指停留在 ★ 上 2 秒，即可呼出固定应用面板",
    ),

    // ---- page 12 · 消息提醒 (buildMenuPage -> buildMsgCard) ----------------------------
    // Legacy has NO master switch on this page; the three whitelist rows carry a chevron that
    // opens page 6 with a different sugFrom (0 = bubble apps, 3 = info apps, 1 = nohu apps).
    12 to PageSpec(
        title = "消息提醒",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_sug_msg",
                        title = "消息气泡",
                        summary = "来通知时在左下角弹头像气泡，点击以小窗打开",
                        def = 0,
                        kind = RowKind.NAV_SWITCH,
                        sugFrom = 0,
                    ),
                    RowSpec(
                        key = "oupao_sug_info",
                        title = "信息气泡",
                        summary = "头像气泡右侧显示消息内容，点整行进子页选生效应用",
                        kind = RowKind.NAV_SWITCH,
                        sugFrom = 3,
                    ),
                    RowSpec(
                        key = "oupao_nohu_all",
                        title = "屏蔽悬浮通知",
                        summary = "来通知时不弹横幅，不影响左下角气泡",
                        def = 0,
                        kind = RowKind.NAV_SWITCH,
                        sugFrom = 1,
                    ),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(key = "oupao_bub_v2", title = "合并气泡", summary = "消息+信息合成一个气泡（光晕/延伸动画），信息流沿用原逻辑"),
                    RowSpec(
                        key = "oupao_bub_dbl_close",
                        title = "气泡双击关闭",
                        summary = "开：双击气泡关闭（单击要等 0.3 秒确认没有第二下才打开）；关：单击立刻打开",
                    ),
                    RowSpec(
                        key = "oupao_bub_info_sec",
                        title = "信息显示时间",
                        kind = RowKind.SLIDER,
                        min = 1, max = 15, unit = " 秒", sliderDefault = 5,
                    ),
                    RowSpec(
                        key = "oupao_bub_sec",
                        title = "气泡停留时间",
                        summary = "拖动调节停留时长；拖到最右为常驻，点一下才关闭",
                        kind = RowKind.SLIDER,
                        min = 1, max = 16, unit = " 秒", sliderDefault = 4,
                        persistKey = "oupao_bub_persist",
                        endLabel = "常驻",
                    ),
                )
            )
        )
    ),

    // ---- page 1 · Flyme 小窗 (inline in MainActivity.onCreate, container sC) ------------
    // Legacy groups: 4 / 3 / 2 / 3 / 2 switches then a 「窗外点击关闭」three-way picker.
    1 to PageSpec(
        title = "Flyme小窗",
        masterKey = "oupao_flyop_master",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        title = "双击小窗小白条操作逻辑",
                        summary = "选择双击小窗底部小白条时执行的操作",
                        kind = RowKind.CHOICE,
                        choice = ChoiceSpec(
                            labels = listOf("关闭", "竖屏双击关闭小窗", "横屏双击转为全屏", "全部开启"),
                            codes = listOf("0", "1", "2", "3"),
                            current = {
                                val close = Cfg.getInt("oupao_double_tap_close", 0) == 1
                                val full = Cfg.getInt("oupao_landscape_fullscreen", 0) == 1
                                when {
                                    close && full -> "3"
                                    close -> "1"
                                    full -> "2"
                                    else -> "0"
                                }
                            },
                            write = { code ->
                                Cfg.save("oupao_double_tap_close", code == "1" || code == "3")
                                Cfg.save("oupao_landscape_fullscreen", code == "2" || code == "3")
                            },
                            display = { code ->
                                when (code) {
                                    "0" -> "关闭"
                                    "1" -> "竖屏双击关闭"
                                    "2" -> "横屏双击全屏"
                                    else -> "全部开启"
                                }
                            },
                        ),
                    ),
                    RowSpec(
                        title = "上滑横条转迷你窗",
                        summary = "选择触发迷你窗需要多大的上滑幅度",
                        kind = RowKind.CHOICE,
                        choice = ChoiceSpec(
                            labels = listOf("关闭", "快速上滑转迷你窗", "一点点滑动转迷你窗"),
                            codes = listOf("0", "1", "2"),
                            current = {
                                val on = Cfg.getInt("oupao_fly_mini", 0) == 1
                                val easy = Cfg.getInt("oupao_fly_mini_easy", 0) == 1
                                when {
                                    !on -> "0"
                                    easy -> "2"
                                    else -> "1"
                                }
                            },
                            write = { code ->
                                Cfg.save("oupao_fly_mini", code != "0")
                                Cfg.save("oupao_fly_mini_easy", code == "2")
                            },
                            display = { code ->
                                when (code) {
                                    "0" -> "关闭"
                                    "2" -> "一点点滑动"
                                    else -> "快速上滑"
                                }
                            },
                        ),
                    ),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_light_open",
                        title = "轻量打开",
                        summary = "通知点击、应用间跳转、设置页跳转都以小窗打开",
                        kind = RowKind.NAV_SWITCH,
                        to = 15,
                    ),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_split_win",
                        title = "平行小窗（二级页保留）",
                        summary = "应用全屏停在二级页（朋友圈/公众号等）时，扇形菜单打开同一应用会另开一扇" +
                            "显示主界面的小窗；在二级页按返回会回到主界面，而不是掉到桌面。默认关闭",
                    ),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_mini_big",
                        key2 = "oupao_longpress_mute",
                        title = "长按迷你窗放大与静音",
                        summary = "长按迷你小窗放大到 135%，长按超小窗静音/恢复声音",
                    ),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(key = "oupao_video_no_pause", title = "上滑手势时不暂停播放"),
                    RowSpec(key = "oupao_dock_keep_alive", title = "小窗贴边时继续播放"),
                    RowSpec(key = "oupao_screen_keep", title = "贴边小窗息屏后继续播放"),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(key = "oupao_cap_clean", title = "隐藏小窗顶部三个点", summary = "点击不再弹菜单"),
                    RowSpec(key = "oupao_corner_off", title = "隐藏小窗左右下角缩放手柄", summary = "角上不再能拖大拖小"),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        title = "窗外点击关闭",
                        summary = "点小窗外部时怎么处理｜点击此行选择（默认双击）",
                        kind = RowKind.CHOICE,
                        choice = ChoiceSpec(
                            labels = listOf("关闭此功能", "单击关闭", "双击关闭"),
                            codes = listOf("0", "1", "2"),
                            // Cfg.flyClose() already collapses to 0 while the page master is off.
                            current = { Cfg.flyClose().toString() },
                            write = {
                                Cfg.saveStr("oupao_fly_close", it)
                                Cfg.push()
                            },
                            display = { code -> listOf("关闭此功能", "单击关闭", "双击关闭")[code.toIntOrNull()?.coerceIn(0, 2) ?: 0] },
                        ),
                    )
                )
            ),
        ),
    ),

    // ---- page 15 · 轻量打开场景（二级页） ------------------------------------------------
    15 to PageSpec(
        title = "轻量打开",
        subtitle = "在下列场景中，需要跳转至其他应用时将使用小窗打开",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_light_notif_port",
                        title = "竖屏打开通知",
                        summary = "竖屏点通知时用小窗打开",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_notif_land",
                        title = "横屏打开通知",
                        summary = "横屏点通知时用小窗打开",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_qs",
                        title = "控制中心",
                        summary = "长按磁贴/快捷开关跳设置时用小窗",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_jump_settings",
                        title = "跳转到设置",
                        summary = "应用内跳到系统设置页时用小窗",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_jump_third",
                        title = "应用间跳转",
                        summary = "普通 A 应用跳 B 应用时用小窗（默认开，保持 3466 行为）",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_share",
                        title = "分享至应用",
                        summary = "分享面板里选目标应用时用小窗",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_text",
                        title = "文本操作",
                        summary = "选中文字后点网页搜索/浏览器搜索时用小窗；翻译、词典仍全屏",
                        def = 1,
                    ),
                    RowSpec(
                        key = "oupao_light_game",
                        title = "游戏内跳转",
                        summary = "游戏内跳活动/公告时用小窗；登录/OAuth 仍全屏",
                        def = 0,
                    ),
                )
            )
        ),
    ),
    // ---- page 8 · Flyme 菜单 (Fly.build) ----------------------------------------------
    // The side picker writes TWO keys at once; two sliders drive the live FlyArea/FlyPrev preview.
    8 to PageSpec(
        title = "Flyme菜单",
        masterKey = "oupao_fly_master",
        cards = listOf(
            CardSpec(
                listOf(
                    RowSpec(
                        title = "斜滑呼出位置",
                        summary = "从屏幕哪个下角斜滑呼出扇形菜单",
                        kind = RowKind.CHOICE,
                        choice = ChoiceSpec(
                            labels = listOf("右侧", "左侧", "两侧"),
                            codes = listOf("R", "L", "B"),
                            current = {
                                val l = Cfg.flyLeft()
                                val r = Cfg.flyRight()
                                when {
                                    l && r -> "B"
                                    l -> "L"
                                    r -> "R"
                                    else -> "N"
                                }
                            },
                            write = { code ->
                                Cfg.save("oupao_fly_left", code != "R")
                                Cfg.save("oupao_fly_right", code != "L")
                            },
                            display = { code ->
                                when (code) {
                                    "L" -> "左侧"
                                    "B" -> "两侧"
                                    "N" -> "关闭"
                                    else -> "右侧"
                                }
                            },
                        ),
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_fly_range",
                        title = "触发范围",
                        summary = "角上开始滑动的区域大小，越大越不容易误触",
                        kind = RowKind.SLIDER,
                        min = 40, max = 80, unit = " dp", sliderDefault = 40,
                        onLive = { FlyArea.setLive(it) },
                        onRelease = { FlyArea.clear() },
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_icon_size",
                        title = "图标大小",
                        summary = "只改图标，不改扇区大小",
                        kind = RowKind.SLIDER,
                        min = 80, max = 120, unit = "%", sliderDefault = 100,
                        onLive = { FlyPrev.setLiveIcon(it) },
                        onRelease = { FlyPrev.clear() },
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_fly_scale",
                        title = "扇区大小",
                        summary = "扇形菜单的整体大小（不影响图标大小）",
                        kind = RowKind.SLIDER,
                        min = 80, max = 120, unit = "%", sliderDefault = 100,
                        onLive = { FlyPrev.setLive(it) },
                        onRelease = { FlyPrev.clear() },
                    )
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_fly_stay_off",
                        title = "未选应用时松手收起",
                        summary = "开：呼出扇形菜单后，没选到应用就松手，菜单直接消失；关：菜单停留等待选择",
                        // 旧 UI 因嵌套 if 的 bug 永远显示为开，但真实配置默认是关（Cfg.flyStayOff 默认 false）。
                        // 这里显示真实值，避免「界面开、实际关」。
                        def = 0,
                    )
                )
            ),
        ),
        footer = "长按标题切换更多应用打开方式「面板 or 小窗」",
    ),

    // ---- page 2 · Aicy 建议 (inline in onCreate, root sB) -------------------------------
    2 to PageSpec(
        title = "Aicy建议",
        masterKey = "oupao_sug_master",
        cards = listOf(
            // 手电筒 + 临时开启屏幕常亮
            CardSpec(
                listOf(
                    RowSpec(key = "oupao_sug_torch", title = "手电筒", summary = "手电筒开启后，建议关闭"),
                    RowSpec(
                        title = "临时开启屏幕常亮",
                        summary = "屏幕半暗时点建议气泡，临时保持屏幕常亮",
                        kind = RowKind.CHOICE,
                        choice = ChoiceSpec(
                            labels = listOf("关闭", "退出应用恢复", "息屏后恢复"),
                            codes = listOf("0", "1", "2"),
                            current = {
                                val appOn = Cfg.getInt("oupao_app_keep", 0) == 1
                                val global = Cfg.getInt("oupao_sug_keep", 0) == 1
                                when {
                                    !appOn -> "0"
                                    global -> "2"
                                    else -> "1"
                                }
                            },
                            write = { code ->
                                Cfg.save("oupao_app_keep", code != "0")
                                Cfg.save("oupao_sug_keep", code == "2")
                            },
                            display = { code ->
                                when (code) {
                                    "0" -> "关闭"
                                    "2" -> "息屏后恢复"
                                    else -> "退出应用恢复"
                                }
                            },
                        ),
                    ),
                )
            ),
            // 打开音乐应用 + 打开新应用
            CardSpec(
                listOf(
                    RowSpec(
                        key = "oupao_sug_music",
                        title = "打开音乐应用",
                        summary = "连接耳机时，建议音乐应用",
                        kind = RowKind.NAV_SWITCH,
                        to = 3,
                    ),
                    RowSpec(key = "oupao_sug_install", title = "打开新应用", summary = "安装新应用后，建议打开"),
                )
            ),
            // 链接相关四项
            CardSpec(
                listOf(
                    RowSpec(key = "oupao_sug_link", title = "打开链接", summary = "复制链接时，建议打开链接"),
                    RowSpec(key = "oupao_sug_word", title = "打开口令", summary = "复制口令时，建议打开口令"),
                    RowSpec(
                        key = "oupao_sug_browser",
                        title = "浏览器打开链接",
                        summary = "复制的链接没有对应 App 时，建议用浏览器打开",
                    ),
                    RowSpec(key = "oupao_sug_web", title = "打开原应用", summary = "浏览网页时，建议在原应用内打开"),
                )
            ),
            CardSpec(
                listOf(
                    RowSpec(
                        title = "气泡位置",
                        summary = "点这里编辑气泡位置（拖动预览气泡）",
                        kind = RowKind.ACTION,
                        onAction = { MainActivity.openPosEditor() },
                    )
                )
            ),
        ),
        footer = "点击执行建议，下滑暂时隐藏。",
    ),
)

/** Pages that need a bespoke composable instead of the row spec (app lists). */
internal val CUSTOM_PAGES: Set<Int> = setOf(3, 6)

// ---------------------------------------------------------------- 选择音乐 App（page 3）

/**
 * 「选择音乐 App」。
 *
 * 列表来源复用 `MainActivity.scanMusic()`（内置候选 + 名字含音乐 + 自定义），
 * 但按「自定义应用 / 内置应用」分成两区（用户要求）。
 *
 * 行为：
 * - 点一行 → 写 `oupao_music_pkg` + `Cfg.push()`；
 * - 长按自定义行 → 从 custom 移除并写入 custom_hid；长按内置行 → 写 hidden；
 * - 「添加自定义应用」→ 打开 Compose 版选择器（与模块界面同一套 Miuix 风格），
 *   选中后立即写入配置、清掉隐藏标记并刷新本页；
 * - 长按「添加自定义应用」→ 恢复内置应用列表。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MusicPickerPage(onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf(Cfg.getStr("oupao_music_pkg", "")) }
    var pickerOpen by remember { mutableStateOf(false) }

    val entries = remember(tick) {
        MainActivity.scanMusic()?.toList().orEmpty().mapNotNull { raw ->
            val cut = raw.indexOf('|')
            if (cut <= 0) null else AppEntry(raw.substring(cut + 1), raw.substring(0, cut))
        }
    }
    val customPkgs = remember(tick) {
        Cfg.getStr("oupao_music_custom", "")
            .split('\n').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val customEntries = entries.filter { it.pkg in customPkgs }
    val builtinEntries = entries.filterNot { it.pkg in customPkgs }

    fun refresh() {
        // 只清扫描缓存，让本页按 tick 重新扫描；不调用旧的 buildMusic() 重建 View 页。
        MainActivity.sMus = null
        tick++
    }

    fun select(pkg: String) {
        Cfg.saveStr("oupao_music_pkg", pkg)
        Cfg.push()
        current = pkg
    }

    fun addCustom(pkg: String) {
        if (pkg.isBlank()) return
        val custom = Cfg.getStr("oupao_music_custom", "")
        if (!MusTxt.has(custom, pkg)) {
            Cfg.saveStr("oupao_music_custom", MusTxt.add(custom, pkg))
        }
        // 之前若被隐藏过，必须两个 hidden 键都清掉，否则刚添加也看不到。
        listOf("oupao_music_hidden", "oupao_music_custom_hid").forEach { key ->
            val hid = Cfg.getStr(key, "")
            if (MusTxt.has(hid, pkg)) {
                val next = MusTxt.remove(hid, pkg)
                if (next.isEmpty()) Cfg.clearKey(key) else Cfg.saveStr(key, next)
            }
        }
        Cfg.saveStr("oupao_music_pkg", pkg)
        Cfg.push()
        current = pkg
        pickerOpen = false
        refresh()
    }

    fun hideMusic(pkg: String) {
        val custom = Cfg.getStr("oupao_music_custom", "")
        if (!MusTxt.has(custom, pkg)) {
            val hidden = Cfg.getStr("oupao_music_hidden", "")
            if (!MusTxt.has(hidden, pkg)) Cfg.saveStr("oupao_music_hidden", MusTxt.add(hidden, pkg))
        } else {
            Cfg.saveStr("oupao_music_custom", MusTxt.remove(custom, pkg))
            val ch = Cfg.getStr("oupao_music_custom_hid", "")
            if (!MusTxt.has(ch, pkg)) Cfg.saveStr("oupao_music_custom_hid", MusTxt.add(ch, pkg))
        }
        Cfg.push()
        refresh()
    }

    fun restoreBuiltin() {
        // 旧 MA$MR.run()：手动添加过又被移除的条目迁到 custom_hid，其余直接清掉 = 恢复内置
        val hidden = Cfg.getStr("oupao_music_hidden", "")
        val custom = Cfg.getStr("oupao_music_custom", "")
        if (hidden.isNotEmpty() && custom.isNotEmpty()) {
            hidden.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { item ->
                if (MusTxt.has(custom, item)) {
                    val old = Cfg.getStr("oupao_music_custom_hid", "")
                    if (!MusTxt.has(old, item)) Cfg.saveStr("oupao_music_custom_hid", old + item + "\n")
                }
            }
        }
        Cfg.clearKey("oupao_music_hidden")
        Cfg.push()
        refresh()
    }

    ContentWidth {
        Column(Modifier.fillMaxSize()) {
            PageHeader(
                "选择音乐 App",
                if (current.isEmpty()) "连接耳机后建议打开的应用" else "当前已选：$current",
                onBack,
            )
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SectionLabel("内置应用")
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        builtinEntries.forEachIndexed { index, app ->
                            MusicEntryRow(app, current == app.pkg, { select(app.pkg) }, { hideMusic(app.pkg) })
                            if (index < builtinEntries.lastIndex) {
                                HorizontalDivider(Modifier.padding(start = 20.dp))
                            }
                        }
                        if (builtinEntries.isEmpty()) {
                            MiuixText(
                                "没有可用的内置音乐应用",
                                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("自定义应用")
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        // 整行可点：打开选择器；长按：恢复内置应用列表（旧 MA$MR）。
                        Box(
                            Modifier.fillMaxWidth().combinedClickable(
                                onClick = { pickerOpen = true },
                                onLongClick = { restoreBuiltin() },
                            )
                        ) {
                            BasicComponent(
                                title = "添加自定义应用",
                                summary = "从已安装应用中选择",
                                insideMargin = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
                                endActions = {
                                    Icon(
                                        MiuixIcons.ChevronForward, null, Modifier.size(18.dp),
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                },
                            )
                        }
                        if (customEntries.isEmpty()) {
                            HorizontalDivider(Modifier.padding(start = 20.dp))
                            MiuixText(
                                "还没有自定义应用",
                                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        } else {
                            customEntries.forEach { app ->
                                HorizontalDivider(Modifier.padding(start = 20.dp))
                                MusicEntryRow(app, current == app.pkg, { select(app.pkg) }, { hideMusic(app.pkg) })
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    MiuixText(
                        "点按选择；长按自定义应用可移除，长按「添加自定义应用」恢复内置列表。",
                        Modifier.padding(horizontal = 4.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }

    MusicAppPickerDialog(
        show = pickerOpen,
        onDismiss = { pickerOpen = false },
        onPick = { addCustom(it) },
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MusicEntryRow(
    app: AppEntry,
    selected: Boolean,
    onSelect: () -> Unit,
    onLongSelect: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = onSelect,
            onLongClick = onLongSelect,
        )
    ) {
        BasicComponent(
            title = app.label,
            summary = app.pkg,
            insideMargin = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            endActions = {
                RadioButton(selected = selected, onClick = onSelect)
            },
        )
    }
}

/** 「添加自定义应用」选择器：与模块界面同一套 Miuix 卡片/弹窗风格。 */
@Composable
private fun MusicAppPickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    LaunchedEffect(show) { if (show) query = "" }
    val apps = remember { loadLauncherApps(ctx) }
    val shown = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps
        else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    ModuleOverlayDialog(
        show = show,
        title = "添加自定义应用",
        summary = "从已安装应用中选择",
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth()) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                ),
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        MiuixText(
                            "搜索应用",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                shown.forEachIndexed { index, app ->
                    BasicComponent(
                        title = app.label,
                        summary = app.pkg,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        endActions = {
                            MiuixText(
                                "添加",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.primary,
                            )
                        },
                        onClick = { onPick(app.pkg) },
                    )
                    if (index < shown.lastIndex) HorizontalDivider(Modifier.padding(start = 12.dp))
                }
                if (shown.isEmpty()) {
                    MiuixText(
                        "没有匹配的应用",
                        Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 应用名单（page 6）

internal data class AppEntry(val label: String, val pkg: String)

/** Launcher-visible apps, label-sorted — same source as the legacy `AppPick.appNames()`. */
internal fun loadLauncherApps(ctx: android.content.Context): List<AppEntry> = runCatching {
    val pm = ctx.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0)
        .mapNotNull { it.activityInfo?.packageName }
        .distinct()
        .map { pkg ->
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            AppEntry(label, pkg)
        }
        .sortedBy { it.label }
}.getOrDefault(emptyList())

/**
 * 「消息气泡 / 屏蔽悬浮通知 / 信息气泡 → 生效应用」名单页（旧 page 6 + AppPick）。
 *
 * 语义与旧版一致：**名单为空 = 全部应用**；勾选即写入 `v1:pkg;pkg`（`Cfg.saveList` 自带前缀与
 * `Cfg.push()`），「恢复全部」= 清空名单。
 */
@Composable
internal fun AppWhitelistPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val from = MainActivity.sugFrom
    val key = when (from) {
        0 -> Cfg.K_BUBBLE_APPS
        1 -> Cfg.K_NOHU_APPS
        else -> Cfg.K_SUG_INFO_APPS
    }
    val title = when (from) {
        0 -> "消息气泡"
        1 -> "屏蔽悬浮通知"
        else -> "信息气泡"
    }
    var selected by remember(key) { mutableStateOf<MutableList<String>>(Cfg.pkgList(key)) }
    var query by remember { mutableStateOf("") }
    val apps = remember { loadLauncherApps(ctx) }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true) }
    }

    fun persist(next: List<String>) {
        selected = next.toMutableList()
        Cfg.saveList(key, next)
    }

    ContentWidth {
        Column(Modifier.fillMaxSize()) {
            PageHeader(
                title,
                if (selected.isEmpty()) "全部应用生效，可按需缩小范围" else "已选择 ${selected.size} 个应用",
                onBack,
                action = if (selected.isNotEmpty()) {
                    { TextButton(text = "恢复全部", onClick = { persist(emptyList()) }) }
                } else null,
            )
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                ),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        MiuixText(
                            "搜索应用",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 28.dp)) {
                item {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        shown.forEachIndexed { index, app ->
                            val checked = selected.contains(app.pkg)
                            BasicComponent(
                                title = app.label,
                                summary = null,
                                insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                endActions = {
                                    Switch(checked = checked, onCheckedChange = { on ->
                                        persist(
                                            if (on) (selected + app.pkg).distinct()
                                            else selected - app.pkg
                                        )
                                    })
                                },
                                onClick = {
                                    persist(
                                        if (checked) selected - app.pkg
                                        else (selected + app.pkg).distinct()
                                    )
                                },
                            )
                            if (index < shown.lastIndex) HorizontalDivider(Modifier.padding(start = 20.dp))
                        }
                        if (shown.isEmpty()) {
                            MiuixText(
                                "没有匹配的应用",
                                Modifier.padding(20.dp),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- renderer

/** One migrated page: heading with back affordance, optional master switch, then grouped cards. */
@Composable
internal fun SettingsPageScreen(spec: PageSpec, onBack: (() -> Unit)?) {
    val scroll = remember(spec.title) { PageScroll.of(spec.title) }
    ContentWidth {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageHeader(spec.title, spec.subtitle, onBack)
            if (spec.masterKey != null) {
                var master by remember(spec.masterKey) { mutableStateOf(Cfg.masterChecked(spec.masterKey)) }
                val mCap = remember(spec.masterKey) { Caps.capOf(spec.masterKey) }
                val mOk = remember(spec.masterKey, mCap) {
                    if (mCap == null) true else Caps.supported(mCap) == true
                }
                SurfaceCard(Modifier.fillMaxWidth()) {
                    BasicComponent(
                        title = spec.masterLabel,
                        summary = if (!mOk) CAP_NO_SUPPORT else spec.masterSummary,
                        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        endActions = {
                            Switch(
                                checked = master && mOk,
                                onCheckedChange = { on ->
                                    if (!mOk) {
                                        toast(CAP_NO_SUPPORT)
                                    } else {
                                        // 2830：同样"确认后才翻"
                                        saveToggle(spec.masterKey, on) { ok -> if (ok) master = on }
                                    }
                                },
                                enabled = mOk,
                            )
                        },
                        onClick = {
                            if (!mOk) {
                                toast(CAP_NO_SUPPORT)
                            } else {
                                val next = !master
                                saveToggle(spec.masterKey, next) { ok -> if (ok) master = next }
                            }
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            spec.cards.forEachIndexed { cardIndex, card ->
                SurfaceCard(Modifier.fillMaxWidth()) {
                    card.rows.forEachIndexed { index, row ->
                        when (row.kind) {
                            RowKind.SWITCH, RowKind.NAV_SWITCH -> SwitchRow(row)
                            RowKind.NAV -> NavRow(row) { openTarget(row) }
                            RowKind.SLIDER -> SliderRow(row)
                            RowKind.CHOICE -> ChoiceRow(row)
                            RowKind.ACTION -> ActionRow(row) { openTarget(row) }
                        }
                        if (index < card.rows.lastIndex) {
                            HorizontalDivider(Modifier.padding(start = 20.dp))
                        }
                    }
                }
                if (cardIndex < spec.cards.lastIndex) Spacer(Modifier.height(12.dp))
            }
            if (spec.footer != null) {
                MiuixText(
                    spec.footer,
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun openTarget(row: RowSpec) {
    row.onAction?.let { it(); return }
    when {
        row.sugFrom >= 0 -> MainActivity.navToSug(row.sugFrom)
        row.to >= 0 -> MainActivity.navTo(row.to)
    }
}

private val ROOT_NEEDED = emptySet<String>()
/*
 * 2830：目前**没有任何开关需要 root**，别再往这里加：
 *  · 通知点击小窗 / 屏蔽侧边栏滑动：改成 system_server 写 Settings.Global、目标进程自己读；
 *  · 隐藏桌面图标：用 setComponentEnabledSetting 关自己的 activity-alias，本来就不要 root。
 * 机制保留 —— 以后真有需要 root 的键加进来即可（会先查 root，没有就拒绝并保持原状态）。
 */

/**
 * 2830：这些键「配置文件写进去」不等于「功能生效」，必须回读**功能真正看的那一路**。
 *
 * 实测踩坑：`oupao_notif_fly` 的判定方在 SystemUI，原来读的是系统属性（只有 root 能写）；
 * 用户把模块的 root 关掉后，开关能拨、配置文件也改了，但属性没变 → 功能照旧。
 * 现在判定改成读 Settings.Global，这里就回读 Settings.Global。
 */
private val EFFECT_READ: Map<String, () -> Boolean?> = mapOf(
    "oupao_light_open" to ({ Cfg.getBoolRaw("oupao_light_open", false) }),
    "oupao_sidebar_noswipe" to ({ Cfg.getBoolRaw("oupao_sidebar_noswipe", false) }),
)

/** 2829：能力位判定为「不支持」时统一用这句话（列表摘要 + 点按 toast） */
internal val CAP_NO_SUPPORT: String = Caps.NO_SUPPORT

private fun toast(msg: String) {
    runCatching {
        val ctx = Ui.appCtx ?: return@runCatching
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 2830：开关落盘 + **先确认系统采纳，再动 UI**。
 *
 * 用户要求（原话）：「值写不进去，开关就不应该能关闭，还要保持开的状态同时提示写入失败」。
 * 所以顺序是：
 *  1) 写配置并回读**配置文件**；写不进去 → 立刻失败，UI 一动不动。
 *  2) 关键键（见 [EFFECT_READ]）**先不动 UI**，等 ~0.9s 从"功能真正读的那条通道"回读确认：
 *     确认成功 → 回调 true → 调用方这才翻开关 + 提示「立即生效」；
 *     没采纳 → 把配置也退回原值，回调 false → 开关保持原样，提示「写入失败…已保持原状态」。
 *  3) 其余键：回读文件后就翻（读方和写方在同一条文件通道，回读已足够）。
 *
 * @param onResult 异步确认结果；只在走过 2) 的键上被调用（其余键同步回调 true）
 * @return false = 同步阶段就失败（调用方不要动 UI）
 */
private fun saveToggle(key: String, value: Boolean, onResult: ((Boolean) -> Unit)? = null): Boolean {
    if (key in ROOT_NEEDED) {
        val ok = runCatching { EnvCheck.root().first }.getOrDefault(false)
        if (!ok) {
            toast("写入失败：该选项需要 root 权限")
            return false
        }
    }
    Cfg.save(key, value)
    val want = if (value) "1" else "0"
    val got = runCatching { Cfg.getStrFresh(key, "") }.getOrDefault("").trim()
    if (got != want) {
        toast("写入失败：配置未落盘（$key=$got）")
        return false
    }
    val readEffect = EFFECT_READ[key]
    if (readEffect != null) {
        val rootOk = runCatching { EnvCheck.root().first }.getOrDefault(false)
        runCatching {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val real = runCatching { readEffect() }.getOrNull()
                if (real == value) {
                    onResult?.invoke(true)
                    toast("立即生效")
                } else {
                    // 系统没采纳 → 配置退回原值，开关保持原状态（不能"看着关了其实还开着"）
                    runCatching { Cfg.save(key, !value) }
                    onResult?.invoke(false)
                    toast(
                        if (!rootOk) "写入失败：该选项需要 root 权限，已保持原状态"
                        else "写入失败：系统未采纳（仍是「" + (if (real == true) "开" else "关") +
                            "」），已保持原状态"
                    )
                }
            }, 900)
        }
        return true
    }
    onResult?.invoke(true)
    toast("立即生效")
    return true
}

/** The legacy slider release path (`SlCb.onStopTrackingTouch`): saveStr → CFG intent → push. */
private fun saveSlider(key: String, value: Int) {
    runCatching {
        val v = value.toString()
        Cfg.saveStr(key, v)
        Ev.send(Ev.it("com.oupao.wiz.CFG").putExtra(key, v))
        Cfg.push()
    }
}

@Composable
private fun SwitchRow(spec: RowSpec) {
    val key = spec.key ?: return
    val key2 = spec.key2
    // 2829：系统不支持这个钩子时，整行置灰、开关点不动（用户要求：不能"不管支不支持都能开"）
    val cap = remember(key) { Caps.capOf(key) }
    val supported = remember(key, cap) { if (cap == null) true else Caps.supported(cap) == true }
    var checked by remember(key, key2) {
        mutableStateOf(
            Cfg.getInt(key, spec.def) == 1 &&
                (key2 == null || Cfg.getInt(key2, spec.def) == 1)
        )
    }
    // 2830：确认中（等系统侧回读）时先别让用户再点，避免连点打架
    var busy by remember(key) { mutableStateOf(false) }
    fun persist(next: Boolean) {
        if (!supported) {
            toast(CAP_NO_SUPPORT)
            return
        }
        if (busy) return
        busy = true
        // ★ 2830：开关**不在点下去的那一刻翻**。先写盘，等系统侧确认采纳了才翻；
        //   没采纳就不动（保持原状态），并把配置退回原值。
        val accepted = saveToggle(key, next) { ok ->
            busy = false
            if (ok) {
                checked = next
                // 第二个键静默写入（避免连弹两次 toast），第一次 save 已经 push 过一次。
                if (key2 != null) Cfg.save(key2, next)
                spec.afterSave?.invoke(next)
            }
        }
        if (!accepted) busy = false
    }
    val toggle = { persist(!checked) }
    BasicComponent(
        title = spec.title,
        summary = if (!supported) {
            // 3313：C17 系统默认就是「上滑不暂停」，给一句明确的说明而不是笼统的"不支持"
            if (key == Cfg.K_VIDEO_NO_PAUSE && Cfg.isC17()) {
                "ColorOS 17 系统默认上滑不暂停，无需开启"
            } else {
                CAP_NO_SUPPORT
            }
        } else spec.summary,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        endActions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (spec.kind == RowKind.NAV_SWITCH) {
                    Icon(
                        MiuixIcons.ChevronForward, "打开应用名单", Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Switch(
                    checked = checked && supported,
                    onCheckedChange = { persist(it) },
                    enabled = supported && !busy,
                )
            }
        },
        // Legacy: a plain row toggles; a chevron row navigates instead (the switch still toggles).
        onClick = { if (spec.kind == RowKind.NAV_SWITCH) openTarget(spec) else toggle() },
    )
}

@Composable
private fun NavRow(spec: RowSpec, onClick: () -> Unit) {
    BasicComponent(
        title = spec.title,
        summary = spec.summary,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        endActions = {
            Icon(
                MiuixIcons.ChevronForward, null, Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ActionRow(spec: RowSpec, onClick: () -> Unit) {
    BasicComponent(
        title = spec.title,
        summary = spec.summary,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        endActions = {
            Icon(MiuixIcons.Basic.ArrowRight, null, Modifier.size(18.dp), tint = MiuixTheme.colorScheme.primary)
        },
        onClick = onClick,
    )
}

/** Integer slider row: title + live value + Miuix slider with the legacy min/max/step. */
@Composable
private fun SliderRow(spec: RowSpec) {
    val key = spec.key ?: return
    // ⚠️ Cfg.getInt 只读一个字节（'0'→0，其它→1），它是布尔读取器。
    // 数值必须走 getNumber（= intOf(rawStr)），否则 100 会显示成 1。
    val persistOn = spec.persistKey?.let { Cfg.getInt(it, 0) == 1 } ?: false
    val initial = if (persistOn) spec.max else
        Cfg.getNumber(key, spec.sliderDefault).coerceIn(spec.min, spec.max)
    var value by remember(key) { mutableStateOf(initial.toFloat()) }
    val isEnd = spec.endLabel != null && value.toInt() >= spec.max
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MiuixText(
                    spec.title,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                )
                if (spec.summary != null) {
                    Spacer(Modifier.height(4.dp))
                    MiuixText(
                        spec.summary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                MiuixText(
                    if (isEnd) spec.endLabel!! else "${value.toInt()}${spec.unit}",
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.primary,
                    textAlign = TextAlign.End
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Slider(
            value = value,
            onValueChange = {
                value = it
                spec.onLive?.invoke(it.toInt())
            },
            valueRange = spec.min.toFloat()..spec.max.toFloat(),
            steps = ((spec.max - spec.min) / spec.step - 1).coerceAtLeast(0),
            onValueChangeFinished = {
                val v = value.toInt()
                val persistKey = spec.persistKey
                if (persistKey != null) {
                    val persist = v >= spec.max
                    Cfg.save(persistKey, persist)
                    // 常驻时把秒数收回到最大值前一档，避免下次取消常驻直接又落在端点。
                    saveSlider(key, if (persist) (spec.max - 1) else v)
                } else {
                    saveSlider(key, v)
                }
                spec.onRelease?.invoke()
            },
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiuixText(
                "${spec.min}${spec.unit}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            MiuixText(
                spec.endLabel ?: "${spec.max}${spec.unit}",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** Legacy popup picker rendered as a row with the current value + an OverlayDialog of options. */
@Composable
private fun ChoiceRow(spec: RowSpec) {
    val choice = spec.choice ?: return
    var open by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf(choice.current()) }
    BasicComponent(
        title = spec.title,
        summary = spec.summary,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        endActions = {
            // 与「日志级别」卡片一致：只显示当前值，不画右侧箭头。
            MiuixText(
                choice.display(code),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        },
        onClick = { open = true },
    )
    ModuleOverlayDialog(
        show = open,
        title = spec.title,
        summary = spec.summary,
        onDismissRequest = { open = false },
    ) {
        Column(Modifier.fillMaxWidth()) {
            choice.labels.forEachIndexed { index, label ->
                BasicComponent(
                    title = label,
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    endActions = {
                        RadioButton(selected = code == choice.codes[index], onClick = {
                            code = choice.codes[index]
                            choice.write(code)
                            open = false
                        })
                    },
                    onClick = {
                        code = choice.codes[index]
                        choice.write(code)
                        open = false
                    },
                )
            }
        }
    }
}

