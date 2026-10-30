package az.qoshul.app.features.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.glass.GlassPill
import az.qoshul.app.core.glass.GlassSurface
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.imagePaint
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Xəritə tabı — Flutter map_screen.dart + tile_map.dart portu (ROL TAB 2,
 * D-027/D-030). Öz OSM tile renderer (Phase 8 qərarı: D-038 — bax
 * docs/decisions.md): Web Mercator, pinch/pan, diskret zoom commit,
 * sabit ölçülü pinlər, OSM atribusiyası.
 */
private const val TILE_SIZE = 256f

// ── Web Mercator riyaziyyatı (slippy) ──
private fun worldSize(zoom: Int): Float = TILE_SIZE * (1 shl zoom)
private fun lngToX(lng: Double, ws: Float): Float = ((lng + 180.0) / 360.0 * ws).toFloat()
private fun latToY(lat: Double, ws: Float): Float {
    val l = lat.coerceIn(-85.05112878, 85.05112878)
    val r = Math.toRadians(l)
    return ((1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * ws).toFloat()
}
private fun xToLng(x: Float, ws: Float): Double = x / ws * 360.0 - 180.0
private fun yToLat(y: Float, ws: Float): Double {
    val n = Math.PI - 2.0 * Math.PI * y / ws
    return Math.toDegrees(Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))))
}

/** Xəritə pin modeli. */
sealed interface MapPin {
    val lat: Double
    val lng: Double
    data class Venue(val v: Partner) : MapPin { override val lat = v.lat; override val lng = v.lng }
    data class Event(val e: QoshulEvent) : MapPin { override val lat = e.lat; override val lng = e.lng }
    data class PersonP(val p: Person) : MapPin { override val lat = p.mapLat; override val lng = p.mapLng }
}

@Composable
fun MapScreen(engine: AppEngine) {
    val st by engine.state.collectAsState()
    var category by remember { mutableStateOf("Hamısı") }
    var distanceKm by remember { mutableStateOf(10) }
    var openNow by remember { mutableStateOf(false) }
    var withDiscount by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MapPin?>(null) }
    val hour = remember { java.time.LocalDateTime.now().hour }

    val myLat = 40.4093
    val myLng = 49.8671

    // — pinlər (filtrlərlə, D-027 görünüş rejimi) —
    val venuePins = FakeQoshulDataSource.partners.filter { v ->
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

    Box(Modifier.fillMaxSize().background(Q.tileFallback)) {
        // — REAL xəritə (OSM tile-lar) —
        TileMapView(
            initLat = myLat, initLng = myLng, initZoom = 13,
            pins = pins,
            myLat = myLat, myLng = myLng,
            onBackgroundTap = { selected = null },
            onPinTap = { selected = it },
        )

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
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(
                        "Hamısı" to Icons.Filled.Public, "İdman" to Icons.Filled.SportsVolleyball,
                        "Qəhvə" to Icons.Filled.LocalCafe, "Texnologiya" to Icons.Filled.Memory,
                        "Piknik" to Icons.Filled.Park,
                    ).forEach { (name, icon) ->
                        GlassFilterChip(name, icon, category == name) { category = name }
                        Spacer(Modifier.width(7.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(1, 3, 5, 10).forEach { km ->
                        GlassFilterChip("$km km", Icons.Filled.NearMe, distanceKm == km) { distanceKm = km }
                        Spacer(Modifier.width(7.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    GlassFilterChip("indi açıqdır", Icons.Filled.Schedule, openNow) { openNow = !openNow }
                    Spacer(Modifier.width(7.dp))
                    GlassFilterChip("endirimi var", Icons.Filled.Percent, withDiscount) { withDiscount = !withDiscount }
                }
            }
        }

        // — sayğac (şüşə həb) —
        GlassPill(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            dense = true, horizontal = 9.dp, vertical = 6.dp,
        ) {
            Text(
                "${venuePins.size} məkan · ${eventPins.size} tədbir · ${peoplePins.size} insan",
                style = T.glassText(size = 10, w = FontWeight.W600),
            )
        }

        // — D-027: görünüş rejimi həbi —
        if (st.mapVisibility != MapVisibility.EVERYONE) {
            GlassPill(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                dense = true, horizontal = 9.dp, vertical = 6.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (st.mapVisibility == MapVisibility.FRIENDS) Icons.Filled.Group
                        else Icons.Filled.VisibilityOff, null,
                        tint = Q.primary, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (st.mapVisibility == MapVisibility.FRIENDS) "Yalnız dostlar görünür" else "Gizli rejim",
                        style = T.glassText(size = 10, w = FontWeight.W600),
                    )
                }
            }
        }

        // — seçilmiş pinin alt kartı (D-025 səth #3) —
        selected?.let { pin ->
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                shape = RoundedCornerShape(Q.rXl.dp),
                sigma = 18.dp,
                luminance = 0.58f,
            ) {
                BottomCardContent(pin)
            }
        }
    }
}

/** Alt kart məzmunu — pin tipinə görə. */
@Composable
private fun BottomCardContent(pin: MapPin) {
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
                                Text("-${v.discountPercent}%", style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 11.5.sp,
                                    fontWeight = FontWeight.W800, color = Q.accentFg))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(v.address, style = T.metaSm)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    // A3: düymələr "tezliklə" vəziyyətində deyil — real klik (boş) saxlanılıb
                    BottomCardButton("Ətraflı", primary = false, onClick = {})
                    Spacer(Modifier.width(8.dp))
                    BottomCardButton("Rezerv et", primary = true, onClick = {})
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
                BottomCardButton("Ətraflı", primary = true, onClick = {}, fill = true)
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
                BottomCardButton("Tanış ol", primary = true, onClick = {}, fill = true)
            }
        }
    }
}

@Composable
private fun BottomCardButton(
    label: String, primary: Boolean, onClick: () -> Unit, fill: Boolean = false,
) {
    val bg = if (primary) Q.primary else Color.White.copy(alpha = 0.5f)
    val fg = if (primary) Q.primaryFg else Q.foreground
    val line = if (fill) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(0.42f)
    Box(
        line
            .height(42.dp)
            .background(bg, RoundedCornerShape(Q.rMd.dp))
            .clickable(onClick = onClick),
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
            .androidxChipTap(interaction, onClick)
            .height(31.dp)
            .androidxChipBg(active)
            .padding(horizontal = 11.dp),
    ) {
        Icon(icon, null, tint = if (active) Q.primaryFg else Q.foreground, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 11.5.sp,
            fontWeight = if (active) FontWeight.W700 else FontWeight.W500,
            color = if (active) Q.primaryFg else Q.foreground))
    }
}

private fun Modifier.androidxChipTap(interaction: MutableInteractionSource, onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick))

private fun Modifier.androidxChipBg(active: Boolean): Modifier = this.then(
    Modifier
        .background(if (active) Q.primary.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.42f), RoundedCornerShape(Q.rMd.dp))
        .border(
            if (active) 1.2.dp else 1.dp,
            if (active) Q.primary else Color.White.copy(alpha = 0.6f),
            RoundedCornerShape(Q.rMd.dp),
        )
)

// ═══════════════════ TileMapView — öz OSM renderer ═══════════════════

/**
 * OSM raster tile renderer — Flutter tile_map.dart portu (D-030/D-038).
 *
 * Model: float zoom (zoomF) + mərkəz (lat/lng). Tile-lar tam zoomda
 * yüklənir, qat viewport mərkəzinə görə k = 2^(zoomF − zoom) miqyasında
 * çəkilir → pinch fasiləsiz, tam zoom pilləsinə keçid səssiz.
 * Pinç mərkəzi (centroid) coğrafi sabit nöqtədə saxlanılır.
 */
@Composable
fun TileMapView(
    initLat: Double,
    initLng: Double,
    initZoom: Int,
    pins: List<MapPin>,
    myLat: Double,
    myLng: Double,
    onBackgroundTap: () -> Unit,
    onPinTap: (MapPin) -> Unit,
) {
    var zoomF by remember { mutableStateOf(initZoom.toFloat()) }
    var centerLat by remember { mutableStateOf(initLat) }
    var centerLng by remember { mutableStateOf(initLng) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val zoom = zoomF.roundToInt().coerceIn(3, 18)
    val k = 2.0.pow((zoomF - zoom).toDouble()).toFloat()          // qat miqyası
    val ws = worldSize(zoom)                          // tam zoom dünya ölçüsü
    val cxW = lngToX(centerLng, ws)                   // mərkəz (dünya px)
    val cyW = latToY(centerLat, ws)

    // görünən dünya sərhədləri
    val halfW = (viewport.width / 2f) / k
    val halfH = (viewport.height / 2f) / k

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    val wsCur = worldSize(zoomF.roundToInt().coerceIn(3, 18))
                    val kCur = 2.0.pow((zoomF - zoomF.roundToInt()).toDouble()).toFloat()
                    // — pan: mərkəzi əks istiqamətə sürüşdür —
                    var ncx = lngToX(centerLng, wsCur) - pan.x / kCur
                    var ncy = latToY(centerLat, wsCur) - pan.y / kCur
                    // — pinch: centroid altındaki coğrafi nöqtə saxlanılır —
                    if (gestureZoom != 1f) {
                        val oldZoomF = zoomF
                        val newZoomF = (zoomF + (ln(gestureZoom.toDouble()) / ln(2.0)).toFloat())
                            .coerceIn(3f, 18f)
                        // centroid altındaki dünya nöqtəsi (tam-zoom koordinatında)
                        val wcx = ncx + (centroid.x - viewport.width / 2f) / kCur
                        val wcy = ncy + (centroid.y - viewport.height / 2f) / kCur
                        // yeni k
                        val newIntZoom = newZoomF.roundToInt().coerceIn(3, 18)
                        val newK = 2.0.pow((newZoomF - newIntZoom).toDouble()).toFloat()
                        // eyni dünya nöqtəsi eyni ekranda qalsın:
                        // mərkəz = wPoint − (centroid − viewport/2)/k'
                        val newWs = worldSize(newIntZoom)
                        ncx = (wcx - (centroid.x - viewport.width / 2f) / newK) *
                            (wsCur / newWs)
                        ncy = (wcy - (centroid.y - viewport.height / 2f) / newK) *
                            (wsCur / newWs)
                        zoomF = newZoomF
                        // world ölçüsü dəyişə bilər — yeni tam zoomda yenidən hesabla
                        val finalWs = worldSize(zoomF.roundToInt().coerceIn(3, 18))
                        val geoLng = xToLng(ncx, wsCur)
                        val geoLat = yToLat(ncy, wsCur)
                        ncx = lngToX(geoLng, finalWs)
                        ncy = latToY(geoLat, finalWs)
                    }
                    centerLng = xToLng(ncx, wsCur)
                    centerLat = yToLat(ncy, wsCur)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onBackgroundTap() })
            }
    ) {
        // — tile qatı —
        val tileN = (1 shl zoom)
        val x0 = ((cxW - halfW) / TILE_SIZE).toInt()
        val x1 = ((cxW + halfW) / TILE_SIZE).toInt()
        val y0 = max(0, ((cyW - halfH) / TILE_SIZE).toInt())
        val y1 = min(tileN - 1, ((cyW + halfH) / TILE_SIZE).toInt())
        for (tx in x0..x1) {
            for (ty in y0..y1) {
                val wx = ((tx % tileN) + tileN) % tileN
                // screen pos: viewport/2 + (worldPos − center)*k
                val left = viewport.width / 2f + (tx * TILE_SIZE - cxW) * k
                val top = viewport.height / 2f + (ty * TILE_SIZE - cyW) * k
                TileImage(
                    url = "https://tile.openstreetmap.org/$zoom/$wx/$ty.png",
                    left = left, top = top, sizePx = TILE_SIZE * k,
                )
            }
        }

        // — pin qatı (sabit ekran ölçüsü) —
        pins.forEach { pin ->
            val px = viewport.width / 2f + (lngToX(pin.lng, ws) - cxW) * k
            val py = viewport.height / 2f + (latToY(pin.lat, ws) - cyW) * k
            PinWidget(
                pin = pin,
                leftPx = px,
                topPx = py,
                onTap = { onPinTap(pin) },
            )
        }

        // — mənim yerim —
        val mx = viewport.width / 2f + (lngToX(myLng, ws) - cxW) * k
        val my = viewport.height / 2f + (latToY(myLat, ws) - cyW) * k
        Box(
            Modifier
                .offsetPx(mx.toInt() - 10, my.toInt() - 10)
                .size(20.dp)
                .background(Q.myLocation, CircleShape)
                .border(3.dp, Color.White, CircleShape)
        )

        // — zoom həbləri (şüşə, D-030) —
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 170.dp, end = 12.dp)
        ) {
            GlassPill(dense = true, horizontal = 7.dp, vertical = 7.dp) {
                Icon(Icons.Filled.Add, "Yaxınlaşdır", tint = Q.foreground,
                    modifier = Modifier.size(16.dp).androidxZoom {
                        zoomF = (zoomF + 1f).coerceIn(3f, 18f)
                    })
            }
            Spacer(Modifier.height(8.dp))
            GlassPill(dense = true, horizontal = 7.dp, vertical = 7.dp) {
                Icon(Icons.Filled.Remove, "Uzaqlaşdır", tint = Q.foreground,
                    modifier = Modifier.size(16.dp).androidxZoom {
                        zoomF = (zoomF - 1f).coerceIn(3f, 18f)
                    })
            }
        }

        // — OSM atribusiyası (istifadə qaydası — məcburi kredit) —
        Text(
            "© OpenStreetMap contributors",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 8.5.sp, color = Q.mutedFg),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 6.dp)
                .background(Q.card.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

/** Pixel dəqiqliyli offset (lambda versiyası px qəbul edir). */
private fun Modifier.offsetPx(x: Int, y: Int): Modifier =
    this.then(Modifier.offset { androidx.compose.ui.unit.IntOffset(x, y) })

private fun Modifier.androidxZoom(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

/** Xəritə pini — tipə görə rəng/ikon; 34–40px, ağ haşiyə. */
@Composable
private fun PinWidget(
    pin: MapPin,
    leftPx: Float,
    topPx: Float,
    onTap: () -> Unit,
) {
    val color = when (pin) {
        is MapPin.Venue -> Q.primary
        is MapPin.Event -> Q.accent
        is MapPin.PersonP -> Q.foreground
    }
    val icon = when (pin) {
        is MapPin.Venue -> Icons.Filled.Storefront
        is MapPin.Event -> Icons.Filled.Event
        is MapPin.PersonP -> null
    }
    val label = when (pin) {
        is MapPin.Venue -> pin.v.name
        is MapPin.Event -> pin.e.title
        is MapPin.PersonP -> pin.p.firstName
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offsetPx(leftPx.toInt() - 17, topPx.toInt() - 40)
            .androidxPinTap(onTap),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(color, CircleShape)
                .border(2.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, label, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(label.take(1), style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 13.sp,
                    fontWeight = FontWeight.W800, color = Color.White))
            }
        }
        Box(Modifier.width(2.5.dp).height(11.dp).background(color))
    }
}

private fun Modifier.androidxPinTap(onTap: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onTap))

/** OSM tile şəkli — URL-dən yüklə, fallback rəng (offline/test). */
@Composable
private fun TileImage(url: String, left: Float, top: Float, sizePx: Float) {
    val context = LocalContext.current
    val bitmap = remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val loaded = remember(url) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(url) {
        runCatching {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            // OSM tile policy: identifikasiya edən UA tələbi (D-030)
            conn.setRequestProperty(
                "User-Agent",
                "Qoshul.az/0.1.0 (beta; https://qoshul.az; native-rewrite)",
            )
            if (conn.responseCode == 200) {
                conn.inputStream.use { ins ->
                    bitmap.value = android.graphics.BitmapFactory.decodeStream(ins)
                }
            }
        }
        loaded.value = true
    }
    val density = LocalDensity.current
    val sizeDp = with(density) { sizePx.toDp() }
    val leftDp = with(density) { left.toDp() }
    val topDp = with(density) { top.toDp() }
    Box(
        Modifier
            .offset(leftDp, topDp)
            .size(sizeDp)
            .background(Q.tileFallback)
    ) {
        bitmap.value?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
