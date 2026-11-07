package az.qoshul.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.rememberAnimationsOn
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * OVERLAY PICK — D-089 (M6, 2026-11-07). İstifadəçi direktivi:
 * "seçim üçün olan yerlər (kategoriya mini) üçün dropdown menu və digər
 * menyuları sürüşdürmək yerinə onların üzərinə çıxıb seçilə bilən
 * kategoriyalar formatına keçirt".
 *
 * Sistem (3 hissə):
 *  1) MiniPickTrigger — kiçik "kateqoriya mini" həbi (cari dəyər + çevrilən
 *     chevron; açıqda secondary fon + primary kontur);
 *  2) PickHostState — bir "host" (ekran / sheet) üçün pick vəziyyəti:
 *     açıq sorğu (DropReq), anchor koordinatları (window məkanında),
 *     host konteynerinin yuxarısı;
 *  3) PickHostOverlay — host-un KÖK Box-ına qoyulur: scrim (0.32 alfa,
 *     toxunanda bağlanır, geri düyməsi bağlayır) + panel.
 *
 * Panel yerləşdirməsi (istifadəçi: "yerləşdirmə və animasiya səhvdir"):
 *  - anchor window koordinatlarında ölçülür (onGloballyPositioned);
 *  - aşağıda yer varsa panel anchorin ALTINA (gap 8dp), yoxdursa ÜSTÜNƏ
 *    açılır (pickAuto) — forma sheet-lərinin aşağıdakı sahələri üçün;
 *  - hərəkət: GD3/qfw popup dili — fade + 10dp yuxarıdan sürüşmə +
 *    0.98→1 miqyas, 220ms easeOutCubic (Ghost-Downloader-3 qfw_patch
 *    PopUpAniStackedWidget 250ms OutQuad-a uyğunlaşdırılmış).
 *
 * D-062 dərsi saxlanılır: panel daxilində hündürlük SRİFDLİDİR
 * (heightIn max), scroll DAXİLDƏDİR — sonsuz constraint çökməsi mümkün deyil.
 */

// ═══════════════════════════════════════════════════════════════════
// Dəyər tipləri
// ═══════════════════════════════════════════════════════════════════

/** Panel seçimi: etiket + (istəyə bağlı) ikon. */
data class PickOption(val label: String, val icon: ImageVector? = null)

/** Açıq sorğu — hansı sahə, variantları, cari dəyəri və seçim callback-i. */
class DropReq(
    val key: String,
    val options: List<PickOption>,
    val selected: String,
    val onPick: (String) -> Unit,
)

/** Anchorın window-məkanında mövqeyi (dp yox — px). */
internal data class AnchorPos(val top: Float, val bottom: Float)

/**
 * Bir host-un (ekranın kök Box-u və ya sheet-in kök Box-u) pick vəziyyəti.
 * Host Modifier.pickHost(state) ilə qeydə alınır; hər trigger
 * Modifier.pickAnchor(state, key) ilə öz mövqeyini yazır.
 */
class PickHostState {
    var openReq by mutableStateOf<DropReq?>(null)
        internal set

    internal var containerTop by mutableFloatStateOf(0f)
    internal var windowH = 0f
    internal val anchors = mutableStateMapOf<String, AnchorPos>()

    /** Trigger toxunuşu: eyni sahədirsə bağla, yoxsa aç (tək açıq sorğu). */
    fun toggle(key: String, options: List<PickOption>, selected: String, onPick: (String) -> Unit) {
        openReq = if (openReq?.key == key) null else DropReq(key, options, selected, onPick)
    }
}

/** Host yaratma — windowH LocalConfiguration-dan (dpi daxil) px-lə ölçülür. */
@Composable
fun rememberPickHostState(): PickHostState {
    val cfg = LocalConfiguration.current
    val d = LocalDensity.current
    return remember {
        PickHostState().also {
            it.windowH = with(d) { cfg.screenHeightDp.dp.toPx() }
        }
    }
}

/** Host konteyneri (kök Box) öz window-y Mövqeyini qeyd edir. */
fun Modifier.pickHost(host: PickHostState): Modifier =
    onGloballyPositioned { c ->
        host.containerTop = c.positionInWindow().y
    }

/** Trigger anchori: window-məkanında [key] sahəsinin üst/alt y-x. */
fun Modifier.pickAnchor(host: PickHostState, key: String): Modifier =
    onGloballyPositioned { c ->
        val p = c.positionInWindow()
        host.anchors[key] = AnchorPos(p.y, p.y + c.size.height)
    }

/** Panelin təxmini hündürlüyü (auto istiqamət qərarı üçün). */
fun estimatePanelHeight(optionCount: Int, columns: Int = 3, max: Dp = 320.dp): Dp {
    val rows = ceil(optionCount / columns.toFloat()).coerceAtLeast(1f).toInt()
    val h = 20.dp + 40.dp * rows + 8.dp * (rows - 1)
    return h.coerceAtMost(max)
}

/**
 * Panelin avtomatik yerləşdirməsi (container koordinatlarında IntOffset):
 * aşağıda panelH + 48px slack yer varsa ALTA (anchor.bottom + gap),
 * yoxdursa ÜSTƏ (anchor.top − gap − panelH). “Yerləşdirmə səhvdir”
 * şikayətinin cavabı: panel heç vaxt ekranın altından çıxmır.
 */
fun Modifier.pickAuto(host: PickHostState, key: String, panelHeight: Dp, gap: Dp = 8.dp): Modifier =
    offset {
        val a = host.anchors[key] ?: return@offset IntOffset.Zero
        val gapPx = gap.toPx()
        val panelPx = panelHeight.toPx()
        val below = host.windowH - a.bottom >= panelPx + 48f
        val y = if (below) {
            a.bottom - host.containerTop + gapPx
        } else {
            a.top - host.containerTop - gapPx - panelPx
        }
        IntOffset(0, y.roundToInt())
    }

// ═══════════════════════════════════════════════════════════════════
// Trigger (mini kateqoriya həbi)
// ═══════════════════════════════════════════════════════════════════

/**
 * MiniPickTrigger — "kateqoriya mini": cari dəyər + chevron.
 * Bağlı: card fon + border; açıq: secondary fon + primary kontur,
 * chevron 180° çevrilir (150ms), 1.03 pop.
 */
@Composable
fun MiniPickTrigger(
    label: String,
    icon: ImageVector?,
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val chev by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(Motion.D1, easing = Motion.ease),
        label = "pickChev",
    )
    val pop by animateFloatAsState(
        targetValue = if (open) 1.03f else 1f,
        animationSpec = tween(Motion.D1, easing = Motion.ease),
        label = "pickPop",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = pop; scaleY = pop }
            .height(36.dp)
            .background(if (open) Q.secondary else Q.card, RoundedCornerShape(Q.rPill))
            .border(
                if (open) 1.2.dp else 1.dp,
                if (open) Q.primary else Q.border,
                RoundedCornerShape(Q.rPill),
            )
            .padding(horizontal = 13.dp),
    ) {
        if (icon != null) {
            Icon2(
                icon, null,
                tint = if (open) Q.primary else Q.mutedFg,
                size = 14.5.dp,
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            style = T.chip.copy(
                color = if (open) Q.primary else Q.foreground,
                fontWeight = if (open) FontWeight.W700 else FontWeight.W600,
            ),
        )
        Spacer(Modifier.width(3.dp))
        Icon2(
            Icons.Filled.ExpandMore, if (open) "bağla" else "seç",
            tint = if (open) Q.primary else Q.mutedFg,
            size = 17.dp,
            rotation = chev,
        )
    }
}

/** Oxşar funksiyaları təkrarlamamaq üçün daxili ikon sətri (rotate ilə). */
@Composable
private fun Icon2(
    icon: ImageVector,
    desc: String?,
    tint: Color,
    size: Dp,
    rotation: Float = 0f,
) {
    androidx.compose.material3.Icon(
        icon, desc, tint = tint,
        modifier = Modifier
            .size(size)
            .rotate(rotation),
    )
}

// ═══════════════════════════════════════════════════════════════════
// Overlay qatı (scrim + panel) — host kök Box-ına qoyulur
// ═══════════════════════════════════════════════════════════════════

/**
 * PickHostOverlay — host-un kök Box-unda çağırılır (BoxScope; son uşaq
 * kimi, bütün məzmunun ÜZƏRİNƏ çəkilir). [panelModifier] panelin
 * yerləşdirmə modifieridir (align/fill/padding) — pickAuto daxildə
 * əlavə olunur.
 *
 * [placement]: ANCHOR_AUTO — ekran tipli hostlarda (aktivlik pəncərəsi
 * koordinatları) anchorin altına/üstünə; CENTERED — DİALOQ (sheet)
 * hostlarında: dialoq öz koordinat məkanı verir (window≠root),
 * anchor riyaziyyatı orada etibarsızdır (M6 debug: panel ekranın
 * 1750px aşağısına düşürdü) — panel scrim üzərində, mərkəzdə —
 * qfw "content dialog" üslubu, koordinat-aqnostik.
 */
@Composable
fun BoxScope.PickHostOverlay(
    host: PickHostState,
    panelModifier: Modifier,
    columns: Int = 3,
    maxHeight: Dp = 320.dp,
    gap: Dp = 8.dp,
    placement: PickPlacement = PickPlacement.ANCHOR_AUTO,
) {
    val req = host.openReq ?: return

    BackHandler(onBack = { host.openReq = null })
    PickerScrim(onDismiss = { host.openReq = null })

    val panelH = estimatePanelHeight(req.options.size, columns, maxHeight)
    val placed = when (placement) {
        PickPlacement.ANCHOR_AUTO ->
            panelModifier.pickAuto(host, req.key, panelH, gap)
        PickPlacement.CENTERED ->
            panelModifier.then(
                Modifier.align(Alignment.Center).padding(vertical = 24.dp)
            )
    }
    PickerPanel(
        options = req.options,
        selected = req.selected,
        onPick = { label ->
            req.onPick(label)
            host.openReq = null
        },
        modifier = placed,
        columns = columns,
        maxHeight = maxHeight,
    )
}

/** Panelin host-da yerləşdirilmə qaydası (bax yuxarıdakı doc). */
enum class PickPlacement { ANCHOR_AUTO, CENTERED }

/** Yarı-şffaf fon (0.40 — D-089 tuninq: VLM QA "scrim zəif" tapıntısı)
 * — toxunanda/geridə bağlanır. Giriş: 180ms fade. */
@Composable
private fun PickerScrim(onDismiss: () -> Unit) {
    val animationsOn = rememberAnimationsOn()
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animationsOn) p.animateTo(1f, tween(180, easing = Motion.ease))
        else p.snapTo(1f)
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.40f * p.value }
            .background(Q.foreground)
            .clickable(interactionSource = interaction, indication = null, onClick = onDismiss)
    )
}

/**
 * PickerPanel — üzərindən çıxan seçim paneli: bərabər enli tor
 * ([columns] sütun), daxili scroll (D-062: heightIn SRİFDLİ, scroll
 * DAXİLDƏ), giriş hərəkəti fade+slide+scale (220ms).
 * Seçili variant: secondary fon + primary kontur + check ikonu.
 */
@Composable
fun PickerPanel(
    options: List<PickOption>,
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    maxHeight: Dp = 320.dp,
) {
    val animationsOn = rememberAnimationsOn()
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animationsOn) p.animateTo(1f, tween(220, easing = Motion.ease))
        else p.snapTo(1f)
    }
    // QEYD (D-089, M6): "açılışda seçiliyə snap-scroll" SINAQDA
    // (Robolectric waitForIdle) sonsuz dövrə salırdı — ləğv edilib;
    // uzun siyahıda seçili element istifadəçi tərəfindən scroll olunur.
    Box(
        modifier
            .graphicsLayer {
                alpha = p.value
                translationY = -10.dp.toPx() * (1f - p.value)
                val s = 0.98f + 0.02f * p.value
                scaleX = s; scaleY = s
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(Q.rLg.dp),
                ambientColor = Q.overlayDark.copy(alpha = 0.10f),
                spotColor = Q.overlayDark.copy(alpha = 0.16f),
            )
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
    ) {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.chunked(columns).forEach { rowOpts ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowOpts.forEach { opt ->
                        val sel = opt.label == selected
                        val interaction = remember { MutableInteractionSource() }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                ) { onPick(opt.label) }
                                .background(
                                    if (sel) Q.secondary else Q.card,
                                    RoundedCornerShape(12.dp),
                                )
                                .border(
                                    if (sel) 1.2.dp else 1.dp,
                                    if (sel) Q.primary else Q.border,
                                    RoundedCornerShape(12.dp),
                                ),
                        ) {
                            if (sel) {
                                Icon2(
                                    Icons.Filled.Check, "seçilib", tint = Q.primary,
                                    size = 13.5.dp,
                                )
                                Spacer(Modifier.width(5.dp))
                            } else if (opt.icon != null) {
                                Icon2(
                                    opt.icon, null, tint = Q.mutedFg,
                                    size = 15.dp,
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                opt.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.W700 else FontWeight.W600,
                                    color = if (sel) Q.primary else Q.foreground,
                                ),
                            )
                        }
                    }
                    // sətir yarım qalıbsa — tor düz qalsın
                    repeat(columns - rowOpts.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
