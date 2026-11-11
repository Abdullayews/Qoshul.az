package az.qoshul.app.features.map

import android.os.Build
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.glass.GlassPill
import az.qoshul.app.core.glass.GlassSurface
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.motion.pressable
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.SelectBoxTrigger
import az.qoshul.app.core.ui.PickHostOverlay
import az.qoshul.app.core.ui.PickOption
import az.qoshul.app.core.ui.pickAnchor
import az.qoshul.app.core.ui.pickHost
import az.qoshul.app.core.ui.rememberPickHostState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.events.EventDetailSheet
import az.qoshul.app.features.venues.ReserveSheet

/**
 * Xəritə tabı (ROL TAB 2, D-027/D-030/D-048): pinlər + filtr + alt kart.
 *
 * D-108 (v0.39.0, direktif 2026-11-09: „map da google sdk keçmək üçün
 * hazırlaş“): kanvas BACKEND-SEÇİLƏNDir — MapScreen özü yalnız məlumat
 * (pinlər, filtrlər, seçim) və xromu (filtr paneli, sayğac, alt kart)
 * saxlayır; tile səthini çəkən kanvas BuildConfig.MAP_BACKEND ilə seçilir.
 * Pin modeli (MapPins.kt) backend-agnostikdir — hər iki kanvas eyni
 * siyahını render edir.
 *
 * D-109 (v0.40.0, direktif 2026-11-09: „google ilə dəyişdirib“): GOOGLE
 * ƏSAS backenddir — default MAP_BACKEND=google (local.properties/env);
 * açar (MAPS_API_KEY → BuildConfig.MAPS_KEY_PRESENT) olanda
 * GoogleMapCanvas (maps-compose 9.0.1, MarkerComposable pin dili),
 * açarsızda maplibre fallback (D-048 açarsız OpenFreeMap). Seçim məntiqi
 * resolveMapBackend (MapPins.kt) saf funksiyasındadır.
 *
 * Pinlər (D-027 görünüş rejimi): məkan = yaşal dot + storefront qlifi,
 * tədbir = narıncı dot + təqvim qlifi, insan = adın baş hərfi. Seçim
 * halosu, boş toxunma = seçimi buraxır.
 */
@Composable
fun MapScreen(engine: AppEngine, nav: NavState, focus: MapFocusState) {
    val st by engine.state.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf("Hamısı") }
    var distanceKm by remember { mutableStateOf(10) }
    var openNow by remember { mutableStateOf(false) }
    var withDiscount by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MapPin?>(null) }
    var reserveVenue by remember { mutableStateOf<Partner?>(null) }
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }
    var pendingCamera by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // D-089: xəritə filtr pick host-u (kateqoriya + məsafə panelləri)
    val pickHost = rememberPickHostState()
    val hour = remember { engine.now().hour } // A2: vahid clock

    // D-129 (direktiv 2026-11-11: „istifadəçidən lokasiya, şəkil, saxlama
    // icazələri al“): xəritə ilk dəfə açılanda (SESSİYA başına bir dəfə —
    // bayraq MapFocusState-də, tab keçidlərində ölmür) lokasiya icazəsi
    // istənilir — yaxınlıq pinləri/məsafə filtrləri üçün. Rədd → seçilmiş
    // şəhərin mərkəzi ilə davam (mövcud demo davranışı dəyişmir).
    val locCtx = androidx.compose.ui.platform.LocalContext.current
    val locLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    if (!focus.locationPermissionAsked &&
        android.content.pm.PackageManager.PERMISSION_GRANTED !=
        androidx.core.content.ContextCompat.checkSelfPermission(
            locCtx, android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            focus.locationPermissionAsked = true
            locLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // U1.1 (UI-03 pattern): seçilmiş pinin alt kartı açıqdırsa GERİ kartı
    // bağlayır (app-i yox). PickHost overlay (D-089) öz BackHandler-ini
    // qoyur — içdə olduğu üçün (innermost) üstünlük onundur.
    BackHandler(enabled = selected != null) { selected = null }

    val myLat = 40.4093
    val myLng = 49.8671

    // — pinlər (filtrlərlə, D-027 görünüş rejimi) —
    val venuePins = engine.partners.filter { v ->
        v.city == st.city && (category == "Hamısı" || v.category == category) &&
            (!openNow || v.isOpenAt(hour)) && (!withDiscount || v.hasDiscount)
    }.map { MapPin.Venue(it) }
    val eventPins = engine.cityEvents.filter {
        (category == "Hamısı" || it.category == category) && it.distance <= distanceKm
    }.map { MapPin.Event(it) }
    val peoplePins = engine.matchCandidates
        .filter { it.distanceKm <= distanceKm }
        .filter {
            st.mapVisibility == MapVisibility.EVERYONE ||
                (st.mapVisibility == MapVisibility.FRIENDS && st.friendships[it.id] == "accepted")
        }.map { MapPin.PersonP(it) }
    val pins = venuePins + eventPins + peoplePins

    // D-089: xəritə filtr ikonları (əvvəl inline siyahı idi) + D-110 izahları
    val mapCatIcons = linkedMapOf(
        "Hamısı" to Icons.Filled.Public, "İdman" to Icons.Filled.SportsVolleyball,
        "Qəhvə" to Icons.Filled.LocalCafe, "Texnologiya" to Icons.Filled.Memory,
        "Piknik" to Icons.Filled.Park,
    )
    val mapCatHints = mapOf(
        "Hamısı" to "Bütün pinlər görünür",
        "İdman" to "İdman məkanları və tədbirlər",
        "Qəhvə" to "Kafe və qəhvə görüşləri",
        "Texnologiya" to "IT məkanları",
        "Piknik" to "Açıq hava məkanları",
    )

    // Robolectric (JVM test): MapLibre FFI .so yüklənə bilməz, Google SDK
    // isə Play Services gözləyir → hər iki kanvas JVM testdə YOXDUR; UI
    // smoke-testləri qalan qatları (çiplər, kartlar) yoxlayır.
    val isJvmTest = remember { Build.FINGERPRINT.contains("robolectric", ignoreCase = true) }

    // — D-050: xəritə fokus istəyi ("Yol göstər"/"Yol tap") — pin seç + kameraya al
    LaunchedEffect(focus.request) {
        when (val t = focus.request) {
            is MapFocusState.Target.Venue ->
                venuePins.find { it.v.id == t.venueId }?.let {
                    selected = it; pendingCamera = it.lng to it.lat
                }
            is MapFocusState.Target.Event ->
                eventPins.find { it.e.id == t.eventId }?.let {
                    selected = it; pendingCamera = it.lng to it.lat
                }
            null -> Unit
        }
        focus.consume()
    }

    Box(Modifier.fillMaxSize().pickHost(pickHost).background(Q.tileFallback)) {
        // — XƏRİTƏ KANVASI (D-108 seam + D-109 keçidi: google əsas,
        // açarsız maplibre fallback — resolveMapBackend) —
        val backend = remember {
            resolveMapBackend(BuildConfig.MAP_BACKEND, BuildConfig.MAPS_KEY_PRESENT)
        }
        if (!isJvmTest) {
            if (backend == "google") {
                GoogleMapCanvas(
                    pins = pins,
                    myLat = myLat,
                    myLng = myLng,
                    selectedKey = selected?.key(),
                    pendingCamera = pendingCamera,
                    onPinTap = { selected = it },
                    onPinDismiss = { selected = null },
                    onCameraConsumed = { pendingCamera = null },
                )
            } else {
                MaplibreCanvas(
                    pins = pins,
                    myLat = myLat,
                    myLng = myLng,
                    selectedKey = selected?.key(),
                    pendingCamera = pendingCamera,
                    onPinTap = { selected = it },
                    onPinDismiss = { selected = null },
                    onCameraConsumed = { pendingCamera = null },
                )
            }
        }

        // — şüşə filtr paneli (D-025 səth #2) —
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(16.dp),
            sigma = 16.dp,
            luminance = 0.5f,
        ) {
            Column(Modifier.padding(10.dp)) {
                // D-110: iki sürüşən filtr zolağı → iki SELECTOR BOX (etiketli
                // qutu; panellər PickHostOverlay-də — root Box-da, aşağıda)
                Row {
                    Box(Modifier.weight(1f)) {
                        SelectBoxTrigger(
                            fieldLabel = "Kateqoriya",
                            value = category,
                            icon = mapCatIcons[category] ?: Icons.Filled.Public,
                            open = pickHost.openReq?.key == "mcat",
                            onClick = {
                                pickHost.toggle(
                                    "mcat",
                                    mapCatIcons.map {
                                        PickOption(it.key, it.value, mapCatHints[it.key])
                                    },
                                    category,
                                ) { category = it }
                            },
                            modifier = Modifier.pickAnchor(pickHost, "mcat"),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        SelectBoxTrigger(
                            fieldLabel = "Məsafə",
                            value = "$distanceKm km",
                            icon = Icons.Filled.NearMe,
                            open = pickHost.openReq?.key == "mdist",
                            onClick = {
                                pickHost.toggle(
                                    "mdist",
                                    listOf(1, 3, 5, 10).map {
                                        PickOption("$it km", Icons.Filled.NearMe)
                                    },
                                    "$distanceKm km",
                                ) { distanceKm = it.removeSuffix(" km").toInt() }
                            },
                            modifier = Modifier.pickAnchor(pickHost, "mdist"),
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
                Row {
                    GlassFilterChip("indi açıqdır", Icons.Filled.Schedule, openNow) { openNow = !openNow }
                    Spacer(Modifier.width(7.dp))
                    GlassFilterChip("endirimi var", Icons.Filled.Percent, withDiscount) { withDiscount = !withDiscount }
                }
            }
        }

        // D-110: xəritə filtr panelləri — xəritənin ÜZƏRİNƏ (vahid sütun,
        // ikon + başlıq + izah sətirləri)
        PickHostOverlay(
            host = pickHost,
            panelModifier = Modifier.align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            maxHeight = 320.dp,
        )
        // — sayğac (şüşə həb) —
        // D-053 fix: nav pill zone-unun üstünə qaldırıldı (əvvəl arxasında gizlənirdi)
        GlassPill(
            modifier = Modifier.align(Alignment.BottomStart)
                .navigationBarsPadding().padding(start = 12.dp, bottom = 96.dp),
            dense = true, horizontal = 9.dp, vertical = 6.dp,
        ) {
            Text(
                "${venuePins.size} məkan · ${eventPins.size} tədbir · ${peoplePins.size} insan",
                style = T.glassText(w = FontWeight.W600), // default 11sp — UI-25: 10sp qadağan
            )
        }

        // — D-027: görünüş rejimi həbi —
        if (st.mapVisibility != MapVisibility.EVERYONE) {
            // U3.1 (UI-06): görünüş rejimi həbi glass deyil (D-096) —
            // opaq photoShade dili (zoom pilləri ilə eyni qayda deyil: tünd scrim,
            // çünki xəritə üzərində şəffaf ağ oxunmur); mətn T.badge (UI-25)
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .navigationBarsPadding().padding(end = 12.dp, bottom = 96.dp)
                    .background(Q.photoShade.copy(alpha = 0.55f), RoundedCornerShape(Q.rPill))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (st.mapVisibility == MapVisibility.FRIENDS) Icons.Filled.Group
                        else Icons.Filled.VisibilityOff, null,
                        tint = Q.primaryText, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (st.mapVisibility == MapVisibility.FRIENDS) "Yalnız dostlar görünür" else "Gizli rejim",
                        style = T.badge.copy(fontWeight = FontWeight.W600, color = Q.photoFg),
                    )
                }
            }
        }

        // — seçilmiş pinin alt kartı (D-025 səth #3) —
        // D-053 fix: kart nav pill-in arxasında qalırdı — „Ətraflı“/„Rezerv et“
        // düymələri basıla bilmirdi. Pill zone = navbar inset + 10 + 68dp;
        // kart inset + 96dp-dən yuxarı başlayır (18dp boşluq, hər navbar növündə).
        selected?.let { pin ->
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 96.dp),
                shape = RoundedCornerShape(Q.rXl.dp),
                sigma = 18.dp,
                luminance = 0.58f,
            ) {
                BottomCardContent(
                    pin = pin,
                    onVenueDetail = { nav.push(Route.VenueDetail(it.id)); selected = null },
                    onReserve = { reserveVenue = it },
                    onEventDetail = { detailEvent = it },
                    onMeetPerson = { nav.push(Route.Match); selected = null },
                )
            }
        }
    }

    // — alt kartın açdığı sheetlər (D-050: ölü düymələr canlandı) —
    reserveVenue?.let { v ->
        ReserveSheet(engine, v, onDismiss = { reserveVenue = null })
    }
    detailEvent?.let { ev ->
        EventDetailSheet(
            engine, ev,
            onDismiss = { detailEvent = null },
            onShowOnMap = { e -> detailEvent = null; focus.event(e.id) },
        )
    }
}

/** Alt kart məzmunu — pin tipinə görə (D-050: bütün düymələr canlıdır). */
@Composable
private fun BottomCardContent(
    pin: MapPin,
    onVenueDetail: (Partner) -> Unit,
    onReserve: (Partner) -> Unit,
    onEventDetail: (QoshulEvent) -> Unit,
    onMeetPerson: () -> Unit,
) {
    when (pin) {
        is MapPin.Venue -> {
            val v = pin.v
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = imagePaint(v.image), contentDescription = v.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(Q.rMd.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = T.glassText(size = 14))
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = Q.accent, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(String.format("%.1f · %s", v.rating, v.priceRange), style = T.meta)
                            if (v.hasDiscount) {
                                Spacer(Modifier.width(6.dp))
                                Text("-${v.discountPercent}%", style = T.chipXs.copy(fontWeight = FontWeight.W800, color = Q.accentFg))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(v.address, style = T.metaSm)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    BottomCardButton("Ətraflı", primary = false, onClick = { onVenueDetail(v) })
                    Spacer(Modifier.width(8.dp))
                    BottomCardButton("Rezerv et", primary = true, onClick = { onReserve(v) })
                }
            }
        }
        is MapPin.Event -> {
            val e = pin.e
            Column(Modifier.padding(14.dp)) {
                Text(e.title, style = T.glassText(size = 14))
                Spacer(Modifier.height(4.dp))
                Text("${e.category} · ${e.date}, ${e.time} · ~${e.distance} km", style = T.meta)
                Spacer(Modifier.height(12.dp))
                BottomCardButton("Ətraflı", primary = true, onClick = { onEventDetail(e) }, fill = true)
            }
        }
        is MapPin.PersonP -> {
            val p = pin.p
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = imagePaint(p.image), contentDescription = p.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${p.firstName}, ${p.age}", style = T.glassText(size = 14))
                        Spacer(Modifier.height(3.dp))
                        Text("${p.distanceLabel} · ${p.match}% uyğunluq", style = T.meta)
                    }
                }
                Spacer(Modifier.height(12.dp))
                BottomCardButton("Tanış ol", primary = true, onClick = onMeetPerson, fill = true)
            }
        }
    }
}

@Composable
private fun BottomCardButton(
    label: String, primary: Boolean, onClick: () -> Unit, fill: Boolean = false,
) {
    // D-121: pressable dili — pressScale + haptik + indication=null
    val interaction = remember { MutableInteractionSource() }
    val bg = if (primary) Q.primaryText else Q.mapPin.copy(alpha = 0.5f)
    val fg = if (primary) Q.primaryFg else Q.foreground
    val line = if (fill) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(0.42f)
    Box(
        line
            .height(42.dp)
            .pressable(interaction, onClick = onClick)
            .background(bg, RoundedCornerShape(Q.rPill)),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = T.button.copy(color = fg)) }
}

/** Şüşə filtr çipi — xəritə üzərində üzən seçim həbi. */
@Composable
private fun GlassFilterChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressable(interaction, onClick = onClick)
            .height(31.dp)
            .androidxChipBg(active)
            .padding(horizontal = 11.dp),
    ) {
        Icon(icon, null, tint = if (active) Q.primaryFg else Q.foreground, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = T.chipXs.copy(fontWeight = if (active) FontWeight.W700 else FontWeight.W500, color = if (active) Q.primaryFg else Q.foreground))
    }
}

private fun Modifier.androidxChipBg(active: Boolean): Modifier = this.then(
    Modifier
        .background(if (active) Q.primary.copy(alpha = 0.88f) else Q.mapPin.copy(alpha = 0.42f), RoundedCornerShape(Q.rMd.dp))
        .border(
            if (active) 1.2.dp else 1.dp,
            if (active) Q.primaryText else Q.mapPin.copy(alpha = 0.6f),
            RoundedCornerShape(Q.rMd.dp),
        )
)
