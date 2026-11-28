package az.qoshul.app.features.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.motion.pressable
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.data.Taxonomy
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.DesignColor
import az.qoshul.app.core.theme.DesignMode
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.features.partners.BusinessModeSheet
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.Route
import androidx.compose.material.icons.filled.People
import az.qoshul.app.features.events.CityPickerSheet
import az.qoshul.app.features.auth.QoshulTextField

/**
 * Ayarlar — Flutter settings_screen.dart portu (ROL TAB 5):
 * Hesab / Məxfilik (D-027 3-seçimli) / Bildirişlər / Təhlükəsizlik / sil-çıxış.
 * A1 düzəlişi: "Haqqında" versiyası BuildConfig-dən oxunur.
 */
@Composable
fun SettingsScreen(engine: AppEngine, nav: NavState) {
    val st by engine.state.collectAsStateWithLifecycle()
    // P0.2 (D-182): signOut üçün context — kompozisiya fazasında götürülür
    // (LocalContext.current YALNIZ @Composable kontekstdə çağırıla bilər;
    // onClick lambda-sı içində deyil)
    val signOutCtx = androidx.compose.ui.platform.LocalContext.current
    var showVisibility by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPhone by remember { mutableStateOf(false) }
    // D-115: şirkət hesabı dialoqu
    var showCompany by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    // D-131: Biznes rejimi sheet-i (şirkət dialoqundan açılır)
    var businessOpen by remember { mutableStateOf(false) }
    var cityOpen by remember { mutableStateOf(false) }
    // D.1 (D-170): profil təfərrüatları — status/dillər/qrup/büdcə seçimi
    var statusOpen by remember { mutableStateOf(false) }
    var langsOpen by remember { mutableStateOf(false) }
    var groupOpen by remember { mutableStateOf(false) }
    var budgetOpen by remember { mutableStateOf(false) }
    var inviteOpen by remember { mutableStateOf(false) }
    // D-119: dizayn seçim dialoqları (rəng + mod)
    var showDesignColor by remember { mutableStateOf(false) }
    var showDesignMode by remember { mutableStateOf(false) }
    // D-184 (6.3): e-poçt/şifrə idarəetməsi (ProfileEdit-dən köçürülüb)
    var email by rememberSaveable { mutableStateOf("") }
    var emailLoaded by remember { mutableStateOf(false) }
    var hasEmailIdentity by remember { mutableStateOf<Boolean?>(null) }
    var emailOpen by remember { mutableStateOf(false) }
    var newPass by rememberSaveable { mutableStateOf("") }
    var mailBusy by remember { mutableStateOf(false) }
    var mailMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var mailErr by rememberSaveable { mutableStateOf<String?>(null) }
    val mailScope = rememberCoroutineScope()

    // Cari e-poçt + identitet növü (bir dəfə, ekrana girəndə — D-167 portu)
    LaunchedEffect(Unit) {
        if (az.qoshul.app.core.backend.BackendConfig.isConfigured &&
            az.qoshul.app.core.backend.SupabaseBackend.client != null
        ) {
            runCatching {
                email = az.qoshul.app.core.backend.SupabaseBackend.currentEmail() ?: ""
                hasEmailIdentity = az.qoshul.app.core.backend.SupabaseBackend.hasEmailIdentity()
            }
            emailLoaded = true
        }
    }

    fun saveMail() {
        val v = email.trim()
        mailErr = null; mailMsg = null
        if (v.isBlank() || !v.contains("@")) {
            mailErr = "Düzgün e-poçt ünvanı yaz."
            return
        }
        mailScope.launch {
            mailBusy = true
            try {
                if (hasEmailIdentity == true) {
                    az.qoshul.app.core.backend.SupabaseBackend.updateUserEmail(v)
                    mailMsg = "Təsdiq məktubu yeni ünvana göndərildi. " +
                        "Hər iki ünvanda təsdiqləyəndə dəyişiklik tamamlanar."
                } else {
                    az.qoshul.app.core.backend.SupabaseBackend.linkEmailIdentity(v)
                    mailMsg = "Təsdiq məktubu göndərildi. Ünvanı təsdiqləyib " +
                        "şifrə qoyandan sonra eyni hesabla e-poçt ilə də daxil ola bilərsən."
                }
            } catch (t: Throwable) {
                mailErr = t.message?.take(140) ?: "Əməliyyat alınmadı."
            } finally {
                mailBusy = false
            }
        }
    }

    fun savePassword() {
        mailErr = null; mailMsg = null
        if (newPass.length < 8) {
            mailErr = "Şifrə ən azı 8 simvol olmalıdır."
            return
        }
        mailScope.launch {
            mailBusy = true
            try {
                az.qoshul.app.core.backend.SupabaseBackend.updatePassword(newPass)
                newPass = ""
                mailMsg = "Şifrə yeniləndi. Artıq e-poçt + şifrə ilə də daxil ola bilərsən."
            } catch (t: Throwable) {
                mailErr = t.message?.take(140) ?: "Əməliyyat alınmadı."
            } finally {
                mailBusy = false
            }
        }
    }

    PushScreenScaffold(title = "Ayarlar", onBack = { nav.pop() }) { mod ->
        LazyColumn(
            modifier = mod.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
            ),
        ) {
            item { SectionLabel("Hesab") }
            item {
                SettingsCard {
                    RowItem(Icons.Filled.Person, "Ad", st.profileName)
                    DividerLine()
                    // D-055: şəhər sətri artıq KLİKLƏNƏBİLİR — CityPickerSheet açır
                    // (25 şəhər; setCity feed/map/aktiv tədbirləri dəyişir)
                    NavRow(Icons.Filled.LocationCity, "Şəhər", value = st.city) {
                        cityOpen = true
                    }
                    DividerLine()
                    // D-184 (6.3): E-POÇT idarəetməsi ProfileEdit-dən
                    // (D-167) AYARLARA köçürüldü — hesab məlumatı profil
                    // redaktəsində deyil, hesab bölməsində idarə olunur.
                    // emailLoaded=false → server konfiqurasiyası yoxdur
                    // (demo rejim) — sətirlər gizli qalır.
                    if (emailLoaded) {
                        NavRow(
                            Icons.Filled.Email, "E-poçt",
                            if (hasEmailIdentity == true) (email.ifBlank { "qeyd olunmayıb" })
                            else "Google hesabı — e-poçt əlavə et",
                        ) { emailOpen = true }
                        DividerLine()
                        NavRow(Icons.Filled.Lock, "Şifrə",
                            if (hasEmailIdentity == true) "Dəyiş" else "Qeyd et — e-poçt girişi üçün",
                        ) { emailOpen = true }
                        DividerLine()
                    }
                    // D.1 (D-170): profil seçimləri Ayarlardan idarə olunur —
                    // hər dəyişiklik dərhal serverə yazılır (setter-lər
                    // SaveProfileIntent göndərir)
                    NavRow(Icons.Filled.School, "Status",
                        value = engine.statusLabel(st.status).ifBlank { "Seçilməyib" }) {
                        statusOpen = true
                    }
                    DividerLine()
                    NavRow(Icons.Filled.Translate, "Dillər",
                        value = st.languages.joinToString(", ").ifBlank { "Seçilməyib" }) {
                        langsOpen = true
                    }
                    DividerLine()
                    NavRow(Icons.Filled.Groups, "Qrup ölçüsü",
                        value = st.groupSize.ifBlank { "Seçilməyib" }) {
                        groupOpen = true
                    }
                    DividerLine()
                    NavRow(Icons.Filled.Payments, "Büdcə",
                        value = st.budget.ifBlank { "Seçilməyib" }) {
                        budgetOpen = true
                    }
                    DividerLine()
                    RowItem(Icons.Filled.WorkspacePremium, "Səviyyə", st.rank)
                    DividerLine()
                    // D-056: dost dəvəti — INVITE_XP (+100) əvvəl heç bir UI-a bağlı deyildi
                    NavRow(Icons.Filled.People, "Dostunu dəvət et", value = "+${XpRules.INVITE_XP} Qoşa Coin") {
                        inviteOpen = true
                    }
                    // D-167 (direktiv: „ayarlarda user ilə əlaqəsiz məlumat
                    // yəni məsələn server modunda olması kimi artıq məlumat
                    // vermə“): „Server statusu“ sətri SİLİNDİ — texniki
                    // detal istifadəçiyə aid deyil.
                    DividerLine()
                    NavRow(
                        Icons.Filled.VerifiedUser, "Hesabı təsdiqlə (telefon)",
                        if (st.phoneVerified) "Təsdiqlənmiş (+${XpRules.PHONE_VERIFY_XP} Qoşa Coin)"
                        else "Təsdiq edilməyib",
                        valueColor = if (st.phoneVerified) Q.primaryText else androidx.compose.ui.graphics.Color.Unspecified,
                    ) { if (!st.phoneVerified) showPhone = true }
                }
            }
            item { SectionLabel("Məxfilik") }
            item {
                SettingsCard {
                    NavRow(
                        Icons.Filled.Map, "Xəritədə görünmə", null,
                        subtitle = "${st.mapVisibility.label} — ${st.mapVisibility.hint}",
                    ) { showVisibility = true }
                    DividerLine()
                    SwitchRow(Icons.Filled.NearMe, "Məsafəmi göstər (~2 km)",
                        st.showDistance) { engine.setShowDistance(it) }
                }
            }
            item {
                Text(
                    "Dəqiq koordinatın heç kimə göstərilmir — yalnız təxmini məsafə. " +
                        "Qoşa Coin-in müddəti yoxdur — balans = qazanma − xərcləmə.",
                    style = T.chipXs.copy(color = Q.mutedFg),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            item { SectionLabel("Bildirişlər") }
            item {
                SettingsCard {
                    NavRow(Icons.Filled.Tune, "Bildiriş seçimləri", null,
                        subtitle = "Hər tip ayrıca; marketinq 23:00-08:00 sükut") {
                        nav.push(Route.Notifications)
                    }
                }
            }
            item { SectionLabel("Dizayn") }
            item {
                // D-119 (direktiv 2026-11-10: „ayarlarda dizayn bölməsində ağ və
                // qara modu əlavə olunacaq, seçim olaraq rənglər #008b2d
                // (default) və #eac66c“): rəng × mod seçimi — dərhal tətbiq
                // olunur (Q.apply — bütün app recompose).
                SettingsCard {
                    NavRow(
                        Icons.Filled.Palette, "Rəng", value = st.designColor.label,
                        subtitle = "Brend rəngi — Yaşıl (#008B2D) və ya Qızılı (#EAC66C)",
                    ) { showDesignColor = true }
                    DividerLine()
                    NavRow(
                        Icons.Filled.DarkMode, "Mod", value = st.designMode.label,
                        subtitle = "Ağ, Qara və ya Amoled (tam qara) üzlük",
                    ) { showDesignMode = true }
                }
            }
            item { SectionLabel("Təhlükəsizlik və dəstək") }
            item {
                SettingsCard {
                    NavRow(Icons.Outlined.HealthAndSafety, "İlk görüş məsləhətləri", null) {
                        nav.push(Route.Safety)
                    }
                    DividerLine()
                    NavRow(Icons.Filled.Block, "Bloklananlar (${st.blocked.size})", null,
                        subtitle = if (st.blocked.isEmpty()) "Bloklanan istifadəçi yoxdur"
                        else "Bəzi istifadəçilər bloklanıb",
                        enabled = st.blocked.isNotEmpty()) { showBlocked = true }
                    DividerLine()
                    NavRow(Icons.Filled.Language, "Dil", null, subtitle = "Azərbaycan dili")
                    DividerLine()
                    NavRow(Icons.Filled.Info, "Haqqında (v${BuildConfig.VERSION_NAME})", null,
                        subtitle = "Native (Compose)")
                    DividerLine()
                    // D-117: xəritə krediti — xəritə üzərindəki SDK atribusiya
                    // zolağı gizlədilib (overlay ləğv); OSM/OpenFreeMap lisenziya
                    // krediti burada, Haqqında-da qalır.
                    NavRow(Icons.Filled.Public, "Xəritə məlumatları",
                        value = "© OpenStreetMap contributors",
                        subtitle = "OpenFreeMap × OpenMapTiles tile-ları")
                    DividerLine()
                    NavRow(Icons.Filled.Description, "Şərtlər və məxfilik", null) { showTerms = true }
                }
            }
            item {
                // D-115 (direktiv: „ayarların sonuna yaxın bir yerdə şirkət
                // hesabına keç düyməsi olsun“): təhlükəsizlik kartından
                // SONRA, hesab silmə/çıxış düymələrindən ƏVVƏL.
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showCompany = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Q.card, contentColor = Q.primaryText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.primaryText),
                ) {
                    Icon(Icons.Filled.BusinessCenter, null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.padding(4.dp))
                    Text("Şirkət hesabına keç", style = T.button)
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Q.destructive),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.destructive),
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.padding(4.dp))
                    Text("Hesabı silmək", style = T.button)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            runCatching {
                                az.qoshul.app.core.backend.SupabaseBackend.signOut(signOutCtx)
                            }
                            engine.signOut()
                            nav.popAll()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Q.card, contentColor = Q.foreground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.height(16.dp)) // U4-4: AutoMirrored
                    Spacer(Modifier.padding(4.dp))
                    Text("Çıxış et", style = T.button)
                }
            }
        }
    }

    // — D-027: görünüş dialoqu —
    if (showVisibility) {
        AlertDialog(
            onDismissRequest = { showVisibility = false },
            title = { Text("Xəritədə görünmə") },
            text = {
                Column {
                    MapVisibility.entries.forEach { v ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .androidxVisRow { engine.setMapVisibility(v); showVisibility = false }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Icon(
                                if (st.mapVisibility == v) Icons.Filled.RadioButtonChecked
                                else Icons.Filled.RadioButtonUnchecked, null,
                                tint = if (st.mapVisibility == v) Q.primaryText else Q.mutedFg,
                                modifier = Modifier.height(19.dp),
                            )
                            Spacer(Modifier.padding(6.dp))
                            Column {
                                Text(v.label, style = T.bodyBold)
                                Text(v.hint, style = T.chipXs.copy(color = Q.mutedFg))
                            }
                        }
                    }
                }
            },
            // U1.6 (UI-32): heç bir affirmativ düyməsi olmayan dialoq — „Bağla“
            // düyməsi əlavə olundu (əvvəl confirmButton = {} — boş idi).
            confirmButton = { TextButton(onClick = { showVisibility = false }) { Text("Bağla") } },
        )
    }

    // — D-119: dizayn seçim dialoqları (rəng × mod — dərhal tətbiq) —
    if (showDesignColor) {
        AlertDialog(
            onDismissRequest = { showDesignColor = false },
            title = { Text("Rəng") },
            text = {
                Column {
                    DesignColor.entries.forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .androidxVisRow { engine.setDesignColor(c); showDesignColor = false }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            // nümayiş rəngi (swatch) — seçimin görünən təsdiqi
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .background(c.swatch, CircleShape)
                                    .border(1.5.dp, if (st.designColor == c) Q.primaryText else Q.border, CircleShape)
                            )
                            Spacer(Modifier.padding(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.label, style = T.bodyBold)
                                Text(
                                    if (c == DesignColor.GREEN) "Brend yaşılı — düymələr və vurğular"
                                    else "Qızılı — isti, günəşli alternativ",
                                    style = T.chipXs.copy(color = Q.mutedFg),
                                )
                            }
                            Icon(
                                if (st.designColor == c) Icons.Filled.RadioButtonChecked
                                else Icons.Filled.RadioButtonUnchecked, null,
                                tint = if (st.designColor == c) Q.primaryText else Q.mutedFg,
                                modifier = Modifier.height(19.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDesignColor = false }) { Text("Bağla") } },
        )
    }

    if (showDesignMode) {
        AlertDialog(
            onDismissRequest = { showDesignMode = false },
            title = { Text("Mod") },
            text = {
                Column {
                    DesignMode.entries.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .androidxVisRow { engine.setDesignMode(m); showDesignMode = false }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Icon(
                                // D-123: üç mod — Ağ / Qara / Amoled (tam qara, OLED)
                                when (m) {
                                    DesignMode.LIGHT -> Icons.Filled.LightMode
                                    DesignMode.DARK -> Icons.Filled.DarkMode
                                    DesignMode.AMOLED -> Icons.Filled.Contrast
                                }, null,
                                tint = if (st.designMode == m) Q.primaryText else Q.mutedFg,
                                modifier = Modifier.height(19.dp),
                            )
                            Spacer(Modifier.padding(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.label, style = T.bodyBold)
                                Text(
                                    when (m) {
                                        DesignMode.LIGHT -> "Ağ fon, tünd mətn (default)"
                                        DesignMode.DARK -> "Qara fon, işıq mətn — gecə gözləri üçün"
                                        DesignMode.AMOLED -> "Tam qara fon — AMOLED ekranda batareya qənaəti"
                                    },
                                    style = T.chipXs.copy(color = Q.mutedFg),
                                )
                            }
                            Icon(
                                if (st.designMode == m) Icons.Filled.RadioButtonChecked
                                else Icons.Filled.RadioButtonUnchecked, null,
                                tint = if (st.designMode == m) Q.primaryText else Q.mutedFg,
                                modifier = Modifier.height(19.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDesignMode = false }) { Text("Bağla") } },
        )
    }

    if (showBlocked) {
        AlertDialog(
            onDismissRequest = { showBlocked = false },
            title = { Text("Bloklananlar") },
            text = {
                Column {
                    st.blocked.forEach { id ->
                        val name = engine.dataSource.people.firstOrNull { it.id == id }?.fullName ?: id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(name, Modifier.weight(1f),
                                style = T.bodyXs)
                            TextButton(onClick = { engine.unblockUser(id); showBlocked = false }) {
                                Text("Blokdan çıxar")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlocked = false }) { Text("Bağla") }
            },
        )
    }

    if (showPhone) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPhone = false },
            title = { Text("Hesabı təsdiqlə") },
            text = {
                Column {
                    Text(
                        "Telefon nömrəni əlavə et və SMS kodunu yaz. Təsdiq " +
                            "„Təsdiqlənmiş“ nişanı + ${XpRules.PHONE_VERIFY_XP} Qoşa Coin verir.",
                        style = T.bodySm,
                    )
                    Spacer(Modifier.height(12.dp))
                    QoshulTextField(value = code,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                        hint = "SMS kodu",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        onImeAction = {
                            if (code.trim().length == 6) {
                                engine.verifyPhone(); showPhone = false
                            }
                        })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (code.trim().length == 6) {
                        engine.verifyPhone(); showPhone = false
                    }
                }) { Text("Təsdiqlə") }
            },
            dismissButton = { TextButton(onClick = { showPhone = false }) { Text("Ləğv et") } },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Hesabı silmək?") },
            text = {
                Text(
                    "Bütün məlumatların (profil, tədbirlər, söhbətlər, Qoşa Coin və kuponlar) " +
                        "GDPR və yerli qanunvericiliyə əsasən silinəcək. Bu əməliyyat geri " +
                        "qaytarıla bilməz. Hesab silinəndə sessiya ləğv olunur və bütün " +
                        "lokal məlumatlar təmizlənir.",
                    style = T.bodyXs,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDelete = false
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            runCatching {
                                az.qoshul.app.core.backend.SupabaseBackend.signOut(signOutCtx)
                            }
                            engine.signOut()
                            nav.popAll()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Q.destructive, contentColor = Q.destructiveFg),
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("İmtina") } },
        )
    }

    // — D-055: şəhər seçimi sheeti —
    if (cityOpen) {
        CityPickerSheet(engine = engine, onDismiss = { cityOpen = false })
    }

    // — D.1 (D-170): profil təfərrüatları seçim dialoqları —
    if (statusOpen) {
        OptionDialog(
            title = "Status",
            options = Taxonomy.statuses,
            active = st.status,
            labelOf = { engine.statusLabel(it) },
        ) { engine.setStatus(it); statusOpen = false }
    }
    if (langsOpen) {
        MultiOptionDialog(
            title = "Dillər",
            options = Taxonomy.languageOptions,
            selected = st.languages,
        ) { engine.setLanguages(it); langsOpen = false }
    }
    // D-184 (6.3): e-poçt/şifrə idarəetmə dialoqu (D-167-dən köçürülüb)
    if (emailOpen) {
        AlertDialog(
            onDismissRequest = { emailOpen = false },
            title = { Text(if (hasEmailIdentity == true) "E-poçt və şifrə" else "E-poçt əlavə et") },
            text = {
                Column {
                    if (hasEmailIdentity != true) {
                        Text(
                            "Google ilə daxil olubsan. E-poçt əlavə edib şifrə qoyasan, " +
                                "eyni hesabla e-poçt ilə də daxil ola bilərsən.",
                            style = T.chipXs.copy(color = Q.mutedFg),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    az.qoshul.app.features.auth.QoshulTextField(
                        value = email,
                        onValueChange = { email = it },
                        hint = "ad@example.com",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        ),
                    )
                    Spacer(Modifier.height(9.dp))
                    Button(
                        onClick = { saveMail() },
                        enabled = !mailBusy && email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(Q.rPill),
                    ) {
                        Text(if (hasEmailIdentity == true) "E-poçtu dəyiş" else "E-poçtu əlavə et",
                            style = T.button)
                    }
                    Spacer(Modifier.height(14.dp))
                    az.qoshul.app.features.auth.QoshulTextField(
                        value = newPass,
                        onValueChange = { if (it.length <= 64) newPass = it },
                        hint = if (hasEmailIdentity == true) "Yeni şifrə (ən azı 8)"
                        else "Şifrə qoy (ən azı 8) — e-poçt girişi üçün",
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        ),
                    )
                    Spacer(Modifier.height(9.dp))
                    Button(
                        onClick = { savePassword() },
                        enabled = !mailBusy && newPass.length >= 8,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(Q.rPill),
                    ) {
                        Text(if (hasEmailIdentity == true) "Şifrəni yenilə" else "Şifrəni qeyd et",
                            style = T.button)
                    }
                    mailMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = T.chipXs.copy(color = Q.primaryText))
                    }
                    mailErr?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = T.chipXs.copy(color = Q.destructive))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }
    if (groupOpen) {
        OptionDialog(
            title = "Qrup ölçüsü",
            options = Taxonomy.groupSizes,
            active = st.groupSize,
            labelOf = { it },
        ) { engine.setGroupSize(it); groupOpen = false }
    }
    if (budgetOpen) {
        OptionDialog(
            title = "Büdcə",
            options = Taxonomy.budgets,
            active = st.budget,
            labelOf = { it },
        ) { engine.setBudget(it); budgetOpen = false }
    }

    // — D-056: dost dəvəti dialoqu (INVITE_XP əvvəl heç bir UI-a bağlı deyildi) —
    if (inviteOpen) {
        var invited by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { inviteOpen = false },
            title = { Text("Dostunu dəvət et") },
            text = {
                Text(
                    if (invited)
                        "Dəvət göndərildi (+${XpRules.INVITE_XP} Qoşa Coin hesabına düşdü. " +
                            "Dostun qoşulanda tədbirləri birlikdə planlayın!"
                    else
                        "Qoshul.az-ı dostlarınla bölüş — hər dəvət sənə +${XpRules.INVITE_XP} Qoşa Coin " +
                            "gətirir (bir dəfə). Paylaş:\n\n\"Qoshul.az — Bakıda və bütün Azərbaycanda " +
                            "maraq görüşləri, endirim Coin-ləri və yeni tanışlıqlar.\"",
                    style = T.subtitle.copy(lineHeight = 21.sp),
                )
            },
            confirmButton = {
                if (invited) {
                    TextButton(onClick = { inviteOpen = false }) { Text("Bağla", style = T.button) }
                } else {
                    Button(
                        onClick = {
                            engine.inviteFriend()
                            invited = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Q.primary, contentColor = Q.primaryFg),
                    ) { Text("Paylaş (+${XpRules.INVITE_XP} Qoşa Coin)", style = T.button) }
                }
            },
            dismissButton = {
                TextButton(onClick = { inviteOpen = false }) { Text("İmtina", style = T.button) }
            },
        )
    }

    // — P0 spec (master-spec 4.3): Şərtlər və məxfilik hüquqi mətnləri —
    // GDPR + AR qanunvericiliyi, məkan məlumatları razılığı, davranış qaydaları.
    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            title = { Text("Şərtlər və məxfilik") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("İstifadə Şərtləri", style = T.bodyBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Qoshul.az sosial əlaqə platformasıdır. Hesab yaradaraq " +
                            "18+ olduğunuzu, profil məlumatlarının dəqiq olduğunu və " +
                            "tətbiqin icazə verdiyi fəaliyyətlər üçün məsuliyyəti " +
                            "öz üzərinizə götürürsünüz. Platforma yalnız tanışlıq və " +
                            "birgə fəaliyyət məqsədilə istifadə oluna bilər; ticarət, " +
                            "reklam və saxta profil fəaliyyəti qadağandır. " +
                            "İhlal halında hesab məhdudlaşdırıla bilər.",
                        style = T.bodyXs.copy(lineHeight = 16.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Məxfilik (GDPR və AR qanunvericiliyi)", style = T.bodyBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Şəxsi məlumatlarınız (ad, e-poçt, şəhər, maraqlar) yalnız " +
                            "platformanın əsas funksiyaları üçün işlənir və üçüncü " +
                            "tərəflərlə bölüşülmür. Azərbaycan Respublikasının " +
                            "\u201CŞəxsi məlumatlar haqqında\u201D Qanunu və GDPR Article 5 " +
                            "(qanunilik, minimallaşdırma, dəqiqlik, saxlama məhdudiyyəti) " +
                            "prinsiplərinə uyğun işlənir. Hesabınızı sildikdə məlumatlarınız " +
                            "30 gün ərzində tam silinir. Məlumat surəti üçün: support@qoshul.com.",
                        style = T.bodyXs.copy(lineHeight = 16.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Məkan məlumatlarının emalı", style = T.bodyBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Xəritə funksiyası üçün təxmini mövqeniz işlənir. Mövqe " +
                            "məlumatı yalnız tədbir yaxınlıq filtri və iştirak " +
                            "doğrulaması üçün istifadə olunur; dəqiq ünvan heç vaxt " +
                            "başqa istifadəçiyə göstərilmir. İstənilən vaxt " +
                            "\u201CXəritədə görünmə\u201D tənzimindən bağlaya bilərsiniz.",
                        style = T.bodyXs.copy(lineHeight = 16.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("İctimai tədbirlərdə davranış qaydaları", style = T.bodyBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tədbirlərdə hörmətli davranış, razılaşdırılmam fiziki " +
                            "təmas və zorakılıq yasaqdır. Təşkilatçı tədbiri ləğv " +
                            "və ya iştirakçını çıxara bilər. Təhlükəsizlik " +
                            "məsləhətləri üçün \u201Cİlk görüş məsləhətləri\u201D bölməsinə baxın. " +
                            "Narahatçılıq yaradan davranışı report edin — moderasiya " +
                            "komandası 24 saat ərzində baxır.",
                        style = T.bodyXs.copy(lineHeight = 16.sp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) { Text("Bağla", style = T.button) }
            },
        )
    }

    // — D-115: şirkət hesabı dialoqu („ayarların sonuna yaxın bir yerdə
    // şirkət hesabına keç düyməsi olsun“) — tərəfdaş/şirkət axınının
    // giriş nöqtəsi. D-131 (direktiv 2026-11-11: „biznes yalnız ayarlarda
    // biznes hesabına keç seçənəyində görünsün“): Tərəfdaşlar ekranından
    // köçürülən Biznes rejimi (işçi modu — XP endirimi) ARTIQ buradan
    // açılır — biznes UI-ın yeganə giriş nöqtəsi bu dialoqdur.
    if (showCompany) {
        AlertDialog(
            onDismissRequest = { showCompany = false },
            title = { Text("Şirkət hesabına keç") },
            text = {
                Text(
                    "Şirkət hesabı tərəfdaş məkanlar üçündür: tədbir hostu, stol " +
                        "rezervasiyaları, endirim kuponları və obyekt Coin hesabatı " +
                        "bir paneldən idarə olunur.\n\nTərəfdaş olmaq üçün: " +
                        "qoshul.com/partners ünvanından müraciət et — komanda " +
                        "obyektini təsdiqləyib hesabı aktivləşdirəcək.",
                    style = T.subtitle.copy(lineHeight = 21.sp),
                )
            },
            confirmButton = {
                TextButton(onClick = { showCompany = false; businessOpen = true }) {
                    Text("Biznes rejimini aç", style = T.button)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompany = false }) { Text("Bağla", style = T.button) }
            },
        )
    }

    // D-131: Biznes rejimi sheet-i — yalnız bu ekrandan (şirkət dialoqu → aç)
    if (businessOpen) {
        BusinessModeSheet(engine, onDismiss = { businessOpen = false })
    }
}

@Composable
private fun SectionLabel(title: String) {
    // D-063: bölmə başlığı üstü 6→16dp — kart qrupları arası tənəffüs
    // (vizual audit: "very little top margin — groups feel cramped")
    Text(title, style = T.bodySmBold.copy(color = Q.mutedFg),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 16.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .androidxSettingsCard()
    ) { content() }
}

@Composable
private fun RowItem(icon: ImageVector, label: String, value: String,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.height(17.dp))
        Spacer(Modifier.padding(6.dp))
        Text(label, Modifier.weight(1f),
            style = T.bodyXs)
        Text(value, style = T.bodySm, color = valueColor)
    }
}

@Composable
private fun NavRow(
    icon: ImageVector, label: String, value: String? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .androidxNavRowTap(enabled, onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.height(17.dp))
        Spacer(Modifier.padding(6.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = T.bodyXs)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = T.chipXs.copy(color = Q.mutedFg))
            }
        }
        if (value != null) Text(value, style = T.bodySm)
        // U1.6 (UI-40): chevron yalnız HƏRƏKƏTİ olan sətirdə — ölü (ripple-yalnız)
        // sətirlər „basıla bilən“ kimi görünməsin.
        if (enabled && onClick != null) Icon(Icons.Filled.ChevronRight, null, tint = Q.mutedFg, modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, label: String, value: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
    ) {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.height(17.dp))
        Spacer(Modifier.padding(6.dp))
        Text(label, Modifier.weight(1f),
            style = T.bodyXs)
        Switch(
            checked = value, onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedTrackColor = Q.primaryText),
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .androidxDivider()
    )
}

private fun Modifier.androidxSettingsCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

// U1.6 (UI-40): onClick nullable — null olduqda sətir kliklənmir, ripple YOX
private fun Modifier.androidxNavRowTap(enabled: Boolean, onClick: (() -> Unit)?): Modifier =
    this.then(if (enabled && onClick != null) Modifier.pressable(enabled = enabled, onClick = onClick) else Modifier)

private fun Modifier.androidxDivider(): Modifier = this.then(Modifier.background(Q.border))

/**
 * D.1 (D-170): tək-seçim dialoqu (status/qrup/büdcə) — seçim dərhal
 * serverə yazılır (setter SaveProfileIntent göndərir).
 */
@Composable
private fun OptionDialog(
    title: String,
    options: List<String>,
    active: String,
    labelOf: (String) -> String,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .androidxVisRow { onPick(opt) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            labelOf(opt),
                            style = T.bodySm.copy(
                                fontWeight = if (opt == active) FontWeight.W700 else FontWeight.W400,
                                color = if (opt == active) Q.primaryText else Q.foreground,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (opt == active) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = Q.primaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

/**
 * D.1 (D-170): çox-seçim dialoqu (dillər) — OK basılanda yazılır.
 */
@Composable
private fun MultiOptionDialog(
    title: String,
    options: List<String>,
    selected: List<String>,
    onDone: (List<String>) -> Unit,
) {
    var sel by remember { mutableStateOf(selected.toSet()) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .androidxVisRow { sel = if (opt in sel) sel - opt else sel + opt }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            opt,
                            style = T.bodySm.copy(
                                fontWeight = if (opt in sel) FontWeight.W700 else FontWeight.W400,
                                color = if (opt in sel) Q.primaryText else Q.foreground,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (opt in sel) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = Q.primaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDone(sel.toList()) }) { Text("Tamam") }
        },
        dismissButton = {},
    )
}

private fun Modifier.androidxVisRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.pressable(onClick = onClick))
