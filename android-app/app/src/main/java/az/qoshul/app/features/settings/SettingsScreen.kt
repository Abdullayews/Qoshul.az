package az.qoshul.app.features.settings

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
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.motion.pressable
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.data.AppConfig
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.DesignColor
import az.qoshul.app.core.theme.DesignMode
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
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
    var showVisibility by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPhone by remember { mutableStateOf(false) }
    // D-115: şirkət hesabı dialoqu
    var showCompany by remember { mutableStateOf(false) }
    var cityOpen by remember { mutableStateOf(false) }
    var inviteOpen by remember { mutableStateOf(false) }
    // D-119: dizayn seçim dialoqları (rəng + mod)
    var showDesignColor by remember { mutableStateOf(false) }
    var showDesignMode by remember { mutableStateOf(false) }

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
                    RowItem(Icons.Filled.WorkspacePremium, "Səviyyə", st.rank)
                    DividerLine()
                    // D-056: dost dəvəti — INVITE_XP (+100) əvvəl heç bir UI-a bağlı deyildi
                    NavRow(Icons.Filled.People, "Dostunu dəvət et", value = "+${XpRules.INVITE_XP} XP") {
                        inviteOpen = true
                    }
                    DividerLine()
                    RowItem(
                        Icons.Filled.Tune, "Məlumat rejimi", AppConfig.modeLabel,
                        valueColor = if (AppConfig.isPlaceholder) Q.accentFg else Q.primaryText,
                    )
                    DividerLine()
                    NavRow(
                        Icons.Filled.VerifiedUser, "Hesabı təsdiqlə (telefon)",
                        if (st.phoneVerified) "Təsdiqlənmiş (+${XpRules.PHONE_VERIFY_XP} XP)"
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
                        "XP-nin müddəti yoxdur — balans = qazanma − xərcləmə.",
                    style = T.chipXs.copy(color = Q.mutedFg),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            item { SectionLabel("Bildirişlər") }
            item {
                SettingsCard {
                    NavRow(Icons.Filled.Tune, "Bildiriş tercihleri", null,
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
                        subtitle = "Native (Compose) — ${AppConfig.modeLabel}")
                    DividerLine()
                    // D-117: xəritə krediti — xəritə üzərindəki SDK atribusiya
                    // zolağı gizlədilib (overlay ləğv); OSM/OpenFreeMap lisenziya
                    // krediti burada, Haqqında-da qalır.
                    NavRow(Icons.Filled.Public, "Xəritə məlumatları",
                        value = "© OpenStreetMap contributors",
                        subtitle = "OpenFreeMap × OpenMapTiles tile-ları")
                    DividerLine()
                    NavRow(Icons.Filled.Description, "Şərtlər və məxfilik", null)
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
                        engine.signOut()
                        nav.popAll()
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
                            "\"Təsdiqlənmiş\" nişanı + ${XpRules.PHONE_VERIFY_XP} XP verir.",
                        style = T.bodySm,
                    )
                    Spacer(Modifier.height(12.dp))
                    QoshulTextField(value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        hint = "6 rəqəmli kod (demo: istənilən)")
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
                    "Bütün məlumatların (profil, tədbirlər, söhbətlər, XP və kuponlar) " +
                        "GDPR və yerli qanunvericiliyə əsasən silinəcək. Bu əməliyyat geri " +
                        "qaytarıla bilməz.\n\nDemo: backend mərhələsində aktivləşəcək.",
                    style = T.bodyXs,
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDelete = false },
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

    // — D-056: dost dəvəti dialoqu (INVITE_XP əvvəl heç bir UI-a bağlı deyildi) —
    if (inviteOpen) {
        var invited by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { inviteOpen = false },
            title = { Text("Dostunu dəvət et") },
            text = {
                Text(
                    if (invited)
                        "Dəvət göndərildi (+${XpRules.INVITE_XP} XP hesabına düşdü. " +
                            "Dostun qoşulanda tədbirləri birlikdə planlayın!"
                    else
                        "Qoshul.az-ı dostlarınla bölüş — hər dəvət sənə +${XpRules.INVITE_XP} XP " +
                            "gətirir (bir dəfə). Paylaş:\n\n\"Qoshul.az — Bakıda və bütün Azərbaycanda " +
                            "maraq görüşləri, endirim XP-si və yeni tanışlıqlar.\"",
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
                    ) { Text("Paylaş (+${XpRules.INVITE_XP} XP)", style = T.button) }
                }
            },
            dismissButton = {
                TextButton(onClick = { inviteOpen = false }) { Text("İmtina", style = T.button) }
            },
        )
    }

    // — D-115: şirkət hesabı dialoqu („ayarların sonuna yaxın bir yerdə
    // şirkət hesabına keç düyməsi olsun“) — tərəfdaş/şirkət axınının
    // giriş nöqtəsi; beta mərhələsində məlumatlandırma dialoqudur.
    if (showCompany) {
        AlertDialog(
            onDismissRequest = { showCompany = false },
            title = { Text("Şirkət hesabına keç") },
            text = {
                Text(
                    "Şirkət hesabı tərəfdaş məkanlar üçündür: tədbir hostu, stol " +
                        "rezervasiyaları, endirim kuponları və obyekt XP hesabatı " +
                        "bir paneldən idarə olunur.\n\nTərəfdaş olmaq üçün: " +
                        "qoshul.az/partners ünvanından müraciət et — komanda " +
                        "obyektini təsdiqləyib hesabı aktivləşdirəcək.",
                    style = T.subtitle.copy(lineHeight = 21.sp),
                )
            },
            confirmButton = {
                TextButton(onClick = { showCompany = false }) { Text("Bağla", style = T.button) }
            },
        )
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

private fun Modifier.androidxVisRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.pressable(onClick = onClick))
