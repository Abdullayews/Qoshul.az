package az.qoshul.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import kotlin.math.roundToInt

/**
 * OVERLAY PICK — D-089 (M6, 2026-11-07). İstifadəçi direktivi:
 * "seçim üçün olan yerlər (kategoriya mini) üçün dropdown menu və digər
 * menyuları sürüşdürmək yerinə onların üzərinə çıxıb seçilə bilən
 * kategoriyalar formatına keçirt".
 *
 * D-110 (v0.41.0, direktiv 2026-11-09: „şəkildəki stildə selector box
 * əlavə et“): trigger SELECTOR BOX dilinə keçir — yuxarıda sahə etiketi,
 * altda cari dəyər qutusu (ikon + dəyər + chevron); panel VAHİD
 * SÜTUNLU SİYAHIDIR — hər sətir [konteynerdə ikon] + [başlıq + izah] +
 * [seçilib check], seçili sətir secondary fon + primary mətn ilə
 * vurğulanır (referans şəklin „Share with“ selektoru dili).
 *
 * Sistem (3 hissə):
 *  1) SelectBoxTrigger — etiketli seçim qutusu (cari dəyər + çevrilən
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

/** Panel seçimi: etiket + (istəyə bağlı) ikon + (istəyə bağlı) izah sətri. */
data class PickOption(
    val label: String,
    val icon: ImageVector? = null,
    val description: String? = null,
)

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

/** Host yaratma — windowH LocalConfiguration-dan (dpi daxil) px-lə ölçülür.
 *  U4-3 (D-097): remember cfg açarına bağlandı — dönüş/fold konfiq dəyişəndə
 *  windowH yenidən hesablanır (əvvəl bir dəfə yadda saxlanılırdı; pickAuto
 *  köhnə ekran hündürlüyü ilə "aşağıda yer var" qərarı verib paneli ekrandan
 *  çıxara bilərdi — manifest orientation kilidi YOXDUR, dönüş realdır). */
@Composable
fun rememberPickHostState(): PickHostState {
    val cfg = LocalConfiguration.current
    val d = LocalDensity.current
    return remember(cfg) {
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

/**
 * Panelin təxmini hündürlüyü (auto istiqamət qərarı üçün) — D-110
 * vahid-sütun siyahısı: hər sətir ~56dp (36dp ikon konteyneri + padding)
 * + 4dp aralıq; izahlı sətirlər bir az hündürdür — max hədd kəsir.
 */
fun estimatePanelHeight(optionCount: Int, max: Dp = 360.dp): Dp {
    val n = optionCount.coerceAtLeast(1)
    val h = 16.dp + 56.dp * n + 4.dp * (n - 1)
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
// Trigger — D-110 selector box (etiket + dəyər qutusu)
// ═══════════════════════════════════════════════════════════════════

/**
 * SelectBoxTrigger — D-110 (direktiv: „şəkildəki stildə selector box
 * əlavə et, rəngi proqrama uyğun et“): yuxarıda sahə etiketi (T.label),
 * altda cari dəyəri göstərən QUTU — ikon + dəyər + çevrilən chevron.
 * Bağlı: card fon + border; açıq: secondary fon + primary kontur,
 * chevron 180° çevrilir (150ms), 1.02 pop. A11y: etiket + dəyər birgə
 * oxunur (Kateqoriya: Hamısı mənası — trigger özü seç/bağla deyir).
 */
@Composable
fun SelectBoxTrigger(
    fieldLabel: String,
    value: String,
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
        targetValue = if (open) 1.02f else 1f,
        animationSpec = tween(Motion.D1, easing = Motion.ease),
        label = "pickPop",
    )
    Column(modifier) {
        Text(fieldLabel, style = T.label)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .scaleTap(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .graphicsLayer { scaleX = pop; scaleY = pop }
                .background(if (open) Q.secondary else Q.card, RoundedCornerShape(Q.rMd.dp))
                .border(
                    if (open) 1.2.dp else 1.dp,
                    if (open) Q.primary else Q.border,
                    RoundedCornerShape(Q.rMd.dp),
                )
                .padding(horizontal = 13.dp, vertical = 12.dp),
        ) {
            if (icon != null) {
                Icon2(
                    icon, null,
                    tint = if (open) Q.primary else Q.mutedFg,
                    size = 16.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = T.input.copy(
                    color = if (open) Q.primary else Q.foreground,
                    fontWeight = if (open) FontWeight.W700 else FontWeight.W600,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            Icon2(
                Icons.Filled.ExpandMore, if (open) "bağla" else "seç",
                tint = if (open) Q.primary else Q.mutedFg,
                size = 18.dp,
                rotation = chev,
            )
        }
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
    maxHeight: Dp = 360.dp,
    gap: Dp = 8.dp,
    placement: PickPlacement = PickPlacement.ANCHOR_AUTO,
) {
    val req = host.openReq ?: return

    BackHandler(onBack = { host.openReq = null })
    PickerScrim(onDismiss = { host.openReq = null })

    val panelH = estimatePanelHeight(req.options.size, maxHeight)
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
        maxHeight = maxHeight,
    )
}

/** Panelin host-da yerləşdirilmə qaydası (bax yuxarıdakı doc). */
enum class PickPlacement { ANCHOR_AUTO, CENTERED }

/** Yarı-şffaf fon (0.40 — D-089 tuninq: vizual QA "scrim zəif" tapıntısı)
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
 * PickerPanel — üzərindən çıxan seçim siyahısı. D-110 (direktiv: „şəkildəki
 * stildə selector box əlavə et“): VAHİD SÜTUN — hər sətir referans şəklin
 * dili ilə: [yumru kvadratda ikon] + [başlıq + izah sətri] + [seçilib
 * check]. Seçili sətir: secondary fon + primary kontur/mətn; ikon
 * konteyneri seçilidə primary dolu, qalanında muted. İkonsuz variantlarda
 * (məs. şəhər siyahısı) konteyner yoxdur — başlıq birbaşa. Daxili scroll
 * (D-062: heightIn SRİFDLİ, scroll DAXİLDİ), giriş hərəkəti fade+slide+
 * scale (220ms). Panel: Q.card fon, Q.rXl radius, dərin kölgə.
 */
@Composable
fun PickerPanel(
    options: List<PickOption>,
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 360.dp,
) {
    val animationsOn = rememberAnimationsOn()
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animationsOn) p.animateTo(1f, tween(220, easing = Motion.ease))
        else p.snapTo(1f)
    }
    Box(
        modifier
            .graphicsLayer {
                alpha = p.value
                translationY = -10.dp.toPx() * (1f - p.value)
                val s = 0.98f + 0.02f * p.value
                scaleX = s; scaleY = s
            }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(Q.rXl.dp),
                ambientColor = Q.overlayDark.copy(alpha = 0.12f),
                spotColor = Q.overlayDark.copy(alpha = 0.18f),
            )
            .background(Q.card, RoundedCornerShape(Q.rXl.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rXl.dp))
    ) {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            options.forEachIndexed { idx, opt ->
                val sel = opt.label == selected
                val interaction = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                        ) { onPick(opt.label) }
                        .background(
                            if (sel) Q.secondary else Color.Transparent,
                            RoundedCornerShape(16.dp),
                        )
                        .then(
                            if (sel) Modifier.border(1.2.dp, Q.primary, RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (opt.icon != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (sel) Q.primary else Q.muted,
                                    RoundedCornerShape(13.dp),
                                ),
                        ) {
                            Icon2(
                                opt.icon, null,
                                tint = if (sel) Q.primaryFg else Q.mutedFg,
                                size = 19.dp,
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            opt.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = T.metaBold.copy(
                                fontWeight = if (sel) FontWeight.W700 else FontWeight.W600,
                                color = if (sel) Q.primary else Q.foreground,
                            ),
                        )
                        if (opt.description != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                opt.description,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = T.chipXs.copy(color = Q.mutedFg),
                            )
                        }
                    }
                    if (sel) {
                        Spacer(Modifier.width(8.dp))
                        Icon2(
                            Icons.Filled.Check, "seçilib", tint = Q.primary,
                            size = 18.dp,
                        )
                    }
                }
                if (idx < options.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}
