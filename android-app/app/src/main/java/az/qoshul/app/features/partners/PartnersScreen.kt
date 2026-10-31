package az.qoshul.app.features.partners

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.VenueXpAccount
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.motion.countUpAsState
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.SheetScaffold
import az.qoshul.app.core.ui.ViewHeading
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.auth.QoshulTextField
import az.qoshul.app.features.events.CreateEventSheet

/**
 * Tərəfdaşlar — Flutter partners_screen.dart portu + Biznes rejimi
 * (business_mode_sheet.dart, D-026 L.4 işçi tərəfi: skan → balans →
 * hesab → endirim → receipt).
 */
@Composable
fun PartnersScreen(engine: AppEngine, nav: NavState, onOpenCreateAtVenue: (az.qoshul.app.core.model.Partner) -> Unit) {
    var businessOpen by remember { mutableStateOf(false) }
    var partnerInfoOpen by remember { mutableStateOf(false) }
    val firstVenue = remember { engine.partners.firstOrNull() }

    PushScreenScaffold(title = "Tərəfdaşlar", onBack = { nav.pop() }) { mod ->
        LazyColumn(
            modifier = mod.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
            ),
        ) {
            item {
                ViewHeading(title = "Sevdiyin məkanlar",
                    subtitle = "Görüşlərə ev sahibliyi edən tərəfdaşlarımız.")
            }
            item {
                // D-055: ölü banner → info dialoqu (tərəfdaşlıq prosesi)
                CommunityBanner(
                    icon = Icons.Filled.Storefront,
                    title = "Öz məkanını əlavə et",
                    subtitle = "Biznes kimi qeydiyyatdan keç, yeni müştərilər qazan.",
                    onClick = { partnerInfoOpen = true },
                    trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                )
            }
            item {
                CommunityBanner(
                    icon = Icons.Filled.QrCodeScanner,
                    title = "Biznes rejimi — XP endirimi ver",
                    subtitle = "İşçi modu: müştəri XP kodunu oxu, hesabı yaz, endirim ver.",
                    onClick = { businessOpen = true },
                    trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                )
            }
            // D-055: saxta "+150 XP" iddiası (heç bir XP qaydasına uyğun deyildi)
            // əvəzinə real hərəkət: tədbir yaratma sheeti partnyor məkanla açılır
            if (firstVenue != null) {
                item {
                    CommunityBanner(
                        icon = Icons.Filled.Handshake,
                        title = "Görüş təşkil et — XP qazan",
                        subtitle = "Tərəfdaş məkanda tədbir yarat: qoşulma +10, check-in +40 XP.",
                        onClick = { onOpenCreateAtVenue(firstVenue) },
                        trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                    )
                }
            }
            itemsIndexed(engine.partners) { _, p ->
                PartnerCard(p, onCreate = { onOpenCreateAtVenue(p) })
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (businessOpen) {
        BusinessModeSheet(engine, onDismiss = { businessOpen = false })
    }

    // D-055: tərəfdaşlıq info dialoqu (əvvəl banner ölü idi)
    if (partnerInfoOpen) {
        AlertDialog(
            onDismissRequest = { partnerInfoOpen = false },
            title = { Text("Öz məkanını əlavə et") },
            text = {
                Text(
                    "Restoran, kafe və ya idman mərkəzi işlədirsən? Qoshul.az tərəfdaşı " +
                        "ol — tədbirlər sənin məkanda keçirilsin, müştərilərin XP endirimi ilə " +
                        "geri qayıtsın.\n\nMüraciət üçün: biznes@qoshul.az (demo). Bu demoda " +
                        "Biznes rejimi ilə XP endirim axınını sınaya bilərsən.",
                    style = T.subtitle.copy(lineHeight = 21.sp),
                )
            },
            confirmButton = {
                TextButton(onClick = { partnerInfoOpen = false }) { Text("Anladım", style = T.button) }
            },
        )
    }
}

@Composable
private fun PartnerCard(p: az.qoshul.app.core.model.Partner, onCreate: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .androidxPartnerCard()
    ) {
        Image(
            painter = imagePaint(p.image), contentDescription = p.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storefront, null, tint = Q.primary, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(p.category, style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 10.5.sp,
                    fontWeight = FontWeight.W700, color = Q.primary))
            }
            Spacer(Modifier.height(8.dp))
            Text(p.name, style = T.h2)
            Spacer(Modifier.height(8.dp))
            Text("${p.address}, ${p.city}", style = T.meta)
            if (p.deal != null) {
                Spacer(Modifier.height(15.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .androidxDealBg()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(p.deal!!, style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 12.5.sp,
                        fontWeight = FontWeight.W800, color = Q.primary))
                    Text(p.hours, style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 12.5.sp, color = Q.primary))
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rPill),
                colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Burada tədbir yarat", style = T.button)
            }
        }
    }
}

// ═══════════════════ Biznes rejimi (D-026 L.4) ═══════════════════

/**
 * İşçi tərəfi: obyekt seç → müştəri QR oxu → balans → hesab+XP →
 * canlı endirim önizləməsi → atomik düşüm → receipt.
 */
@Composable
fun BusinessModeSheet(engine: AppEngine, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var venueId by remember { mutableStateOf("v5") }
    var scanFailed by remember { mutableStateOf(false) }
    var bill by remember { mutableStateOf("") }
    var xpSpend by remember { mutableStateOf("100") }
    val st by engine.state.collectAsState()
    val acct = engine.venueXpFor(venueId)

    SheetScaffold(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Biznes rejimi — XP endirimi", style = T.h2.copy(fontSize = 16.sp))
            Spacer(Modifier.height(4.dp))
            Text("Müştərinin dinamik XP kodunu oxu, hesabı yaz — endirim avtomatik hesablanır.",
                style = T.meta.copy(fontSize = 11.5.sp))
            Spacer(Modifier.height(16.dp))
            if (acct == null) {
                Text("Obyekt tapılmadı.", style = T.subtitle)
            } else when (step) {
                0 -> {
                    // — obyekt seç + skan —
                    Text("Obyekt", style = T.meta.copy(fontWeight = FontWeight.W700))
                    Spacer(Modifier.height(8.dp))
                    Column {
                        engine.partners.filter { engine.venueXpFor(it.id) != null }
                            .forEach { v ->
                                val sel = venueId == v.id
                                Text(
                                    v.name,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = JakartaSans, fontSize = 12.5.sp,
                                        fontWeight = if (sel) FontWeight.W700 else FontWeight.W400,
                                        color = if (sel) Q.primary else Q.foreground),
                                    modifier = Modifier
                                        .padding(end = 8.dp, bottom = 8.dp)
                                        .androidxVenueChip(sel) { venueId = v.id; scanFailed = false }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                    }
                    Spacer(Modifier.height(18.dp))
                    if (scanFailed) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .androidxScanErrBg()
                                .padding(10.dp)
                        ) {
                            Row {
                                Icon(Icons.Filled.ErrorOutline, null, tint = Q.destructive, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Kod oxunmadı: müştərinin XP kartı bu obyekt üçün etibarlı " +
                                        "deyil və ya vaxtı bitib. Müştəçi \"Profil → XP kartım\"-dan " +
                                        "bu obyekt üçün yeni kod almalıdır (60 saniyəlik, tək istifadəlik).",
                                    style = T.meta.copy(fontSize = 11.5.sp, color = Q.destructive),
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (engine.xpCardValidFor(venueId)) {
                                scanFailed = false; step = 1
                            } else scanFailed = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(Q.rPill),
                        colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Müştəri XP kodunu oxu", style = T.button)
                    }
                }
                1 -> {
                    // — balans —
                    PopIn {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .androidxBalanceBg()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(36.dp).background(Q.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Person, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${st.profileName} (demo müştəri)",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = JakartaSans, fontSize = 13.sp,
                                        fontWeight = FontWeight.W800))
                                Text("Kod təsdiqləndi · ${acct.venueName} bazası",
                                    style = T.meta.copy(fontSize = 11.5.sp))
                            }
                            val bal = countUpAsState(acct.balance)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$bal XP", style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 17.sp,
                                    fontWeight = FontWeight.W800, color = Q.primary))
                                Text("balans", style = T.meta.copy(fontSize = 10.5.sp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Məzənnə: ${acct.config.describe()} · Endirim hesabın maks " +
                            "${acct.config.maxDiscountPercent}%-i qədər ola bilər.",
                        style = T.meta.copy(fontSize = 11.5.sp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(Q.rPill),
                        colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                    ) {
                        Icon(Icons.Filled.ReceiptLong, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hesabı yaz", style = T.button)
                    }
                }
                2 -> {
                    // — hesab + XP → canlı önizləmə —
                    val billD = bill.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val xpI = xpSpend.toIntOrNull() ?: 0
                    val p = engine.previewSpend(venueId, xpI, billD)
                    val errText = when (p.error) {
                        "balans" -> "Balansda kifayət qədər XP yoxdur."
                        "minBill" -> "Hesab minimum ${acct.config.minBillAzn} ₼ olmalıdır (obyekt qaydası)."
                        "cap" -> "Endirim hesabın ${acct.config.maxDiscountPercent}%-i həddini aşır — " +
                            String.format("%.2f", p.discount) + " ₼-dən çox ola bilməz. Daha az XP yaz."
                        "hesab" -> "Məbləğ və XP düzgün daxil edilməyib."
                        else -> ""
                    }
                    QoshulTextField(value = bill, onValueChange = { bill = it }, hint = "48.00")
                    Spacer(Modifier.height(4.dp))
                    Text("Hesab məbləği (₼)", style = T.label)
                    Spacer(Modifier.height(10.dp))
                    QoshulTextField(value = xpSpend, onValueChange = { xpSpend = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.height(4.dp))
                    Text("Xərclənəcək XP", style = T.label)
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .androidxPreviewBg(p.ok)
                            .padding(13.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Endirim", style = T.meta.copy(fontSize = 12.5.sp, fontWeight = FontWeight.W700))
                            Text(
                                if (p.ok) "− ${String.format("%.2f", p.discount)} ₼" else "—",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 15.sp, fontWeight = FontWeight.W800,
                                    color = if (p.ok) Q.primary else Q.destructive),
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Müştəri ödəyir", style = T.meta.copy(fontSize = 12.5.sp))
                            Text(
                                if (p.ok) "${String.format("%.2f", billD - p.discount)} ₼" else "—",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700),
                            )
                        }
                        if (!p.ok && errText.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(errText, style = T.meta.copy(fontSize = 11.5.sp, color = Q.destructive))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row {
                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(Q.rPill),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Q.card, contentColor = Q.foreground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                        ) { Text("Geri", style = T.button) }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val d = engine.spendVenueXp(venueId, xpI, billD)
                                if (d != null) {
                                    engine.markXpCardUsed()
                                    step = 3
                                }
                            },
                            enabled = p.ok,
                            modifier = Modifier.weight(2f).height(52.dp),
                            shape = RoundedCornerShape(Q.rPill),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Q.primary, contentColor = Q.primaryFg,
                                disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                        ) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Təsdiqlə və endirim ver", style = T.button)
                        }
                    }
                }
                else -> {
                    // — receipt —
                    val last = acct.recentLedger.first()
                    PopIn {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(62.dp).background(Q.primary, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CheckCircle, null,
                                    tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(34.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("Endirim tətbiq olundu", style = T.h2.copy(fontSize = 15.sp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${acct.venueName} bazasından ${last.amount} XP xərcləndi. " +
                                    "Hər iki tərəfə receipt göndərildi — əməliyyat ledger-də izlənilir.",
                                style = T.meta.copy(fontSize = 11.5.sp), textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            ReceiptRow("İştirak edən XP", "${last.amount} XP")
                            ReceiptRow("Qalıq balans", "${last.balanceAfter} XP")
                            ReceiptRow("Ledger qeydi", last.id)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(Q.rPill),
                                colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                            ) { Text("Bitir", style = T.button) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReceiptRow(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, style = T.meta.copy(fontSize = 12.5.sp))
        Text(v, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.5.sp, fontWeight = FontWeight.W700))
    }
}

private fun Modifier.androidxPartnerCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxPartnerBorder()
)

private fun Modifier.androidxPartnerBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxDealBg(): Modifier = this.then(
    Modifier.background(Q.secondary, RoundedCornerShape(5.dp))
)

private fun Modifier.androidxVenueChip(sel: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .androidxChipClick(onClick)
        .background(if (sel) Q.secondary else Q.card, RoundedCornerShape(Q.rMd.dp))
        .androidxVenueChipBorder(sel)
)

private fun Modifier.androidxChipClick(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

private fun Modifier.androidxVenueChipBorder(sel: Boolean): Modifier = this.then(
    Modifier.border(
        1.4.dp.takeIf { sel } ?: 1.dp,
        if (sel) Q.primary else Q.border,
        RoundedCornerShape(Q.rMd.dp),
    )
)

private fun Modifier.androidxScanErrBg(): Modifier = this.then(
    Modifier
        .background(Q.destructive.copy(alpha = 0.08f), RoundedCornerShape(Q.rMd.dp))
        .androidxScanErrBorder()
)

private fun Modifier.androidxScanErrBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.destructive.copy(alpha = 0.4f), RoundedCornerShape(Q.rMd.dp))
)

private fun Modifier.androidxBalanceBg(): Modifier = this.then(
    Modifier
        .background(Q.secondary, RoundedCornerShape(Q.rLg.dp))
        .androidxBalanceBorder()
)

private fun Modifier.androidxBalanceBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxPreviewBg(ok: Boolean): Modifier = this.then(
    Modifier
        .background(
            if (ok) Q.secondary else Q.destructive.copy(alpha = 0.07f),
            RoundedCornerShape(Q.rMd.dp),
        )
        .androidxPreviewBorder(ok)
)

private fun Modifier.androidxPreviewBorder(ok: Boolean): Modifier = this.then(
    Modifier.border(
        1.dp,
        if (ok) Q.border else Q.destructive.copy(alpha = 0.4f),
        RoundedCornerShape(Q.rMd.dp),
    )
)
