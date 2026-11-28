package az.qoshul.app.features.profile

import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.data.Taxonomy
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.QRDropDown
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.auth.QoshulTextField

/**
 * PROFİL REDAKTƏ EKRANI — D-112 (v0.41.0, istifadəçi direktivi 2026-11-09:
 * „profil edit düyməsi özü bir taba yönləndirsin, ad, soyad, profil şəkli
 * seçimi orda olsun“). Əvvəlki kiçik ad-dialoqu (ProfileScreen AlertDialog)
 * ləğv — redaktə tam ekran push-səhifədir:
 *  - AD və SOYAD ayrıca sahələr (engine.setProfileFullName — vahid
 *    „Ad Soyad“ profileName törəməsi);
 *  - PROFİL ŞƏKLİ seçimi — 4 brend şəkli (imagePaint açarları) arasından
 *    dairə seçici; seçim engine.setAvatar ilə state-ə yazılır;
 *  - „Yadda saxla“ — hər ikisini birlikdə saxlayır və geri qayıdır.
 *
 * D-167 (direktiv: „user ayralardan (əgər google ilə giriş etməyibsə)
 * maili dəyişə bilməlidir…“): e-poçt idarəetməsi əVVƏL burada idi —
 * D-184 (6.3) ilə AYARLAR → HESAB bölməsinə köçürüldü (SettingsScreen).
 *
 * D-184 (Loop 3, 6.4): profil təfərrüatları redaktəyə gəlir —
 *  - DİLLƏR iki siyahı: „Bilənlər“ (languageOptions) + „Öyrəndiklərim“
 *    (hər dilə səviyyə: Başlanğıc/Orta/Yaxşı/Ana dili). Serverdə
 *    languages jsonb massivi — „Dil (Səviyyə)“ formatında saxlanılır
 *    („English (Orta)“) — bir sahə, iki mənalı;
 *  - QRUP ÖLÇÜSÜ — Taxonomy.groupSizes + „Heç nə“ (DB NULL-a qayıdış
 *    yolu — allowEmpty; server tərəfdə p_group_size=NULL keçir);
 *  - BÜDCƏ — hazırkı siyahı + „Fərq etmir“ + „Öz aralığım“ (min≤max
 *    AZN; validasiya: min≤max, hər ikisi ≥0; boş min = yalnız max);
 *  - SƏVİYYƏ — ana dil səviyyəsi seçimi (languageLevels) — profilin
 *    „səviyyə“ göstəricisi.
 *
 * BÜTÜN forma state-i rememberSaveable — proses ölüb diriləndə (məs.
 * kamera/şəkil seçicidən qayıdış) yazılan mətn İT MİR.
 */
@Composable
fun ProfileEditScreen(engine: AppEngine, nav: NavState) {
    val st by engine.state.collectAsStateWithLifecycle()
    // D-184: rememberSaveable — proses death bərpası (6.1 tələbi)
    var first by rememberSaveable { mutableStateOf(st.firstName) }
    var last by rememberSaveable { mutableStateOf(st.lastName) }
    var avatar by rememberSaveable { mutableStateOf(st.avatar) }
    // — profil təfərrüatları (D-184 / 6.4) —
    var langsKnown by rememberSaveable { mutableStateOf(st.languages) }
    var groupSize by rememberSaveable { mutableStateOf(st.groupSize) }
    var budget by rememberSaveable { mutableStateOf(st.budget) }
    var level by rememberSaveable { mutableStateOf(st.languages.firstOrNull { it.endsWith("(Ana dili)") }?.let { l -> l.removeSuffix(" (Ana dili)") } ?: "") }

    // „Öz aralığım“ büdcə sahələri (yalnız custom rejim aktiv olanda görsənir)
    var budgetCustom by rememberSaveable { mutableStateOf(false) }
    var budgetMin by rememberSaveable { mutableStateOf("") }
    var budgetMax by rememberSaveable { mutableStateOf("") }

    val avatarOptions = listOf("coffee", "padel", "tech", "friends_baku")
    val canSave = first.isNotBlank()

    /** Dil əlavə dialoqu („+“ düyməsi) — dil + səviyyə seçimi. */
    var addLangOpen by remember { mutableStateOf(false) }
    var addLangLevelOpen by remember { mutableStateOf(false) }
    var pendingLang by remember { mutableStateOf<String?>(null) }

    PushScreenScaffold(
        title = "Profili redaktə et",
        onBack = { nav.pop() },
    ) { mod ->
        Column(
            mod
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            // — cari profil şəkli (böyük önizləmə) —
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .background(Q.card, CircleShape)
                        .border(4.dp, Q.card, CircleShape)
                        .border(2.dp, Q.primaryText, CircleShape),
                ) {
                    Image(
                        painter = imagePaint(avatar),
                        contentDescription = "Profil şəkli",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            // — profil şəkli seçimi —
            // D-177 (ikinci dalğa): şəkil yükləmə backendi hələ aktiv
            // olmadığından (PHOTO_UPLOAD_ENABLED=false) avatar dəyişmə UI
            // GİZLİDİR — brend avatar seçimi real yükləmə gələndə açılır.
            if (az.qoshul.app.core.backend.BackendConfig.photoUploadEnabled) {
                Text("Profil şəkli", style = T.label)
                Spacer(Modifier.height(9.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    avatarOptions.forEach { key ->
                        val label = key.replaceFirstChar { it.uppercase() }
                        val sel = key == avatar
                        val interaction = remember { MutableInteractionSource() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .androidxAvatarPick(interaction, sel) {
                                    avatar = key
                                }
                                .padding(vertical = 9.dp),
                        ) {
                            Image(
                                painter = imagePaint(key),
                                contentDescription = key,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (sel) Modifier.border(2.dp, Q.primaryText, CircleShape)
                                        else Modifier
                                    ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (sel) {
                                    Icon(
                                        Icons.Filled.CheckCircle, null,
                                        tint = Q.primaryText, modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                }
                                Text(
                                    label,
                                    style = T.chipXs.copy(
                                        fontWeight = if (sel) FontWeight.W700 else FontWeight.W500,
                                        color = if (sel) Q.primaryText else Q.mutedFg,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            // — ad / soyad —
            Spacer(Modifier.height(10.dp))
            Text("Ad", style = T.label)
            Spacer(Modifier.height(7.dp))
            QoshulTextField(value = first, onValueChange = { if (it.length <= 30) first = it },
                hint = "Adın")
            Text("Soyad", style = T.label)
            Spacer(Modifier.height(7.dp))
            QoshulTextField(value = last, onValueChange = { if (it.length <= 30) last = it },
                hint = "Soyadın")

            // ═══ D-184 (6.4): PROFİL TƏFƏRRÜATLARI ═══

            // — QRUP ÖLÇÜSÜ („Heç nə“ = DB NULL) —
            Spacer(Modifier.height(22.dp))
            QRDropDown(
                label = "Qrup ölçüsü",
                options = Taxonomy.groupSizes,
                selected = groupSize,
                allowEmpty = true,
                emptyLabel = "Heç nə",
            ) { groupSize = it }

            // — BÜDCƏ („Fərq etmir“ + „Öz aralığım“) —
            Spacer(Modifier.height(16.dp))
            if (!budgetCustom) {
                QRDropDown(
                    label = "Büdcə",
                    options = Taxonomy.budgets,
                    selected = budget,
                ) { pick ->
                    if (pick == "Öz aralığım") {
                        // custom rejimə keç — hazırki dəyərdən min/max törət
                        val nums = Regex("\\d+").findAll(budget).map { it.value }.toList()
                        budgetMin = nums.firstOrNull() ?: ""
                        budgetMax = nums.lastOrNull() ?: ""
                        budgetCustom = true
                    } else budget = pick
                }
            } else {
                Text("Büdcə — öz aralığın (AZN)", style = T.label)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QoshulTextField(
                        value = budgetMin,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) budgetMin = it },
                        hint = "min",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                    )
                    QoshulTextField(
                        value = budgetMax,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) budgetMax = it },
                        hint = "max",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                    )
                    androidx.compose.material3.IconButton(
                        onClick = { budgetCustom = false; budget = "" },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Close, "Aralığı sil", tint = Q.mutedFg)
                    }
                }
                val mn = budgetMin.toIntOrNull()
                val mx = budgetMax.toIntOrNull()
                if (mn != null && mx != null && mn > mx) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Minimum maksimumdan böyük ola bilməz",
                        style = T.chipXs.copy(color = Q.destructive),
                    )
                }
            }

            // — SƏVİYYƏ (ana dil səviyyəsi) —
            Spacer(Modifier.height(16.dp))
            QRDropDown(
                label = "Ana dil səviyyəsi",
                options = Taxonomy.languageLevels,
                selected = level,
            ) { level = it }

            // — DİLLƏR: bilənlər + öyrəndiklərim —
            Spacer(Modifier.height(22.dp))
            Text("Dillər", style = T.label)
            Spacer(Modifier.height(4.dp))
            Text(
                "Bilənlər və öyrəndiklərin — hər dilə səviyyə qeyd et.",
                style = T.chipXs.copy(color = Q.mutedFg),
            )
            Spacer(Modifier.height(9.dp))
            // cari seçilmiş dillər — çip kimi, "+" ilə əlavə
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                langsKnown.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Q.secondary, RoundedCornerShape(Q.rPill.dp))
                            .border(1.dp, Q.primaryText, RoundedCornerShape(Q.rPill.dp))
                            .clickable {
                                langsKnown = langsKnown - entry
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            entry,
                            style = T.chipXs.copy(color = Q.primaryText, fontWeight = FontWeight.W600),
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Close, "Sil", tint = Q.primaryText,
                            modifier = Modifier.size(12.dp))
                    }
                }
                // „+“ çipi — dil əlavə et
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { addLangOpen = true }
                        .background(Q.card, RoundedCornerShape(Q.rPill.dp))
                        .border(1.dp, Q.border, RoundedCornerShape(Q.rPill.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Add, "Dil əlavə et", tint = Q.primaryText,
                        modifier = Modifier.size(14.dp))
                    Text("Əlavə et", style = T.chipXs.copy(color = Q.primaryText))
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    engine.setProfileFullName(first.trim(), last.trim())
                    engine.setAvatar(avatar)
                    // D-184: qrup ölçüsü — „Heç nə“ → DB NULL (boş string
                    // p_group_size=null kimi ötürülür — server coalesce
                    // saxlayır, amma bizim RPC yeni parametrlə NULL
                    // yazmağı dəstəkləyir: 0015 miqrasiyası)
                    engine.setGroupSize(groupSize)
                    // büdcə: custom aralıq → „min–max AZN“ formatına birləşdir
                    val budgetOut = if (budgetCustom) {
                        val mn = budgetMin.toIntOrNull()
                        val mx = budgetMax.toIntOrNull()
                        when {
                            mn != null && mx != null && mn <= mx -> "$mn–$mx AZN"
                            mx != null -> "$mx AZN-ə qədər"
                            mn != null -> "$mn AZN-dən"
                            else -> ""
                        }
                    } else budget
                    engine.setBudget(budgetOut)
                    // dillər: səviyyə + ana dili birləşdir → languages massivi
                    val langList = buildList {
                        if (level.isNotBlank()) add("$level (ana dili)")
                        langsKnown.forEach { add(it) }
                    }.distinct()
                    engine.setLanguages(langList)
                    nav.pop()
                },
                // custom büdcə rejimində min>max olanda saxlama bloklanır
                enabled = canSave && !(budgetCustom && run {
                    val mn = budgetMin.toIntOrNull(); val mx = budgetMax.toIntOrNull()
                    mn != null && mx != null && mn > mx
                }),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Q.primary, contentColor = Q.primaryFg,
                    disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg,
                ),
            ) {
                Text("Yadda saxla", style = T.button)
            }
            if (!canSave) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ad boş ola bilməz",
                    style = T.chipXs.copy(color = Q.mutedFg),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ═══ Dil əlavə et dialoqları (iki mərhələ: dil → səviyyə) ═══
    if (addLangOpen) {
        LangPickDialog(
            title = "Dil seç",
            options = Taxonomy.languageOptions.filter { l ->
                // artıq əlavə olunmuş dili gizlət
                langsKnown.none { it.startsWith(l) }
            },
        ) { lang ->
            addLangOpen = false
            if (lang != null) {
                pendingLang = lang
                addLangLevelOpen = true
            }
        }
    }
    if (addLangLevelOpen && pendingLang != null) {
        LangPickDialog(
            title = "Səviyyə — ${pendingLang}",
            options = Taxonomy.languageLevels,
        ) { lvl ->
            addLangLevelOpen = false
            if (lvl != null && pendingLang != null) {
                langsKnown = langsKnown + "${pendingLang} ($lvl)"
                pendingLang = null
            }
        }
    }
}

/**
 * D-184: dil/səviyyə seçim dialoqu — SettingsScreen OptionDialog
 * üslubunda (sıra-sıra seçim; boş seçim = ləğv). Yenidən istifadə
 * olunmur (yalnız bu ekranda), ona görə privatdır.
 */
@Composable
private fun LangPickDialog(
    title: String,
    options: List<String>,
    onPick: (String?) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onPick(null) },
        title = { Text(title) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .androidxLangRow { onPick(opt) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                    ) { Text(opt, style = T.bodySm) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

private fun Modifier.androidxLangRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

/** Seçilə bilən avatar kartı fonu: seçili secondary + primary kontur. */
private fun Modifier.androidxAvatarPick(
    interaction: MutableInteractionSource,
    selected: Boolean,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .background(
            if (selected) Q.secondary else Q.card,
            RoundedCornerShape(Q.rLg.dp),
        )
        .border(
            if (selected) 1.2.dp else 1.dp,
            if (selected) Q.primaryText else Q.border,
            RoundedCornerShape(Q.rLg.dp),
        )
)
