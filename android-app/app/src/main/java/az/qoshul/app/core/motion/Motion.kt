package az.qoshul.app.core.motion

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Hərəkət sistemi — Flutter `core/widgets/motion.dart` portu (D-025).
 *
 * Tokenlər: d1 150ms mikro · d2 250ms standart · d3 400ms vurğulu ·
 * d4 550ms səhnə · stagger 45ms.
 * Əyrilər: ease = easeOutCubic, enter = easeOutBack (overshoot),
 * spring = elasticOut (bounce), decel = decelerate.
 * `disableAnimations` hörmət olunur (Android: ANIMATOR_DURATION_SCALE == 0).
 */
object Motion {
    const val D1 = 150
    const val D2 = 250
    const val D3 = 400
    const val D4 = 550
    const val STAGGER_STEP = 45

    val ease = EaseOutCubic
    val enter = EaseOutBack
    val decel = CubicBezierEasing(0f, 0f, 0.58f, 1f) // Curves.decelerate
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    // D-102 (v0.35.0): spring hərəkət dili — SPATIAL keçidlər (tab slaydı,
    // ikon dip-i) tween əvəzinə spring fizikasına keçir (hesabat §7.4
    // "Motion keçidlərinin tween-dən spring fizikasına keçirilməsinin
    // başlanğıcı" + cədvəl 5 "M3E / spring hərəkət dili: yüksək prioritet").
    // Qayda: spatial (mövqe/ölçü) = spring, ALPHA (fade) = tween qalır —
    // spring alfa overshoot edib vizual artefakt verir.
    // spatialStiffness — keçid sürəti (MediumLow ~ eyni müddət hissi D2 ilə);
    // spatialDamping — bounce YOX (səhifə keçidində overshoot gurultulu
    // görünür; bounce yalnız mikro-elementlərdə — navIconDip).
    val spatialStiffness = Spring.StiffnessMediumLow
    val spatialDamping = Spring.DampingRatioNoBouncy
    val dipStiffness = Spring.StiffnessMedium
    val dipDamping = Spring.DampingRatioMediumBouncy
}

/** Sistem animasiyaları bağlıdırmı (Flutter disableAnimationsOf ekvivalenti). */
@Composable
fun rememberAnimationsOn(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )
        scale != 0f
    }
}

/** StaggerIn — siyahı item-ləri üçün fade + 14dp sürüşmə (max 8 addım). */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    val animationsOn = rememberAnimationsOn()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (animationsOn) {
            kotlinx.coroutines.delay((Motion.STAGGER_STEP * index.coerceIn(0, 8)).toLong())
            progress.animateTo(1f, tween(Motion.D3, easing = Motion.decel))
        } else {
            progress.snapTo(1f)
        }
    }
    Box(
        modifier = Modifier
            .alpha(progress.value)
            // D-054: dp əsaslı sürüşmə (əvvəl xam 14f px — DPI-dan asılı idi)
            .graphicsLayer { translationY = 14.dp.toPx() * (1f - progress.value) }
    ) { content() }
}

/** PopIn — bounce miqyas (0.6→1) + fade; badge / bayram / MATCH üçün. */
@Composable
fun PopIn(delayMs: Int = 0, key: Any? = Unit, content: @Composable () -> Unit) {
    val animationsOn = rememberAnimationsOn()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (animationsOn) {
            kotlinx.coroutines.delay(delayMs.toLong())
            progress.animateTo(1f, tween(Motion.D3, easing = Motion.enter))
        } else {
            progress.snapTo(1f)
        }
    }
    val scale = 0.6f + 0.4f * progress.value
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = progress.value.coerceIn(0f, 1f)
        }
    ) { content() }
}

/** ScaleTap — basılanda 0.97 miqyas; interactionSource ilə birgə istifadə. */
fun Modifier.scaleTap(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = composed {
    val animationsOn = rememberAnimationsOn()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && animationsOn) 0.97f else 1f,
        animationSpec = tween(90),
        label = "scaleTap",
    )
    scale(scale)
}

/**
 * D-121 (v0.42.0, direktiv 2026-11-10: „Haptik və pressScale əlavə
 * dəyişikliklərdir və indication = null edilməlidir“): VAHİD BASIQLAMA
 * MODİFİKATORU — pressScale (0.97) + klik anında yüngül haptik
 * (CONTEXT_CLICK „tick“) + ripple YOX (indication = null). Bütün custom
 * basıla bilən elementlərin (nav, FAB, çiplər, selector box-lar, xəritə
 * düymələri, kartlar) ortaq dili — ripple-in animasiyaları „iç içə
 * girməsinin“ də qarşısını alır (D-122 sıralama qərarı).
 *
 * Robolectric (JVM test): performHapticFeedback no-op — test təhlükəsizliyi.
 */
fun Modifier.pressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    haptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val view = LocalView.current
    this
        .scaleTap(interactionSource, enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
        ) {
            if (haptic) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onClick()
        }
}

/**
 * D-121: haptik „tick“ — pressable istifadə edə bilməyən yerlər üçün (məs.
 * vendored FloatingTabBar-in onClick lambda-ları). Click anında çağırılır.
 */
@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } }
}

/**
 * D-121: pressable-in SADƏ yükü — interactionSource daxildən yadda saxlanır
 * (sətir/kart çağırışlarında ayrıca `remember { MutableInteractionSource() }`
 * yazmaq lazım deyil). Dil eynidir: pressScale + haptik + indication=null.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    this.pressable(interaction, enabled, haptic, onClick)
}

/** CountUp — animasiyalı rəqəm (XP/stat); göstərilən dəyəri qaytarır. */
@Composable
fun countUpAsState(value: Int): Int {
    val animationsOn = rememberAnimationsOn()
    val displayed by animateIntAsState(
        targetValue = value,
        animationSpec = if (animationsOn) tween(Motion.D4, easing = Motion.ease) else tween(0),
        label = "countUp",
    )
    return displayed
}

/** TypingDots — 3 nöqtə dalğası (yazır göstəricisi). */
@Composable
fun TypingDots(color: Color, size: Float = 5f) {
    val animationsOn = rememberAnimationsOn()
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = LinearEasing), RepeatMode.Restart
        ),
        label = "phase",
    )
    Row {
        repeat(3) { i ->
            val t = if (animationsOn) {
                val local = (phase - i * 0.18f) % 1f
                val adj = if (local < 0) local + 1f else local
                adj * adj // easeOutQuad ≈
            } else 0f
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    // D-054: dp əsaslı hoppanma (əvvəl xam 3f px)
                    .graphicsLayer { translationY = -3.dp.toPx() * t }
                    .size(size.dp)
                    .alpha(0.35f + 0.65f * t)
                    .background(color, CircleShape)
            )
        }
    }
}
