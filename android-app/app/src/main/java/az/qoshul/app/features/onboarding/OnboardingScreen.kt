package az.qoshul.app.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.model.LocalDate
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.FormFieldX
import az.qoshul.app.core.ui.LogoMark
import az.qoshul.app.core.ui.Wordmark
import az.qoshul.app.features.auth.QoshulTextField
import kotlinx.coroutines.launch

/**
 * Onboarding — Flutter onboarding_screen.dart portu (ROL B):
 * 0) 3 slaydlıq tanışlıq (Kəşf et / Qoşul / Qazan) + "Keç"
 * 1) Profil: ad, doğum tarixi (18+), cins (ist.), şəkil 1–6, bio 150
 * 2) Maraqlar: 9 kateqoriya, min 3, maks 8
 * 3) İcazələr: məkan + bildirişlər (rdd mümkün) → finishOnboarding (+50 XP)
 */
@Composable
fun OnboardingScreen(engine: AppEngine) {
    var step by remember { mutableStateOf(0) }
    androidx.compose.animation.AnimatedContent(
        targetState = step,
        transitionSpec = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))
                .togetherWith(
                    androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(200)
                    )
                )
        },
        label = "onboarding",
    ) { s ->
        when (s) {
            0 -> WelcomeStep(onNext = { step = 1 })
            1 -> ProfileStep(engine, onNext = { step = 2 })
            2 -> InterestsStep(engine, onNext = { step = 3 })
            else -> PermissionsStep(engine)
        }
    }
}

// ───────────────────────── Addım 0: slaydlar ─────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val slides = listOf(
        Triple(Icons.Filled.Explore, "Kəşf et",
            "Yaxınlığındakı insanları, tədbirləri və məkanları kəşf et. Ortaq maraqlar sizi birləşdirir."),
        Triple(Icons.Filled.GroupAdd, "Qoşul",
            "Tədbirlərə qoşul, stol rezerv et, qrup söhbətlərində tanış olduğun insanlarla plan et."),
        Triple(Icons.Filled.EmojiEvents, "Qazan",
            "Hər addımda XP qazan. XP ilə səviyyə qalx və partnyor məkanlarda endirim kuponları al."),
    )
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Q.background)
            .androidxStatusPadding()
            // D-053 fix: "İrəli" düyməsi gesture/3-düymə nav barının altında qalırdı
            .navigationBarsPadding()
    ) {
        TextButton(
            onClick = onNext,
            modifier = Modifier.align(Alignment.End).padding(end = 12.dp),
        ) { Text("Keç", style = androidx.compose.ui.text.TextStyle(fontFamily = JakartaSans, fontSize = 12.sp)) }
        // Müvəqqəti brend nişanı (D-046) — tanışlıq slaydları üzərində
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PopIn { LogoMark(size = 60.dp) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val (icon, title, text) = slides[page]
            PopIn(key = title) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 34.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.size(84.dp).background(Q.secondary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = Q.primary, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(title, style = T.hero)
                    Spacer(Modifier.height(12.dp))
                    Text(text, style = T.subtitle.copy(fontSize = 13.sp, lineHeight = 23.sp))
                }
            }
        }
        // nöqtələr — aktiv enlənir (D-025 morph)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) {
            repeat(slides.size) { i ->
                val active = pager.currentPage == i
                val w by animateDpAsState(
                    targetValue = if (active) 18.dp else 6.dp,
                    animationSpec = tween(Motion.D2, easing = Motion.ease),
                    label = "dot",
                )
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .width(w)
                        .height(6.dp)
                        .background(
                            if (active) Q.primary else Q.border,
                            RoundedCornerShape(3.dp),
                        )
                )
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < slides.size - 1) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else onNext()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .height(52.dp),
            shape = RoundedCornerShape(Q.rPill),
            colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
        ) {
            Text(
                if (pager.currentPage < slides.size - 1) "İrəli" else "Başlayaq!",
                style = T.button,
            )
        }
        Spacer(Modifier.height(22.dp))
    }
}

private fun Modifier.androidxStatusPadding(): Modifier = this.statusBarsPadding()

// ───────────────────────── Addım 1: profil ─────────────────────────

@Composable
private fun ProfileStep(engine: AppEngine, onNext: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var birth by remember { mutableStateOf<LocalDate?>(null) }
    var gender by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Q.background)
            // D-053 fix: (1) insetlər scroll-dan ƏVVƏL — əvvəl padding scroll
            // ilə birlikdə yuxarı qaçırdı, məzmun status barın altına sürüşürdü;
            // (2) navigationBarsPadding — alt bar altında məzmun;
            // (3) imePadding — klaviatura ad/bio sahələrini örtürdü
            // (edge-to-edge + adjustResize klaviatura üçün kifayət etmir).
            .androidxStatusPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Spacer(Modifier.height(18.dp))
        Wordmark(fontSize = 22)
        Spacer(Modifier.height(14.dp))
        Text("Profilini yarad", style = T.h1)
        Spacer(Modifier.height(4.dp))
        Text("Səni tanımağa başlayaq — sonra maraqlarını seçəcəyik.", style = T.subtitle)
        FormFieldX("Ad və soyad") {
            QoshulTextField(value = name, onValueChange = { if (it.length <= 60) name = it },
                hint = "Məs: Aysel Əliyeva")
        }
        FormFieldX("Doğum tarixi (18+)", hint = "Yaş həddi yoxlaması aparılır") {
            var showPicker by remember { mutableStateOf(false) }
            Button(
                onClick = { showPicker = true },
                shape = RoundedCornerShape(Q.rPill),
                colors = ButtonDefaults.buttonColors(containerColor = Q.card, contentColor = Q.foreground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
            ) {
                Icon(Icons.Filled.Cake, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(birth?.let {
                    "%02d.%02d.%d".format(it.dayOfMonth, it.monthValue, it.year)
                } ?: "Tarix seç", style = T.button)
            }
            if (showPicker) {
                BirthDatePicker(
                    initial = birth,
                    onPick = {
                        birth = it
                        showPicker = false
                    },
                    onDismiss = { showPicker = false },
                )
            }
        }
        FormFieldX("Cins (istəyə bağlı)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kişi" to Icons.Filled.Male, "Qadın" to Icons.Filled.Female,
                    "Digər" to Icons.Filled.MoreHoriz).forEach { (g, ic) ->
                    GenderChip(g, ic, gender == g) {
                        gender = if (gender == g) "" else g
                    }
                }
            }
        }
        FormFieldX("Şəkil ($photos/${XpRules.MAX_PHOTOS})", hint = "Azı 1 şəkil məcburidir — yüklənəndə moderasiyaya düşür") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..XpRules.MAX_PHOTOS).forEach { i ->
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(
                                if (i <= photos) Q.secondary else Q.card,
                                RoundedCornerShape(Q.rMd.dp),
                            )
                            .border(
                                1.4.dp.takeIf { i <= photos } ?: 1.dp,
                                if (i <= photos) Q.primary else Q.border,
                                RoundedCornerShape(Q.rMd.dp),
                            )
                            .clickable { photos = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (i <= photos) Icons.Filled.Check else Icons.Filled.AddPhotoAlternate,
                            null, tint = if (i <= photos) Q.primary else Q.mutedFg,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
        FormFieldX("Qısa bio (${bio.length}/${XpRules.BIO_LIMIT})") {
            QoshulTextField(value = bio, onValueChange = { if (it.length <= XpRules.BIO_LIMIT) bio = it },
                maxLines = 3, hint = "Özün haqqında bir-iki cümlə...")
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Q.destructive,
                style = androidx.compose.ui.text.TextStyle(fontFamily = JakartaSans, fontSize = 12.5.sp))
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                when {
                    name.isBlank() -> error = "Adını yaz."
                    birth == null -> error = "Doğum tarixini seç."
                    birth?.let { !XpRules.isAdult(it) } == true ->
                        error = "Qoshul 18+ platformadır. (Rəsmi yaş həddi təsdiq gözləyir)"
                    else -> {
                        val parts = name.trim().split(Regex("\\s+"))
                        engine.setProfileCreation(
                            first = parts.first(),
                            last = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else "",
                            birth = birth!!,
                            gdr = gender,
                            bioText = bio.trim(),
                            photos = photos,
                        )
                        onNext()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(Q.rPill),
            colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
        ) { Text("Davam et", style = T.button) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GenderChip(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(36.dp)
            .background(if (active) Q.primary else Q.card, RoundedCornerShape(Q.rMd.dp))
            .border(1.dp, if (active) Q.primary else Q.border, RoundedCornerShape(Q.rMd.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
    ) {
        Icon(icon, null, tint = if (active) Q.primaryFg else Q.mutedFg, modifier = Modifier.size(14.5.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = if (active) T.chipActive else T.chip)
    }
}

// ───────────────────────── Addım 2: maraqlar ─────────────────────────

@Composable
private fun InterestsStep(engine: AppEngine, onNext: () -> Unit) {
    val selected = remember { mutableStateOf(setOf<String>()) }
    val icons = mapOf(
        "Əyləncə və Gecə həyatı" to Icons.Filled.Nightlife,
        "Kafe və Yemək" to Icons.Filled.LocalCafe,
        "İdman və Aktivlik" to Icons.Filled.SportsVolleyball,
        "Təbiət və Səyahət" to Icons.Filled.Terrain,
        "Mədəniyyət və İncəsənət" to Icons.Filled.Palette,
        "Təhsil və Karyera" to Icons.Filled.BusinessCenter,
        "Oyun və Hobbi" to Icons.Filled.Casino,
        "Musiqi" to Icons.Filled.MusicNote,
        "Könüllülük və Sosial" to Icons.Filled.VolunteerActivism,
    )
    val ok = selected.value.size >= XpRules.MIN_INTERESTS

    Column(
        Modifier
            .fillMaxSize()
            .background(Q.background)
            // D-053 fix: insetlər scroll-dan əvvəl + navbar + ime (yuxarıda izah)
            .androidxStatusPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Spacer(Modifier.height(18.dp))
        Wordmark(fontSize = 22)
        Spacer(Modifier.height(14.dp))
        Text("Maraqlarını seç", style = T.h1)
        Spacer(Modifier.height(4.dp))
        Text(
            "Ən azı ${XpRules.MIN_INTERESTS}, ən çoxu ${XpRules.MAX_INTERESTS} kateqoriya. " +
                "Bunlar kəşf axınını, match sırasını və bildirişləri müəyyən edir.",
            style = T.subtitle,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Seçilib: ${selected.value.size}/${XpRules.MAX_INTERESTS}",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 12.5.sp, fontWeight = FontWeight.W700
            ),
        )
        Spacer(Modifier.height(14.dp))
        engine.dataSource.rolCategories.forEach { c ->
            val isSel = c in selected.value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(
                        if (isSel) Q.secondary else Q.card,
                        RoundedCornerShape(Q.rLg.dp),
                    )
                    .border(
                        1.4.dp.takeIf { isSel } ?: 1.dp,
                        if (isSel) Q.primary else Q.border,
                        RoundedCornerShape(Q.rLg.dp),
                    )
                    .clickable {
                        selected.value = if (isSel) selected.value - c
                        else if (selected.value.size < XpRules.MAX_INTERESTS) selected.value + c
                        else selected.value
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Icon(icons[c] ?: Icons.Filled.Explore, null,
                    tint = if (isSel) Q.primary else Q.mutedFg, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c, style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W700))
                    Spacer(Modifier.height(2.dp))
                    Text(engine.dataSource.rolCategoryHints[c] ?: "", style = T.meta)
                }
                if (isSel) Icon(Icons.Filled.CheckCircle, null, tint = Q.primary, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                engine.setInterests(selected.value.toList())
                onNext()
            },
            enabled = ok,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(Q.rPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = Q.primary, contentColor = Q.primaryFg,
                disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg,
            ),
        ) {
            Text(
                if (ok) "Davam et (${selected.value.size} maraq)"
                else "Ən azı ${XpRules.MIN_INTERESTS} maraq seç",
                style = T.button,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ───────────────────────── Addım 3: icazələr ─────────────────────────

@Composable
private fun PermissionsStep(engine: AppEngine) {
    var location by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Q.background)
            // D-053 fix: insetlər scroll-dan əvvəl + navbar + ime (yuxarıda izah)
            .androidxStatusPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Spacer(Modifier.height(18.dp))
        Wordmark(fontSize = 22)
        Spacer(Modifier.height(14.dp))
        Text("İcazələr", style = T.h1)
        Spacer(Modifier.height(4.dp))
        Text("Hər ikisi istəyə bağlıdır — rədd etsən də tətbiq işləməyə davam edir.", style = T.subtitle)
        Spacer(Modifier.height(18.dp))
        PermCard(Icons.Filled.LocationOn, "Məkan",
            "Yaxınlıq üçün · \"tətbiq istifadə olunarkən\"\nDəqiq koordinat yalnız sənə görünür, başqalarına ~məsafə.",
            location) { location = true }
        PermCard(Icons.Filled.Notifications, "Bildirişlər",
            "Match, mesaj, xatırlatma və rezerv yenilikləri (FCM).",
            notifications) { notifications = true }
        Spacer(Modifier.height(16.dp))
        CommunityBanner(
            icon = Icons.Filled.EmojiEvents,
            title = "Hazırsan!",
            subtitle = "Qeydiyyatı tamamladıqda +${XpRules.REGISTRATION_XP} XP qazanacaqsan. " +
                "Sonra profilini 100% doldurub əlavə +${XpRules.PROFILE_COMPLETE_XP} XP qazana bilərsən.",
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { engine.finishOnboarding() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(Q.rPill),
            colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
        ) { Text("Qoshul-a başla", style = T.button) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermCard(
    icon: ImageVector, title: String, subtitle: String,
    granted: Boolean, onAsk: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .border(
                1.4.dp.takeIf { granted } ?: 1.dp,
                if (granted) Q.primary else Q.border,
                RoundedCornerShape(Q.rLg.dp),
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(
                    if (granted) Q.secondary else Q.muted,
                    RoundedCornerShape(Q.rMd.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = if (granted) Q.primary else Q.mutedFg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W800))
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 11.5.sp, lineHeight = 18.sp, color = Q.mutedFg))
            }
            if (granted) {
                Icon(Icons.Filled.CheckCircle, null, tint = Q.primary, modifier = Modifier.size(22.dp))
            } else {
                TextButton(onClick = onAsk) { Text("İcazə ver", style = T.button) }
            }
        }
    }
}

/** Material3 DatePicker — doğum tarixi seçimi (18+ yoxlaması AppEngine-də). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDatePicker(
    initial: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(java.time.ZoneOffset.UTC)
            ?.toInstant()?.toEpochMilli()
            ?: java.time.LocalDate.of(2004, 1, 15)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    onPick(
                        java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    )
                } ?: onDismiss()
            }) { Text("Seç", style = T.button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ləğv et", style = T.button) } },
    ) {
        DatePicker(state = state)
    }
}
