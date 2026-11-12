package az.qoshul.app.features.events

import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.dateLabelFor
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.ActivityXpResult
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.model.LocalDate
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.motion.pressable
import androidx.compose.foundation.interaction.MutableInteractionSource
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.AiBox
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.QrCode
import az.qoshul.app.core.ui.SheetScaffold
import az.qoshul.app.core.ui.PickHostOverlay
import az.qoshul.app.core.ui.PickHostState
import az.qoshul.app.core.ui.PickOption
import az.qoshul.app.core.ui.PickPlacement
import az.qoshul.app.core.ui.pickAnchor
import az.qoshul.app.core.ui.rememberPickHostState
import az.qoshul.app.features.auth.QoshulTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

/**
 * "Birlikdə yeni plan quraq" — Flutter create_event_sheet.dart portu.
 * [initial] — Assistant [TƏDBİR] təklifindən prefill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventSheet(
    engine: AppEngine,
    onDismiss: () -> Unit,
    initial: az.qoshul.app.core.model.EventProposal? = null,
    presetCity: String = "",
    venue: az.qoshul.app.core.model.Partner? = null,
) {
    // U2.2 (UI-02): forma sahələri proses ölümündən sağ qalır (audit:
    // „CreateFinishSheets formaları sıfırlanır“). LocalDate/Boolean — Serializable.
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var idea by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var tags by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable {
        mutableStateOf(presetCity.ifEmpty { venue?.city ?: engine.state.value.city })
    }
    var category by rememberSaveable {
        // D-051 fix: initial==null → NPE riski aradan qaldırıldı (takeIf zənciri)
        mutableStateOf(initial?.category?.takeIf { engine.dataSource.allCategories.contains(it) } ?: "Qəhvə")
    }
    var format by rememberSaveable { mutableStateOf("Oflayn") }
    // D-051 fix: əvvəl düyməsiz ölü placeholder idi (onSelect boş, tipo "üçun")
    var ageLimit by rememberSaveable { mutableStateOf("Hamı üçün") }
    var date by rememberSaveable { mutableStateOf(engine.today().plusDays(1)) } // A2: vahid clock
    var time by rememberSaveable { mutableStateOf("18:00") }
    // D-053 fix: capacity artıq xam mətn kimi saxlanılır — əvvəl Int state
    // sahəni idarə edirdi, "1" yazmaq (15-in ilk rəqəmi) SİLMİŞ sayılırdı
    // (1 ∉ 2..500 → dəyər rədd edilirdi). Submittə clamp olunur.
    var capacityText by rememberSaveable {
        mutableStateOf(initial?.capacity?.takeIf { it in 2..500 }?.toString() ?: "8")
    }
    var paid by rememberSaveable { mutableStateOf(false) }
    var approval by rememberSaveable { mutableStateOf(false) }
    // D-114: tarix/saat PICKER dialoqları (kalendar + saat seçici — əl ilə
    // yazmaq/dropdown siyahısı əvəzinə)
    var dateOpen by remember { mutableStateOf(false) }
    var timeOpen by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiLocation by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    // D-089: forma pick-ləri üçün host — panel sheet-in kök qatında açılır
    val pickHost = rememberPickHostState()

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
            capacityText = "10"
            aiBusy = false
        }
    }

    fun submit() {
        // UI-7 (D-098): əl ilə yazılan saatın format yoxlaması —
        // "H:MM"/"HH:MM" qəbul olunur; səhv/boş yazılışda default 18:00.
        val t = time.trim()
        val safeTime = if (TIME_REGEX.matches(t)) t else "18:00"
        engine.createEvent(
            title = title.trim(), category = category, city = city,
            location = aiLocation ?: location.trim(),
            date = "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth),
            time = safeTime,
            // D-053: xam mətn submittə clamp olunur (boşa/toxunulmaya → 8)
            capacity = capacityText.toIntOrNull()?.coerceIn(2, 500) ?: 8,
            description = desc.trim(),
            epochDay = date.toEpochDay().toInt(),
            price = if (paid) price.toIntOrNull() ?: 0 else 0,
            format = format, approval = approval,
            ageLimit = ageLimit,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
        onDismiss()
    }

    SheetScaffold(
        onDismiss = onDismiss,
        overlay = {
            // D-089: pick panelləri formanın ÜZƏRİNƏ — sheet kök qatı;
            // D-110: vahid sütun (ikon + başlıq siyahısı)
            PickHostOverlay(
                host = pickHost,
                panelModifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                maxHeight = 300.dp,
                placement = PickPlacement.CENTERED,
            )
        },
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Birlikdə yeni plan quraq", style = T.h2)
            Spacer(Modifier.height(4.dp))
            Text("İstədiyin formada tədbir yarat və maraqlarını bölüşən insanlarla tanış ol.", style = T.subtitle)
            // — AI bloku —
            AiBox {
                Column {
                    Text("AI köməkçisi: fikrini qısa yaz",
                        style = T.bodySmSemi)
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoFixHigh, null, tint = Q.primaryText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            QoshulTextField(value = idea, onValueChange = { idea = it },
                                hint = "Məsələn, şənbə səhəri dağa yürüş")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // D-063: AI input → AI düymə arası 8→12dp (vizual audit: "gap small")
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { aiFill() },
                        enabled = !aiBusy && idea.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(Q.rPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Q.secondary, contentColor = Q.primaryText,
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
                    host = pickHost, key = "cat",
                    options = engine.dataSource.allCategories,
                    selected = category, onSelect = { category = it },
                )
            }
            Row {
                Box(Modifier.weight(1f)) {
                    FormLine("Format") {
                        DropdownLine(host = pickHost, key = "format", options = listOf("Oflayn", "Onlayn"), selected = format, onSelect = { format = it })
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    FormLine("Şəhər") {
                        DropdownLine(
                            host = pickHost, key = "city",
                            options = engine.dataSource.cities.map { it.name },
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
                        // D-114 (direktiv: „tarix və saat tarix seçici kalendar
                        // ilə açılsın“): etiket tarixin oxunaqlı adı („Sabah“/
                        // „Bu gün"/„Noy 12"), klik KALENDAR dialoqu açır.
                        PickerFieldTrigger(
                            value = dateLabelFor(date.toEpochDay(), engine.today()),
                            icon = Icons.Filled.CalendarMonth,
                            onClick = { dateOpen = true },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    FormLine("Saat") {
                        // D-114: saat da seçicidir („saat da onunla birlikdə“) —
                        // 24s TimePicker dialoqu; əl ilə yazmaq ləğv.
                        PickerFieldTrigger(
                            value = time,
                            icon = Icons.Filled.Schedule,
                            onClick = { timeOpen = true },
                        )
                    }
                }
            }
            // D-132 (direktiv 2026-11-11: „iştirakçı sayı minimum 2 yazılsın,
            // 2-500 yox“): etiket artıq yuxarı həddi göstərmir — yalnız
            // minimumu (daxili clamp 2..500 qorunur — Rust core pariteti).
            FormLine("İştirakçı sayı (minimum 2)") {
                // D-053: yalnız rəqəm + max 3 rəqəm; dəyər submittə clamp olunur
                QoshulTextField(
                    value = capacityText,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) capacityText = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
            // U4-1 (D-097): qonşu FormLine strukturuna keçid — əvvəl sağ sütun
            // nested FormLine idi (öz etiketi + 15dp üst doldurma) → sol dropdown
            // sahəsi ilə eyni bazada düz gəlmirdi (testçinin 31.10 telefon
            // skrinşotundakı "Yaş həddi aşağıda düşür" sinfi). Format/Şəhər və
            // Tarix/Saat sətirləri ilə eyni pattern.
            Row {
                Box(Modifier.weight(1f)) {
                    FormLine("İştirak haqqı") {
                        DropdownLine(host = pickHost, key = "paid", options = listOf("Pulsuz", "Ödənişli"), selected = if (paid) "Ödənişli" else "Pulsuz") {
                            paid = it == "Ödənişli"
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    FormLine(if (paid) "Qiymət (AZN)" else "Yaş həddi") {
                        if (paid) {
                            QoshulTextField(value = price,
                                onValueChange = { price = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ))
                        } else {
                            DropdownLine(
                                host = pickHost, key = "age",
                                options = listOf("Hamı üçün", "16+", "18+", "18–25", "25–35"),
                                selected = ageLimit, onSelect = { ageLimit = it },
                            )
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
                        .background(if (approval) Q.primaryText else Q.card, RoundedCornerShape(Q.rSm.dp))
                        .border(1.4.dp, if (approval) Q.primaryText else Q.border, RoundedCornerShape(Q.rSm.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (approval) Icon(Icons.Filled.CheckCircle, null, tint = Q.primaryFg, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Qoşulma istəklərini özüm təsdiqləyim",
                    style = T.bodyXs)
            }
            FormLine("Tədbir haqqında") {
                QoshulTextField(value = desc, onValueChange = { if (it.length <= 2000) desc = it },
                    maxLines = 4, hint = "Birlikdə nə edəcəyik?")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (title.isNotBlank() && desc.isNotBlank()) submit() },
                enabled = title.isNotBlank() && desc.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rPill),
                colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tədbir yarat", style = T.button)
            }
            // U4-2 (D-097): disabled CTA səbəbsiz boz görünürdü (testçi onu
            // xəta sanırdı — 31.10 skrinşotu) — FinishActivitySheet-in
            // "Ən azı bir şəkil seç" pattern-i buraya da gətirilir.
            if (title.isBlank() || desc.isBlank()) {
                Text(
                    "Davam etmək üçün tədbirin adı və təsviri mütləqdir",
                    style = T.chipXs.copy(color = Q.mutedFg),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // — D-114: TARİX KALENDARI (material3 DatePicker dialoqu) —
    if (dateOpen) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    dateOpen = false
                }) { Text("Təsdiqlə") }
            },
            dismissButton = {
                TextButton(onClick = { dateOpen = false }) { Text("Ləğv et") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    // — D-114: SAAT SEÇİCİSİ (24s TimePicker dialoqu) —
    if (timeOpen) {
        val parts = time.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 18,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timeOpen = false },
            title = { Text("Saatı seç", style = T.h3) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    time = "%02d:%02d".format(timeState.hour, timeState.minute)
                    timeOpen = false
                }) { Text("Təsdiqlə") }
            },
            dismissButton = {
                TextButton(onClick = { timeOpen = false }) { Text("Ləğv et") }
            },
        )
    }
}

private fun Modifier.androidxApprovalToggle(onClick: () -> Unit): Modifier =
    this.then(Modifier.pressable(onClick = onClick))

/** UI-7 (D-098 → D-114): saat PICKER-dən gəlir — format həmişə HH:MM;
 * regex yalnız müdafiə qalıdır (xarici prefill üçün). */
private val TIME_REGEX = Regex("^([01]?\\d|2[0-3]):[0-5]\\d$")

/** Sadə forma sətri. */
@Composable
private fun FormLine(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 15.dp)) {
        Text(label, style = T.label)
        Spacer(Modifier.height(7.dp))
        content()
    }
}

/**
 * D-114: tarix/saat sahəsi — DropdownLine görünüşü ilə eyni dil (ikon +
 * dəyər + çevrilən chevron YOX — dialoq açılır), amma pick host-u YOX:
 * klik material3 kalendar/saat dialoqu açır.
 */
@Composable
private fun PickerFieldTrigger(
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scaleTap(interaction)
            .androidxPickerFieldTap(interaction, onClick)
            .androidxDropdownBg()
            .padding(horizontal = 12.dp, vertical = 13.dp),
    ) {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(value, style = T.input, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.ExpandMore, "seç",
            tint = Q.mutedFg,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun Modifier.androidxPickerFieldTap(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
)

/** D-089: dropdown — üzərinə çıxan seçim paneli (PickHostOverlay
 * vasitəsilə, sheet-in kök Box-ında; əvvəlki 200dp scroll siyahısı
 * ləğv — "sürüşdürmək yerinə üzərinə çıxan format" direktivi).
 * Trigger: input sahəsi + çevrilən chevron. */
@Composable
fun DropdownLine(
    host: PickHostState,
    key: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val open = host.openReq?.key == key
    val chev by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(Motion.D1, easing = Motion.ease),
        label = "ddChev",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .pickAnchor(host, key)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .androidxDropdownBg()
                .androidxDropdownClick {
                    host.toggle(key, options.map { PickOption(it) }, selected, onSelect)
                }
                .padding(horizontal = 12.dp, vertical = 13.dp),
        ) {
            Text(
                selected.ifEmpty { "Seç" }, style = T.input,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ExpandMore, if (open) "bağla" else "seç",
                tint = if (open) Q.primaryText else Q.mutedFg,
                modifier = Modifier.size(18.dp).rotate(chev),
            )
        }
    }
}

private fun Modifier.androidxDropdownBg(): Modifier = this.then(
    Modifier
        // D-058: input sahəsi — reference 16px radius
        .background(Q.card, RoundedCornerShape(16.dp))
        .border(1.dp, Q.border, RoundedCornerShape(16.dp))
)

private fun Modifier.androidxDropdownClick(onClick: () -> Unit): Modifier =
    this.then(Modifier.pressable(onClick = onClick))

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
    var step by rememberSaveable { mutableStateOf(0) }
    val tagged = rememberSaveable { mutableStateOf(setOf<String>()) }
    val sharedPhotos = rememberSaveable { mutableStateOf(setOf<Int>()) }
    var result by remember { mutableStateOf<ActivityXpResult?>(null) }
    val st by engine.state.collectAsStateWithLifecycle()

    // U1.2 (UI-16): GERİ bir addım geri; 1-ci addımda / nəticədə bağlanır
    // (əvvəl: back hər addımda sheet-i qapatırdı — audit UI-16).
    BackHandler {
        if (result != null || step == 0) onDismiss() else step--
    }
    val acct = engine.venueXpFor(event.venueId ?: "")
    val participants = engine.dataSource.people.take(event.participants.coerceIn(1, 5))

    SheetScaffold(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Tədbiri bitir — „${event.title}“",
                style = T.personName)
            Spacer(Modifier.height(4.dp))
            Text(
                "Obyektin statik QR-ı → şəkil paylaş → iştirakçıları taq et → " +
                    "Endirim XP paylanır (təşkilatçı +${XpRules.ORGANIZER_XP}, " +
                    "hər taq +${XpRules.TAGGED_PARTICIPANT_XP}).",
                style = T.chipXs.copy(color = Q.mutedFg),
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
                            // D-063: 148→200dp — kamera üçün etibarlı oxuma ölçüsü
                            // (vizual audit: "~160dp looks too small for reliable scanning")
                            QrCode(payload = "STATIC-${acct.venueId}-${acct.venueName}", size = 200.dp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${acct.venueName} · obyektin statik QR-ı.\n" +
                            "Kassada və ya çıxışda oxu — tədbir obyektlə əlaqələnir.",
                        style = T.chipXs.copy(color = Q.mutedFg),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(Q.rPill),
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
                        style = T.bodySmBold.copy(color = Q.mutedFg))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Şəkli yalnız SƏN (təşkilatçı) paylaşa bilərsən — Aktivite şəkilləri " +
                            "obyektin biznes hesabına düşür (İnstagram-model).",
                        style = T.chipXs.copy(color = Q.mutedFg),
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
                                if (sel) Icon(Icons.Filled.CheckCircle, null, tint = Q.primaryText, modifier = Modifier.size(30.dp))
                                else Icon(Icons.Filled.AddPhotoAlternate, null,
                                    tint = Q.mutedFg.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { step = 2 },
                        enabled = sharedPhotos.value.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(Q.rPill),
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
                        style = T.bodySmBold.copy(color = Q.mutedFg))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Taq edilməyən iştirakçı XP almır. Hər taq edilənə " +
                            "+${XpRules.TAGGED_PARTICIPANT_XP} XP, sənə təşkilatçı kimi " +
                            "+${XpRules.ORGANIZER_XP} XP.",
                        style = T.chipXs.copy(color = Q.mutedFg),
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
                                Text(p.firstName.take(1), style = T.metaBold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.fullName, style = T.metaBold)
                                Text("${p.distanceLabel} · ortaq maraq", style = T.metaSm)
                            }
                            Icon(
                                if (sel) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                                tint = if (sel) Q.primaryText else Q.mutedFg, modifier = Modifier.size(21.dp),
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
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(Q.rPill),
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
                                    Icon(az.qoshul.app.core.ui.emojiEventsIcon, null, tint = Q.primaryFg,
                                        modifier = Modifier.size(32.dp))
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("Aktivite bitdi!", style = T.personName)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${sharedPhotos.value.size} şəkil ${r.venueName}-in biznes hesabına düşdü. " +
                                        "${r.taggedCount} iştirakçı taq edildi — hər birinə " +
                                        "+${r.perParticipantXp} Endirim XP yazıldı.",
                                    style = T.chipXs.copy(color = Q.mutedFg),
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
                                    style = T.metaSm,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(Q.rPill),
                                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                                ) { Text("Əla!", style = T.button) }
                            }
                        }
                    }
                }
            }
            // D-063: 24→8dp — SheetScaffold özü artıq 24dp alt boşluq verir
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun XpRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Q.primaryText, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = T.bodySm.copy(color = Q.mutedFg), modifier = Modifier.weight(1f))
        Text(value, style = T.metaBold.copy(color = Q.primaryText))
    }
}

@Composable
private fun KvRow(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, style = T.bodySm.copy(color = Q.mutedFg))
        Text(v, style = T.bodySmBold.copy(color = Q.primaryText))
    }
}

private fun Modifier.androidxPhotoTile(sel: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .pressable(onClick = onClick)
        .background(if (sel) Q.secondary else Q.muted, RoundedCornerShape(Q.rMd.dp))
        .border(
            if (sel) 2.dp else 1.dp,
            if (sel) Q.primaryText else Q.border,
            RoundedCornerShape(Q.rMd.dp),
        )
)

private fun Modifier.androidxTagRow(sel: Boolean, onClick: () -> Unit): Modifier = this.then(
    Modifier
        .pressable(onClick = onClick)
        .background(if (sel) Q.secondary else Q.card, RoundedCornerShape(Q.rMd.dp))
        .border(
            if (sel) 2.dp else 1.dp,
            if (sel) Q.primaryText else Q.border,
            RoundedCornerShape(Q.rMd.dp),
        )
)

private fun Modifier.androidxPreviewBg(): Modifier = this.then(
    Modifier
        .background(Q.secondary, RoundedCornerShape(Q.rMd.dp))
        .border(1.dp, Q.border, RoundedCornerShape(Q.rMd.dp))
)

private fun Modifier.androidxFooterDivider(): Modifier = this.then(Modifier.background(Q.border))
