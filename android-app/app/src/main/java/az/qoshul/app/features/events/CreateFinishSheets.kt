package az.qoshul.app.features.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.ActivityXpResult
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.model.LocalDate
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.AiBox
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.QrCode
import az.qoshul.app.core.ui.SheetScaffold
import az.qoshul.app.features.auth.QoshulTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Birlikdə yeni plan quraq" — Flutter create_event_sheet.dart portu.
 * [initial] — Assistant [TƏDBİR] təklifindən prefill.
 */
@Composable
fun CreateEventSheet(
    engine: AppEngine,
    onDismiss: () -> Unit,
    initial: az.qoshul.app.core.model.EventProposal? = null,
    presetCity: String = "",
    venue: az.qoshul.app.core.model.Partner? = null,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var idea by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var tags by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var city by remember {
        mutableStateOf(presetCity.ifEmpty { venue?.city ?: engine.state.value.city })
    }
    var category by remember {
        mutableStateOf(if (FakeQoshulDataSource.allCategories.contains(initial?.category)) initial!!.category else "Qəhvə")
    }
    var format by remember { mutableStateOf("Oflayn") }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var time by remember { mutableStateOf("18:00") }
    var capacity by remember { mutableStateOf(if ((initial?.capacity ?: 8) in 2..500) initial!!.capacity else 8) }
    var paid by remember { mutableStateOf(false) }
    var approval by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiLocation by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    fun aiFill() {
        scope.launch {
            aiBusy = true
            delay(1100)
            val i = idea.lowercase()
            if (i.contains("dağ") || i.contains("yürüş")) {
                title = "Şənbə səhəri dağa yürüş"; category = "Yürüş"
                desc = "Orta çətinlikdə marşrut, gözəl mənzərəli dayanacaqlar. Su və rahat ayaqqabı yanına al."
            } else if (i.contains("qəhvə") || i.contains("çay")) {
                title = "Qəhvə və tanışlıq səhəri"; category = "Qəhvə"
                desc = "Səhər qəhvəsi ətrafında asan söhbət. Yeni insanlarla tanış ol, həftəni planlaşdır."
            } else {
                title = "Şənbə üçün birgə plan"; category = "Stolüstü oyunlar"
                desc = "Maraqlı keçirmək istəyənlər üçün açıq dəvət. Oyunlar, söhbət, yaxşı əhval."
            }
            capacity = 10
            aiBusy = false
        }
    }

    fun submit() {
        engine.createEvent(
            title = title.trim(), category = category, city = city,
            location = aiLocation ?: location.trim(),
            date = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth),
            time = time, capacity = capacity, description = desc.trim(),
            epochDay = date.toEpochDay().toInt(),
            price = if (paid) price.toIntOrNull() ?: 0 else 0,
            format = format, approval = approval,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
        onDismiss()
    }

    SheetScaffold(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Birlikdə yeni plan quraq", style = T.h2)
            Spacer(Modifier.height(4.dp))
            Text("İstədiyin formada tədbir yarat və maraqlarını bölüşən insanlarla tanış ol.", style = T.subtitle)
            // — AI bloku —
            AiBox {
                Column {
                    Text("AI köməkçisi: fikrini qısa yaz",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp, fontWeight = FontWeight.W600))
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoFixHigh, null, tint = Q.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            QoshulTextField(value = idea, onValueChange = { idea = it },
                                hint = "Məsələn, şənbə səhəri dağa yürüş")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { aiFill() },
                        enabled = !aiBusy && idea.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Q.secondary, contentColor = Q.primary,
                            disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                    ) {
                        if (aiBusy) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Filled.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(if (aiBusy) "Hazırlanır..." else "Tədbiri AI ilə doldur", style = T.button)
                    }
                }
            }
            // — Forma —
            FormLine("Tədbirin adı") {
                QoshulTextField(value = title, onValueChange = { if (it.length <= 65) title = it },
                    hint = "Məsələn, şənbə günü qəhvə görüşü")
            }
            FormLine("Kateqoriya") {
                DropdownLine(
                    options = FakeQoshulDataSource.allCategories,
                    selected = category, onSelect = { category = it },
                )
            }
            Row {
                Box(Modifier.weight(1f)) {
                    FormLine("Format") {
                        DropdownLine(options = listOf("Oflayn", "Onlayn"), selected = format, onSelect = { format = it })
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    FormLine("Şəhər") {
                        DropdownLine(
                            options = FakeQoshulDataSource.cities.map { it.name },
                            selected = city, onSelect = { city = it },
                        )
                    }
                }
            }
            if (format == "Oflayn") {
                FormLine("Məkan və ünvan") {
                    QoshulTextField(value = location, onValueChange = { location = it },
                        hint = "Məkanın adı və ünvanı")
                }
            }
            Row {
                Box(Modifier.weight(1f)) {
                    FormLine("Tarix") {
                        DropdownLine(
                            options = FakeQoshulDataSource.nextDateLabels(14),
                            selected = "%02d.%02d".format(date.dayOfMonth, date.monthValue),
                            onSelect = { sel ->
                                val idx = FakeQoshulDataSource.nextDateLabels(14).indexOf(sel)
                                if (idx >= 0) date = LocalDate.now().plusDays(idx.toLong())
                            },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    FormLine("Saat") {
                        DropdownLine(
                            options = (6..23).map { "%02d:00".format(it) } + listOf("18:30", "19:30", "20:30"),
                            selected = time, onSelect = { time = it },
                        )
                    }
                }
            }
            FormLine("İştirakçı sayı (2–500)") {
                QoshulTextField(value = capacity.toString(), onValueChange = {
                    it.toIntOrNull()?.let { n -> if (n in 2..500) capacity = n }
                })
            }
            FormLine("İştirak haqqı") {
                Row {
                    Box(Modifier.weight(1f)) {
                        DropdownLine(listOf("Pulsuz", "Ödənişli"), if (paid) "Ödənişli" else "Pulsuz") {
                            paid = it == "Ödənişli"
                        }
                    }
                    if (paid) {
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            FormLine("Qiymət (AZN)") {
                                QoshulTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() } })
                            }
                        }
                    } else {
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            FormLine("Yaş həddi") {
                                DropdownLine(
                                    listOf("Hamı üçün", "16+", "18+", "18–25", "25–35"),
                                    selected = "Hamı üçun", onSelect = { },
                                )
                            }
                        }
                    }
                }
            }
            FormLine("Teqlər (vergüllə)") {
                QoshulTextField(value = tags, onValueChange = { tags = it }, hint = "tələbələr, açıq hava, pulsuz")
            }
            Spacer(Modifier.height(12.dp))
            // approval checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .androidxApprovalToggle { approval = !approval }
                    .padding(vertical = 6.dp),
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .background(if (approval) Q.primary else Q.card, RoundedCornerShape(Q.rSm.dp))
                        .border(1.4.dp, if (approval) Q.primary else Q.border, RoundedCornerShape(Q.rSm.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (approval) Icon(Icons.Filled.CheckCircle, null, tint = Q.primaryFg, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Qoşulma istəklərini özüm təsdiqləyim",
                    style = androidx.compose.ui.text.TextStyle(fontFamily = JakartaSans, fontSize = 12.sp))
            }
            FormLine("Tədbir haqqında") {
                QoshulTextField(value = desc, onValueChange = { if (it.length <= 2000) desc = it },
                    maxLines = 4, hint = "Birlikdə nə edəcəyik?")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (title.isNotBlank() && desc.isNotBlank()) submit() },
                enabled = title.isNotBlank() && desc.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(Q.rMd.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tədbir yarat", style = T.button)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun Modifier.androidxApprovalToggle(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

/** Sadə forma sətri. */
@Composable
private fun FormLine(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 15.dp)) {
        Text(label, style = T.label)
        Spacer(Modifier.height(7.dp))
        content()
    }
}

/** Sadə dropdown (ExposedDropdownMenu əvəzinə scrollable seçim siyahısı sətri). */
@Composable
fun DropdownLine(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .androidxDropdownBg()
                .androidxDropdownClick { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 13.dp)
        ) {
            Text(selected.ifEmpty { "Seç" }, style = T.input)
        }
        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .androidxDropdownBg()
                    .heightInMax()
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).height(200.dp)) {
                    options.forEach { opt ->
                        Text(
                            opt,
                            style = T.input.copy(
                                color = if (opt == selected) Q.primary else Q.foreground,
                                fontWeight = if (opt == selected) FontWeight.W700 else FontWeight.W400,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .androidxDropdownClick {
                                    onSelect(opt)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.androidxDropdownBg(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rMd.dp))
        .border(1.dp, Q.border, RoundedCornerShape(Q.rMd.dp))
)

private fun Modifier.androidxDropdownClick(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

private fun Modifier.heightInMax(): Modifier = this

// ═══════════════════════════════════════════════════════════════════
// Bitirmə axını — Flutter finish_activity_sheet.dart portu (D-026 L.3)
// ═══════════════════════════════════════════════════════════════════

/**
 * Təşkilatçı bitirmə axını: statik QR → şəkil → taq → nəticə.
 * Təşkilatçı +50, hər taq edilən +25 Endirim XP.
 */
@Composable
fun FinishActivitySheet(
    engine: AppEngine,
    event: QoshulEvent,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    val tagged = remember { mutableStateOf(setOf<String>()) }
    val sharedPhotos = remember { mutableStateOf(setOf<Int>()) }
    var result by remember { mutableStateOf<ActivityXpResult?>(null) }
    val st by engine.state.collectAsState()
    val acct = engine.venueXpFor(event.venueId ?: "")
    val participants = FakeQoshulDataSource.people.take(event.participants.coerceIn(1, 5))

    SheetScaffold(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Aktiviteyi bitir — \"${event.title}\"",
                style = T.h2.copy(fontSize = 15.sp))
            Spacer(Modifier.height(4.dp))
            Text(
                "Obyektin statik QR-ı → şəkil paylaş → iştirakçıları taq et → " +
                    "Endirim XP paylanır (təşkilatçı +${XpRules.ORGANIZER_XP}, " +
                    "hər taq +${XpRules.TAGGED_PARTICIPANT_XP}).",
                style = T.meta.copy(fontSize = 11.5.sp),
            )
            Spacer(Modifier.height(16.dp))
            if (acct == null) {
                EmptyState(icon = Icons.Filled.WorkspacePremium,
                    text = "Bu tədbir partnyor obyektinə bağlı deyil.")
            } else when (step) {
                0 -> {
                    // — statik QR —
                    PopIn {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            QrCode(payload = "STATIC-${acct.venueId}-${acct.venueName}", size = 148.dp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${acct.venueName} · obyektin statik QR-ı.\n" +
                            "Kassada və ya çıxışda oxu — aktivite obyektlə əlaqələnir.",
                        style = T.meta.copy(fontSize = 11.5.sp),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("QR-ı oxudum", style = T.button)
                    }
                }
                1 -> {
                    // — şəkil paylaşımı —
                    Text("Şəkilləri paylaş",
                        style = T.meta.copy(fontWeight = FontWeight.W700, fontSize = 12.5.sp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Şəkli yalnız SƏN (təşkilatçı) paylaşa bilərsən — Aktivite şəkilləri " +
                            "obyektin biznes hesabına düşür (İnstagram-model).",
                        style = T.meta.copy(fontSize = 11.5.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row {
                        (0..2).forEach { i ->
                            val sel = i in sharedPhotos.value
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(end = if (i < 2) 10.dp else 0.dp)
                                    .height(92.dp)
                                    .androidxPhotoTile(sel) { sharedPhotos.value = if (sel) sharedPhotos.value - i else sharedPhotos.value + i },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (sel) Icon(Icons.Filled.CheckCircle, null, tint = Q.primary, modifier = Modifier.size(30.dp))
                                else Icon(Icons.Filled.AddPhotoAlternate, null,
                                    tint = Q.mutedFg.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { step = 2 },
                        enabled = sharedPhotos.value.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Q.primary, contentColor = Q.primaryFg,
                            disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (sharedPhotos.value.isEmpty()) "Ən azı bir şəkil seç"
                            else "${sharedPhotos.value.size} şəkli biznes hesabına paylaş",
                            style = T.button,
                        )
                    }
                }
                2 -> {
                    // — taq —
                    Text("İştirakçıları taq et",
                        style = T.meta.copy(fontWeight = FontWeight.W700, fontSize = 12.5.sp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Taq edilməyən iştirakçı XP almır. Hər taq edilənə " +
                            "+${XpRules.TAGGED_PARTICIPANT_XP} XP, sənə təşkilatçı kimi " +
                            "+${XpRules.ORGANIZER_XP} XP.",
                        style = T.meta.copy(fontSize = 11.5.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    participants.forEach { p ->
                        val sel = p.id in tagged.value
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .androidxTagRow(sel) { tagged.value = if (sel) tagged.value - p.id else tagged.value + p.id }
                                .padding(10.dp),
                        ) {
                            Box(
                                Modifier.size(32.dp).background(Q.muted, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(p.firstName.take(1), style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W800))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.fullName, style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700))
                                Text("${p.distanceLabel} · ortaq maraq", style = T.meta.copy(fontSize = 10.5.sp))
                            }
                            Icon(
                                if (sel) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                                tint = if (sel) Q.primary else Q.mutedFg, modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // canlı XP önizləməsi
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .androidxPreviewBg()
                            .padding(12.dp)
                    ) {
                        XpRow("Sən (təşkilatçı)", "+${XpRules.ORGANIZER_XP} XP", Icons.Filled.WorkspacePremium)
                        Spacer(Modifier.height(5.dp))
                        XpRow("Taq edilən ${tagged.value.size} iştirakçı",
                            "+${tagged.value.size * XpRules.TAGGED_PARTICIPANT_XP} XP", Icons.Filled.Groups)
                        Spacer(Modifier.height(7.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).androidxFooterDivider())
                        Spacer(Modifier.height(7.dp))
                        XpRow("Cəmi paylanacaq",
                            "+${XpRules.ORGANIZER_XP + tagged.value.size * XpRules.TAGGED_PARTICIPANT_XP} XP",
                            Icons.Filled.Toll)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            result = engine.finishActivity(event, tagged.value.toList())
                            step = 3
                        },
                        enabled = tagged.value.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Q.primary, contentColor = Q.primaryFg,
                            disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tagged.value.isEmpty()) "Ən azı bir iştirakçı taq et" else "Bitir və XP payla",
                            style = T.button,
                        )
                    }
                }
                else -> {
                    // — nəticə —
                    val r = result
                    if (r == null) {
                        EmptyState(icon = Icons.Filled.WorkspacePremium, text = "XP paylanmadı — yenidən cəhd et.")
                    } else {
                        PopIn {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(Modifier.size(62.dp).background(Q.primary, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    Icon(az.qoshul.app.core.ui.emojiEventsIcon, null, tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(32.dp))
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("Aktivite bitdi!", style = T.h2.copy(fontSize = 15.sp))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${sharedPhotos.value.size} şəkil ${r.venueName}-in biznes hesabına düşdü. " +
                                        "${r.taggedCount} iştirakçı taq edildi — hər birinə " +
                                        "+${r.perParticipantXp} Endirim XP yazıldı.",
                                    style = T.meta.copy(fontSize = 11.5.sp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                Spacer(Modifier.height(16.dp))
                                KvRow("Sənə (təşkilatçı)", "+${r.organizerXp} XP · ${r.venueName} bazası")
                                KvRow("İştirakçılara", "+${r.participantsTotal} XP (hərəsinə ${r.perParticipantXp})")
                                KvRow("Yeni balansın", "${acct.balance} XP")
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "XP yalnız ${r.venueName} bazasında keçərlidir — digər obyektdən " +
                                        "endirim almaq olmaz (ayrı baza qaydası).",
                                    style = T.meta.copy(fontSize = 10.5.sp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(Q.rMd.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                                ) { Text("Əla!", style = T.button) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun XpRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Q.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = T.meta.copy(fontSize = 12.5.sp), modifier = Modifier.weight(1f))
        Text(value, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W800, color = Q.primary))
    }
}

@Composable
private fun KvRow(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, style = T.meta.copy(fontSize = 12.5.sp))
        Text(v, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = Q.primary))
    }
}

private fun Modifier.androidxPhotoTile(sel: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .clickable(onClick = onClick)
        .background(if (sel) Q.secondary else Q.muted, RoundedCornerShape(Q.rMd.dp))
        .border(
            if (sel) 2.dp else 1.dp,
            if (sel) Q.primary else Q.border,
            RoundedCornerShape(Q.rMd.dp),
        )
)

private fun Modifier.androidxTagRow(sel: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .clickable(onClick = onClick)
        .background(if (sel) Q.secondary else Q.card, RoundedCornerShape(Q.rMd.dp))
        .border(
            if (sel) 2.dp else 1.dp,
            if (sel) Q.primary else Q.border,
            RoundedCornerShape(Q.rMd.dp),
        )
)

private fun Modifier.androidxPreviewBg(): Modifier = this.then(
    Modifier
        .background(Q.secondary, RoundedCornerShape(Q.rMd.dp))
        .border(1.dp, Q.border, RoundedCornerShape(Q.rMd.dp))
)

private fun Modifier.androidxFooterDivider(): Modifier = this.then(Modifier.background(Q.border))
