package az.qoshul.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.theme.Q
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T

/**
 * Naviqasiya — Flutter `Navigator.push(GlassPageRoute(...))` pariteti (D-036).
 *
 * Öz yüngül back-stack (Route siyahısı) + AnimatedContent keçidi:
 * fade + 18px yuxarı sürüşmə + 0.98→1 miqyas (GlassPageRoute port).
 * Navigation-Compose kitabxanası bu mərhələdə RƏDD (Flutter imperativ
 * Navigator modelinə birebir uyğun + deep-link lazım olanda əlavə olunar;
 * qərar docs/decisions.md D-036).
 *
 * D-127 (v0.44.0, direktiv 2026-11-11: „ayarlara girib çıxandan sonra
 * istifadəçi Profil tabında qalsın, menyuya yönləndirilməsin“): hər ekran
 * SaveableStateProvider içindədir — push/pop roundtrip-də rememberSaveable
 * state İTMİR. Əvvəl AppShell (home) push açılanda kompozisiyadan çıxır,
 * tab index-i (Profil) sıfırlanırdı — ayarlardan qayıdanda istifadəçi
 * Ana menyuya düşürdü. İndi index=3 bərpa olunur (navigation-compose
 * NavHost-un eyni mexanizmi).
 *
 * D-134 (direktiv: „animasiyaları daha da araşdır“): push/pop keçidi 2
 * FAZALIDIR (D-122 tab keçidi pariteti) — (1) köhnə ekran 140ms-də tam
 * çıxır, (2) yeni ekran yalnız SONRA girir (220ms, 140ms gecikmə ilə).
 * Əvvəlki vahid crossfade-də iki ekran eyni anda yarı-şəffaf olurdu —
 * arxada qalan window fonu (ağ) görünüb „iç içə“ təsir bağışlayırdı
 * (D-128 ilə birlikdə tam həll: artıq arxada tema rəngi görünür).
 */

/** Push olunan ekranlar (AppShell daxilində). */
sealed interface Route {
    data object Partners : Route
    data object Assistant : Route
    data object Notifications : Route
    data object Match : Route
    data object Coupons : Route
    data object Reservations : Route
    data object XpCard : Route
    data object Settings : Route
    data object Safety : Route
    /** D-056: şəxsiyyət testi (10 sual → PersonalityResult). */
    data object Quiz : Route
    /** D-112: profil redaktə ekranı (ad, soyad, profil şəkli seçimi). */
    data object ProfileEdit : Route
    data class VenueDetail(val venueId: String) : Route
}

/** Naviqasiya vəziyyəti — rememberNavController. */
class NavState {
    val stack = mutableStateListOf<Route>()
    val canPop: Boolean get() = stack.isNotEmpty()

    fun push(r: Route) {
        stack.add(r)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
    }

    fun popAll() = stack.clear()
}

@Composable
fun rememberNavState(): NavState = remember { NavState() }

/** GlassPageRoute keçidi: fade + slide-up + scale (D-025). */
@Composable
fun GlassNavHost(
    nav: NavState,
    home: @Composable () -> Unit,
    content: @Composable (Route) -> Unit,
) {
    val current: Route? = nav.stack.lastOrNull()
    // D-127: state retention — SaveableStateHolder hər ekranın saveable
    // state-ini kompozisiyadan çıxanda saxlayır, geri qayıdanda bərpa edir
    // (navigation-compose NavHost-un eyni mexanizmi).
    val stateHolder = rememberSaveableStateHolder()
    // D-053 fix: sistem geri düyməsi/gesture push-ekranı POP etsin.
    // Əvvəl heç bir BackHandler yox idi — push-ekranda geri basanda bütün
    // tətbiq bağlanırdı (10 push-ekranın hamısında). Sheet-lər öz daxili
    // back-handler-lərinə görə üstünlük alır (son qeydiyyatlı callback).
    BackHandler(enabled = current != null) { nav.pop() }
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            // D-134: 2-fazalı sıralama — köhnə ekranın çıxışı (140ms) ilə
            // yeni ekranın girişi (220ms + 140ms gecikmə) ÜST-ÜSTƏ DÜŞMÜR.
            // Motion.ease = easeOutCubic (D-025 dil ailəsi).
            (
                fadeIn(tween(220, delayMillis = 140, easing = Motion.ease)) +
                    scaleIn(
                        initialScale = 0.98f,
                        animationSpec = tween(220, delayMillis = 140, easing = Motion.ease),
                    ) togetherWith
                    fadeOut(tween(140))
                ).using(androidx.compose.animation.SizeTransform(clip = false))
        },
        label = "glassNav",
        contentKey = { it?.hashCode() ?: -1 },
    ) { target ->
        if (target == null) {
            stateHolder.SaveableStateProvider("home") { home() }
        } else {
            stateHolder.SaveableStateProvider("route-${target.hashCode()}") { content(target) }
        }
    }
}

/** Push-ekran başlığı — Flutter AppBar portu (ağ fon, yaşıl geri düyməsi). */
@Composable
fun PushScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            // D-051 fix: push-ekranlar status barın altında qalırdı — bütün
            // push ekranlar bu scaffold-u istifadə edir (10 ekran bir düzəlişlə).
            // D-053 fix: (1) background padding-lərdən ƏVVƏL — barların arxası
            // da boyanır (əvvəl status bar zonası themes.xml-in ağ fonunda qalırdı);
            // (2) navigationBarsPadding — alt sistem barının altında məzmun
            // gizlənməsin (gesture/3-düymə nav, 10 ekran bir düzəlişlə).
            .background(Q.background)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        // AppBar (D-050 düzəlişi: fillMaxSize → fillMaxWidth — sətir hündürlüyü
        // height(56) ilə sabitlənir, amma semantika düzgün olmalı idi)
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Q.card)
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ChevronLeft, contentDescription = "Geri",
                    tint = Q.primaryText,
                )
            }
            Text(
                title,
                style = T.titleLg,
                modifier = Modifier.weight(1f),
            )
            actions?.invoke()
        }
        // Alt haşiyə
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Q.border)
        ) { }
        Box(Modifier.weight(1f)) {
            content(
                Modifier
                    .fillMaxSize()
                    .background(Q.background)
            )
        }
    }
}
