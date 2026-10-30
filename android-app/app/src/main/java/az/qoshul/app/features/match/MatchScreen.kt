package az.qoshul.app.features.match

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.glass.GlassPill
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.reportReasons
import az.qoshul.app.core.model.safetyTips
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.SafetyNote
import az.qoshul.app.core.ui.imagePaint
import kotlin.math.abs

/**
 * "Tanış ol" — Flutter match_screen.dart portu (ROL D): kart dəsti.
 * Dartılma (drag) kartı çevirir; 110px hədd — uçur (swipe) və ya yay geri.
 */
@Composable
fun MatchScreen(engine: AppEngine, nav: NavState) {
    var reportFor by remember { mutableStateOf<Person?>(null) }
    var safetyOpen by remember { mutableStateOf(false) }
    var matchDialog by remember { mutableStateOf<Person?>(null) }
    val st by engine.state.collectAsState()
    val candidates = engine.matchCandidates

    PushScreenScaffold(
        title = "Tanış ol",
        onBack = { nav.pop() },
        actions = {
            IconButton(onClick = { safetyOpen = true }) {
                Icon(Icons.Outlined.HealthAndSafety, "Təhlükəsizlik məsləhətləri", modifier = Modifier.size(20.dp))
            }
        },
    ) { mod ->
        if (candidates.isEmpty()) {
            Box(mod.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.FavoriteBorder,
                    text = if (st.swipes.isEmpty())
                        "Hazırda yeni kart yoxdur.\nYaxınlıqdakı insanlar burada görünəcək."
                    else "Bugünlük kartlar bitdi.\nYeni insanlar üçün sabah yenidən bax!",
                    action = if (st.swipes.isNotEmpty()) {
                        {
                            OutlinedButton(onClick = { engine.resetSwipes() }) {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Dəsti yenilə (demo)")
                            }
                        }
                    } else null,
                )
            }
        } else {
            Column(mod.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.weight(1f)) {
                    val deck = candidates.take(3)
                    // arxa kartlar — dərinlik
                    deck.drop(1).reversed().forEachIndexed { i, p ->
                        val depth = deck.size - 1 - i
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = -12f * depth
                                    scaleX = 1f - 0.05f * depth
                                    scaleY = 1f - 0.05f * depth
                                }
                        ) {
                            CardFace(
                                person = p, engine = engine, showActions = false,
                                onReport = { reportFor = p },
                                onPass = {}, onInterested = {},
                            )
                        }
                    }
                    // üst kart — dartılabilir
                    val top = deck.first()
                    SwipeCard(
                        person = top,
                        engine = engine,
                        onSwiped = { interested ->
                            val matched = engine.swipe(top.id, interested)
                            if (matched) matchDialog = top
                        },
                        onReport = { reportFor = top },
                    )
                }
                if (candidates.size > 3) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "...və daha ${candidates.size - 3} insan növbəti kartlarda",
                        textAlign = TextAlign.Center, style = T.meta,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                SafetyNote()
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // — match dialoqu —
    matchDialog?.let { p ->
        AlertDialog(
            onDismissRequest = { matchDialog = null },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    az.qoshul.app.core.motion.PopIn {
                        Box(
                            Modifier.size(68.dp).background(Q.secondary, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Favorite, null, tint = Q.primary, modifier = Modifier.size(32.dp)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Match!", style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 20.sp, fontWeight = FontWeight.W800))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${p.fullName} də sənə maraq göstərdi. Söhbəti başla " +
                            "(+10 XP, gündə max 3).",
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp, lineHeight = 21.sp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { matchDialog = null; nav.pop() }) { Text("Söhbətə keç") }
            },
            dismissButton = {
                TextButton(onClick = { matchDialog = null }) { Text("Davam et") }
            },
        )
    }

    // — təhlükəsizlik dialoqu —
    if (safetyOpen) {
        AlertDialog(
            onDismissRequest = { safetyOpen = false },
            title = { Text("İlk görüş məsləhətləri") },
            text = {
                Column {
                    safetyTips.forEach { (title, text) ->
                        Column(Modifier.padding(bottom = 10.dp)) {
                            Text(title, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700))
                            Spacer(Modifier.height(3.dp))
                            Text(text, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.5.sp,
                                lineHeight = 20.sp, color = Q.mutedFg))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { safetyOpen = false }) { Text("Bağla") }
            },
        )
    }

    // — report sheet —
    reportFor?.let { p ->
        var reason by remember { mutableStateOf(reportReasons.first()) }
        AlertDialog(
            onDismissRequest = { reportFor = null },
            title = { Text("Report et") },
            text = {
                Column {
                    Text("${p.fullName} haqqında bildiriş. Səbəb seç:", style = T.subtitle)
                    Spacer(Modifier.height(8.dp))
                    reportReasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = reason == r, onClick = { reason = r },
                                colors = RadioButtonDefaults.colors(selectedColor = Q.primary),
                            )
                            Text(r, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.sp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    engine.reportUser(p.id, reason)
                    reportFor = null
                }) { Text("Göndər") }
            },
            dismissButton = { TextButton(onClick = { reportFor = null }) { Text("Ləğv et") } },
        )
    }
}

/** Dartılabilir üst kart — swipe fizikası (D-025). */
@Composable
private fun SwipeCard(
    person: Person,
    engine: AppEngine,
    onSwiped: (Boolean) -> Unit,
    onReport: () -> Unit,
) {
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val threshold = 300f // dp boşluğunda təxmini px həddi

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(person.id) {
                detectDragGestures(
                    onDragEnd = {
                        if (abs(dragX) > threshold) {
                            onSwiped(dragX > 0)
                        }
                        dragX = 0f; dragY = 0f
                    },
                    onDragCancel = { dragX = 0f; dragY = 0f },
                ) { change, drag ->
                    change.consume()
                    dragX += drag.x
                    dragY += drag.y
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = dragX
                    translationY = dragY
                    rotationZ = (dragX / 22f).coerceIn(-12f, 12f)
                }
        ) {
            CardFace(
                person = person, engine = engine, showActions = true,
                onReport = onReport, onPass = { onSwiped(false) }, onInterested = { onSwiped(true) },
                dragX = dragX, threshold = threshold,
            )
        }
    }
}

/** Kart üzü — şəkil + məlumat + şüşə fəaliyyət həbləri (D-025 səth #5). */
@Composable
private fun CardFace(
    person: Person,
    engine: AppEngine,
    showActions: Boolean,
    onReport: () -> Unit,
    onPass: () -> Unit,
    onInterested: () -> Unit,
    dragX: Float = 0f,
    threshold: Float = 300f,
) {
    val st by engine.state.collectAsState()
    val shared = person.rolInterests.filter { it in st.interests }
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .androidxMatchBg()
    ) {
        Image(
            painter = imagePaint(person.image), contentDescription = person.fullName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // kölgə
        Box(
            Modifier
                .fillMaxSize()
                .androidxMatchShade(),
        )
        // uyğunluq çipi
        GlassPill(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            dense = true, horizontal = 8.dp, vertical = 5.dp,
        ) {
            Text("${person.match}% uyğunluq",
                style = T.glassText(size = 9, w = FontWeight.W600, color = Color.White))
        }
        // report/blok menyu
        Box(Modifier.align(Alignment.TopStart).padding(4.dp)) {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, "Menyu", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Report et") },
                    onClick = { menuOpen = false; onReport() })
                DropdownMenuItem(text = { Text("Blokla") },
                    onClick = {
                        menuOpen = false
                        engine.blockUser(person.id)
                    })
            }
        }
        // — swipe möhürləri —
        if (dragX > 12f) {
            Text(
                "MARAQLIYAM",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 13.sp,
                    fontWeight = FontWeight.W800, color = Q.primary),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 34.dp)
                    .graphicsLayer { rotationZ = -10f }
                    .androidxStampBorder(Q.primary)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        } else if (dragX < -12f) {
            Text(
                "KEÇ",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 13.sp,
                    fontWeight = FontWeight.W800, color = Q.destructive),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 18.dp, top = 34.dp)
                    .graphicsLayer { rotationZ = 10f }
                    .androidxStampBorder(Q.destructive)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        // — məlumat + fəaliyyətlər —
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Text("${person.firstName}, ${person.age}",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 19.sp,
                    fontWeight = FontWeight.W800, color = Color.White))
            Spacer(Modifier.height(3.dp))
            Row {
                Icon(Icons.Filled.LocationOn, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    (if (st.showDistance) "${person.distanceLabel} · ${person.city}" else person.city) +
                        "    ${FakeQoshulDataSource.statusLabel(person.status)} · ${person.org}",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.7f)),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (person.rolInterests.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row {
                    person.rolInterests.take(4).forEach { i ->
                        val isShared = i in shared
                        Text(
                            i,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 10.5.sp,
                                fontWeight = if (isShared) FontWeight.W700 else FontWeight.W500,
                                color = Color.White),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .androidxInterestChip(isShared)
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Icon(Icons.Filled.AutoAwesome, null,
                    tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text(person.reason, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 11.5.sp,
                        lineHeight = 17.sp, color = Color.White.copy(alpha = 0.7f)))
            }
            if (showActions) {
                Spacer(Modifier.height(12.dp))
                Row {
                    GlassPill(modifier = Modifier.weight(1f), horizontal = 12.dp, vertical = 11.dp) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().androidxPassTap(onPass),
                        ) {
                            Icon(Icons.Filled.Close, null, tint = Q.foreground, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Keç", style = T.glassText(size = 12))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    GlassPill(modifier = Modifier.weight(2f), horizontal = 12.dp, vertical = 11.dp) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().androidxPassTap(onInterested),
                        ) {
                            Icon(Icons.Filled.Favorite, null, tint = Q.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Maraqlıyam", style = T.glassText(size = 12))
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.androidxMatchBg(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rXl.dp))
        .androidxMatchBorder()
)

private fun Modifier.androidxMatchBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rXl.dp))
)

private fun Modifier.androidxMatchShade(): Modifier = this.then(
    Modifier.background(
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.38f to Color.Transparent,
            1.0f to Q.photoShade.copy(alpha = 0.92f),
        )
    )
)

private fun Modifier.androidxStampBorder(color: Color): Modifier = this.then(
    Modifier.androidxStampBorderReal(color)
)

private fun Modifier.androidxStampBorderReal(color: Color): Modifier = this.then(
    Modifier.border(2.5.dp, color, RoundedCornerShape(6.dp))
)

private fun Modifier.androidxInterestChip(shared: Boolean): Modifier = this.then(
    Modifier
        .background(
            if (shared) Q.primary.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.14f),
            RoundedCornerShape(5.dp),
        )
        .androidxInterestBorder(shared)
)

private fun Modifier.androidxInterestBorder(shared: Boolean): Modifier = this.then(
    Modifier.border(
        1.dp,
        if (shared) Q.primary.copy(alpha = 0.65f) else Q.glassBorder,
        RoundedCornerShape(5.dp),
    )
)

private fun Modifier.androidxPassTap(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
