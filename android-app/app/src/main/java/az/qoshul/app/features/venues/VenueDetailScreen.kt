package az.qoshul.app.features.venues

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.WhereToVote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.CategoryChip
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.QrCode
import az.qoshul.app.core.ui.RatingStars
import az.qoshul.app.core.ui.ResvStatusChip
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.SectionLink
import az.qoshul.app.core.ui.SheetScaffold
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.auth.QoshulTextField
import kotlinx.coroutines.launch

/**
 * Məkan detal + Rezerv axını — Flutter venue_detail_screen.dart +
 * reserve_sheet.dart portu (ROL E). Onlayn ödəniş YOXDUR.
 */
@Composable
fun VenueDetailScreen(engine: AppEngine, nav: NavState, venueId: String) {
    val st by engine.state.collectAsState()
    val venue = FakeQoshulDataSource.partners.firstOrNull { it.id == venueId } ?: return
    var reserveOpen by remember { mutableStateOf(false) }
    val hour = remember { java.time.LocalDateTime.now().hour }
    val openNow = venue.isOpenAt(hour)
    val reviews = FakeQoshulDataSource.mockVenueReviews[venue.id] ?: emptyList()

    Column(Modifier.fillMaxSize().background(Q.background)) {
        // — şəkil başlıq (SliverAppBar portu) —
        Box(Modifier.fillMaxWidth().height(230.dp)) {
            Image(
                painter = imagePaint(venue.image), contentDescription = venue.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().androidxVenueShade(),
            )
            IconButton(
                onClick = { nav.pop() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(40.dp)
                    .background(Q.glass, CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Geri", tint = Color.White)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(venue.name, style = T.h1.copy(fontSize = 22.sp), modifier = Modifier.weight(1f))
                if (venue.hasDiscount) {
                    Text(
                        "-${venue.discountPercent}% endirim",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp,
                            fontWeight = FontWeight.W800, color = Q.accentFg),
                        modifier = Modifier
                            .background(Q.rankBadgeBg, RoundedCornerShape(Q.rMd.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingStars(rating = venue.rating, size = 15.dp)
                Spacer(Modifier.width(6.dp))
                Text(String.format("%.1f (%d rəy)", venue.rating, venue.reviewsCount), style = T.meta)
                Spacer(Modifier.width(10.dp))
                Text("${venue.category} · ${venue.priceRange}", style = T.meta)
            }
            Spacer(Modifier.height(14.dp))
            // info kart
            Column(
                Modifier
                    .fillMaxWidth()
                    .androidxInfoCard()
                    .padding(14.dp)
            ) {
                InfoRow(Icons.Filled.Schedule, "${venue.hours} · " +
                    (if (openNow) "İndi açıqdır" else "Bağlıdır"),
                    color = if (openNow) Q.primary else Q.mutedFg)
                androidx.compose.material3.HorizontalDivider(color = Q.border, modifier = Modifier.padding(vertical = 7.dp))
                InfoRow(Icons.Filled.Payments, "Qiymət aralığı: ${venue.priceRange}")
                androidx.compose.material3.HorizontalDivider(color = Q.border, modifier = Modifier.padding(vertical = 7.dp))
                InfoRow(Icons.Filled.TableRestaurant,
                    "Rezerv: " + (if (venue.autoAccept) "avtomatik təsdiq" else "partnyor təsdiqi ilə"))
            }
            Spacer(Modifier.height(14.dp))
            if (venue.deal != null) {
                CommunityBanner(
                    icon = Icons.Filled.Percent,
                    title = "Aktiv təklif: ${venue.deal}",
                    subtitle = if (venue.autoAccept) "Rezerv avtomatik təsdiqlənir."
                    else "Rezerv partnyorun təsdiqini gözləyir (GÖZLƏYİR).",
                )
            }
            SectionHeading(title = "Rəylər", icon = Icons.Filled.RateReview)
            if (reviews.isEmpty()) {
                EmptyState(icon = Icons.Filled.RateReview, text = "Hələ rəy yazılmayıb.")
            } else {
                reviews.forEach { r ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .androidxReviewCard()
                            .padding(12.dp)
                    ) {
                        Row {
                            Text(r.author, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700))
                            Spacer(Modifier.weight(1f))
                            RatingStars(rating = r.stars.toDouble(), size = 11.dp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(r.text, style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp,
                            lineHeight = 20.sp, color = Q.mutedFg))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                // A3: "Yol tap" — xəritə fazasında
                OutlinedButton(
                    onClick = {}, enabled = false,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContainerColor = Q.card, disabledContentColor = Q.mutedFg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                ) {
                    Icon(Icons.Filled.NearMe, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yol tap", style = T.button)
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { reserveOpen = true },
                    modifier = Modifier.weight(2f).height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                ) {
                    Icon(Icons.Filled.TableRestaurant, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rezerv et", style = T.button)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Onlayn ödəniş yoxdur — rezerv yalnız yer ayırmadır. (Depozit məsələsi təsdiq gözləyir)",
                textAlign = TextAlign.Center, style = T.meta,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }

    if (reserveOpen) {
        ReserveSheet(engine, venue, onDismiss = { reserveOpen = false })
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String,
                    color: Color = Q.foreground) {
    Row {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.sp, color = color))
    }
}

/** Rezerv axını: tarix → saat → nəfər → qeyd+kupon → təsdiq → status+QR. */
@Composable
fun ReserveSheet(engine: AppEngine, venue: Partner, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var date by remember { mutableStateOf<String?>(null) }
    var time by remember { mutableStateOf<String?>(null) }
    var party by remember { mutableIntStateOf(2) }
    var note by remember { mutableStateOf("") }
    var couponCode by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<az.qoshul.app.core.model.Reservation?>(null) }
    val st by engine.state.collectAsState()
    val dates = remember { FakeQoshulDataSource.nextDateLabels() }
    val slots = FakeQoshulDataSource.venueSlotTimes[venue.id] ?: emptyList()
    val myCoupons = engine.activeCouponsFor(venue.id)
    val canConfirm = date != null && time != null

    SheetScaffold(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Rezerv — ${venue.name}", style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 18.sp, fontWeight = FontWeight.W800))
            Spacer(Modifier.height(4.dp))
            Text(
                if (step == 4) "Rezerv yaradıldı."
                else "Addım ${step + 1}/4 — " +
                    listOf("tarix", "saat", "nəfər sayı", "qeyd və kupon")[step],
                style = T.subtitle,
            )
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> {
                    SectionTitle("Tarix seç")
                    Spacer(Modifier.height(10.dp))
                    Column {
                        dates.chunked(4).forEach { row ->
                            Row {
                                row.forEach { d ->
                                    Box(Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                                        CategoryChip(label = d, icon = Icons.Filled.CalendarToday,
                                            active = date == d) {
                                            date = d; step = 1
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    SectionTitle("Saat slotu seç")
                    Spacer(Modifier.height(10.dp))
                    Column {
                        slots.chunked(4).forEach { row ->
                            Row {
                                row.forEach { s ->
                                    Box(Modifier.padding(end = 8.dp, bottom = 8.dp)) {
                                        CategoryChip(label = s, icon = Icons.Filled.Schedule,
                                            active = time == s) {
                                            time = s; step = 2
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionLink(text = "← tarixə qayıt", onClick = { step = 0 })
                }
                2 -> {
                    SectionTitle("Nəfər sayı")
                    Spacer(Modifier.height(4.dp))
                    Text("Məkanın tutumuna görə (maks. 8 nəfər)", style = T.meta)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = { if (party > 1) party-- }) {
                            Icon(Icons.Filled.Remove, null)
                        }
                        Text("$party nəfər", style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 18.sp, fontWeight = FontWeight.W800),
                            modifier = Modifier.width(76.dp), textAlign = TextAlign.Center)
                        IconButton(onClick = { if (party < 8) party++ }) {
                            Icon(Icons.Filled.Check, null)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { step = 3 },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                    ) { Text("Davam et", style = T.button) }
                    Spacer(Modifier.height(12.dp))
                    SectionLink(text = "← saata qayıt", onClick = { step = 1 })
                }
                3 -> {
                    Text("Qeyd (isteğe bağlı)", style = T.label)
                    Spacer(Modifier.height(7.dp))
                    QoshulTextField(value = note,
                        onValueChange = { if (it.length <= 200) note = it },
                        maxLines = 2, hint = "Ad günü, pəncərə kənarı və s.")
                    Spacer(Modifier.height(6.dp))
                    SectionTitle("Kupon seç")
                    Spacer(Modifier.height(10.dp))
                    if (myCoupons.isEmpty()) {
                        Text(
                            "Bu məkan üçün aktiv kuponun yoxdur. " +
                                "\"Endirimlər\" bölməsindən XP ilə ala bilərsən.",
                            style = T.subtitle,
                        )
                    } else {
                        CouponTile(null, "Kuponsuz rezerv", couponCode == null) { couponCode = null }
                        myCoupons.forEach { c ->
                            CouponTile(c.code, "${c.label} · ${c.code}", couponCode == c.code) {
                                couponCode = c.code
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val r = engine.createReservation(
                                venue = venue, date = date!!, time = time!!, party = party,
                                note = note.trim(), couponCode = couponCode,
                            )
                            if (r == null) {
                                onDismiss()
                            } else {
                                result = r; step = 4
                            }
                        },
                        enabled = canConfirm,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Q.primary, contentColor = Q.primaryFg,
                            disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                    ) { Text("Təsdiqlə", style = T.button) }
                    Spacer(Modifier.height(12.dp))
                    SectionLink(text = "← nəfər sayına qayıt", onClick = { step = 2 })
                }
                else -> {
                    // — nəticə: status + QR —
                    result?.let { r ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            ResvStatusChip(status = r.status)
                            Spacer(Modifier.height(14.dp))
                            PopIn { QrCode(payload = r.qrCode) }
                            Spacer(Modifier.height(8.dp))
                            Text(r.qrCode, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.sp,
                                fontWeight = FontWeight.W700))
                        }
                        Spacer(Modifier.height(14.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .androidxResultCard()
                                .padding(12.dp)
                        ) {
                            FactRow("${r.venueName} · ${r.date}, ${r.time}")
                            FactRow("${r.party} nəfər" +
                                (r.couponCode?.let { " · kupon: $it" } ?: ""))
                            FactRow(
                                if (r.status == az.qoshul.app.core.model.ResvStatus.CONFIRMED)
                                    "Avtomatik təsdiqləndi — TƏSDİQLƏNDİ"
                                else "Partnyor təsdiqi gözlənilir — GÖZLƏYİR",
                            )
                            FactRow("Xatırlatmalar: 24 saat və 2 saat əvvəl bildiriş")
                            FactRow("Məkanda QR-i göstər — partnyor skan edəndə +${XpRules.RESERVATION_XP} XP")
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(Q.rMd.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                        ) { Text("Hazırdır", style = T.button) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = androidx.compose.ui.text.TextStyle(
        fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W700))
}

@Composable
private fun FactRow(text: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Icon(Icons.Filled.Check, null, tint = Q.primary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.5.sp, lineHeight = 20.sp))
    }
}

@Composable
private fun CouponTile(code: String?, label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .androidxCouponTile(active, onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(
            if (active) Icons.Filled.CheckCircle else Icons.Filled.Percent, null,
            tint = if (active) Q.primary else Q.mutedFg, modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.sp))
    }
}

private fun Modifier.androidxVenueShade(): Modifier = this.then(
    Modifier.background(
        androidx.compose.ui.graphics.Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.6f to Color.Transparent,
            1.0f to Q.photoShade.copy(alpha = 0.6f),
        )
    )
)

private fun Modifier.androidxInfoCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxInfoBorder()
)

private fun Modifier.androidxInfoBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxReviewCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxReviewBorder()
)

private fun Modifier.androidxReviewBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxResultCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxResultBorder()
)

private fun Modifier.androidxResultBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxCouponTile(active: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .clickable(onClick = onClick)
        .background(if (active) Q.secondary else Q.card, RoundedCornerShape(Q.rMd.dp))
        .androidxCouponBorder(active)
)

private fun Modifier.androidxCouponBorder(active: Boolean): Modifier = this.then(
    Modifier.border(
        1.4.dp.takeIf { active } ?: 1.dp,
        if (active) Q.primary else Q.border,
        RoundedCornerShape(Q.rMd.dp),
    )
)
