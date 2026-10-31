package az.qoshul.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.data.AppConfig
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.Route
import az.qoshul.app.features.auth.QoshulTextField
import kotlinx.coroutines.launch

/**
 * Ayarlar — Flutter settings_screen.dart portu (ROL TAB 5):
 * Hesab / Məxfilik (D-027 3-seçimli) / Bildirişlər / Təhlükəsizlik / sil-çıxış.
 * A1 düzəlişi: "Haqqında" versiyası BuildConfig-dən oxunur.
 */
@Composable
fun SettingsScreen(engine: AppEngine, nav: NavState) {
    val st by engine.state.collectAsState()
    var showVisibility by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPhone by remember { mutableStateOf(false) }

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
                    RowItem(Icons.Filled.LocationCity, "Şəhər", st.city)
                    DividerLine()
                    RowItem(Icons.Filled.WorkspacePremium, "Səviyyə", st.rank)
                    DividerLine()
                    RowItem(
                        Icons.Filled.Tune, "Məlumat rejimi", AppConfig.modeLabel,
                        valueColor = if (AppConfig.isPlaceholder) Q.accentFg else Q.primary,
                    )
                    DividerLine()
                    NavRow(
                        Icons.Filled.VerifiedUser, "Hesabı təsdiqlə (telefon)",
                        if (st.phoneVerified) "Təsdiqlənmiş ✓ (+${XpRules.PHONE_VERIFY_XP} XP)"
                        else "Təsdiq edilməyib",
                        valueColor = if (st.phoneVerified) Q.primary else androidx.compose.ui.graphics.Color.Unspecified,
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

            // — SERVER_ONLY (D-050): server bağlantı kartı — URL dəyişdirilə bilər —
            if (AppConfig.isServerOnly && engine.server != null) {
                item { SectionLabel("Server") }
                item { ServerSettingsCard(engine) }
            }
            item {
                Text(
                    "Dəqiq koordinatın heç kimə göstərilmir — yalnız təxmini məsafə. " +
                        "XP-nin müddəti yoxdur — balans = qazanma − xərcləmə.",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 11.5.sp,
                        lineHeight = 18.sp, color = Q.mutedFg),
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
                    NavRow(Icons.Filled.Description, "Şərtlər və məxfilik", null)
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
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
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Q.card, contentColor = Q.foreground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                ) {
                    Icon(Icons.Filled.Logout, null, modifier = Modifier.height(16.dp))
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
                                tint = if (st.mapVisibility == v) Q.primary else Q.mutedFg,
                                modifier = Modifier.height(19.dp),
                            )
                            Spacer(Modifier.padding(6.dp))
                            Column {
                                Text(v.label, style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 13.sp,
                                    fontWeight = FontWeight.W700))
                                Text(v.hint, style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 11.5.sp, color = Q.mutedFg))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
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
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 12.sp))
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
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp, lineHeight = 21.sp),
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
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 12.sp, lineHeight = 20.sp),
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDelete = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Q.destructive, contentColor = androidx.compose.ui.graphics.Color.White),
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Imtina") } },
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(title, style = androidx.compose.ui.text.TextStyle(
        fontFamily = JakartaSans, fontSize = 12.5.sp, fontWeight = FontWeight.W700,
        color = Q.mutedFg),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 6.dp))
}

/**
 * SERVER_ONLY (D-050): server URL konfiqurasiyası + canlı bağlantı yoxlaması.
 * Lokal backend sınağı üçün (məs. http://10.0.2.2:4000) ünvan dəyişilə bilər.
 */
@Composable
private fun ServerSettingsCard(engine: AppEngine) {
    val api = engine.server ?: return
    val conn by api.connection.collectAsState()
    var url by remember { mutableStateOf(api.baseUrl) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
        Text("Server ünvanı", style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700))
        Spacer(Modifier.height(6.dp))
        QoshulTextField(value = url, onValueChange = { url = it },
            hint = "https://api.qoshul.az")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (label, color) = when (conn) {
                az.qoshul.app.core.data.ConnectionState.ONLINE -> "Server hazır" to Q.primary
                az.qoshul.app.core.data.ConnectionState.CHECKING -> "Yoxlanılır..." to Q.mutedFg
                az.qoshul.app.core.data.ConnectionState.OFFLINE -> "Serverə qoşulunmur" to Q.destructive
                az.qoshul.app.core.data.ConnectionState.UNKNOWN -> "Vəziyyət bilinmir" to Q.mutedFg
            }
            Text("●", style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 13.sp, color = color))
            Spacer(Modifier.padding(5.dp))
            Text(label, style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 12.sp, color = color),
                modifier = Modifier.weight(1f))
            OutlinedButton(
                enabled = !checking && url.isNotBlank(),
                onClick = {
                    scope.launch {
                        checking = true
                        api.baseUrl = url
                        api.checkConnection()
                        checking = false
                    }
                },
                shape = RoundedCornerShape(Q.rMd.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Q.card, contentColor = Q.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Q.primary),
            ) {
                if (checking) {
                    androidx.compose.material3.CircularProgressIndicator(
                        strokeWidth = 2.dp, color = Q.primary,
                        modifier = Modifier.height(14.dp).width(14.dp))
                } else {
                    Text("Yadda saxla və yoxla", style = T.button.copy(fontSize = 11.5.sp))
                }
            }
        }
    }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.height(17.dp))
        Spacer(Modifier.padding(6.dp))
        Text(label, Modifier.weight(1f),
            style = androidx.compose.ui.text.TextStyle(fontFamily = JakartaSans, fontSize = 12.sp))
        Text(value, style = T.meta.copy(fontSize = 12.5.sp), color = valueColor)
    }
}

@Composable
private fun NavRow(
    icon: ImageVector, label: String, value: String? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .androidxNavRowTap(enabled, onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.height(17.dp))
        Spacer(Modifier.padding(6.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 12.sp))
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 11.5.sp, color = Q.mutedFg))
            }
        }
        if (value != null) Text(value, style = T.meta.copy(fontSize = 12.5.sp))
        if (enabled) Icon(Icons.Filled.ChevronRight, null, tint = Q.mutedFg, modifier = Modifier.height(18.dp))
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
            style = androidx.compose.ui.text.TextStyle(fontFamily = JakartaSans, fontSize = 12.sp))
        Switch(
            checked = value, onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedTrackColor = Q.primary),
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

private fun Modifier.androidxNavRowTap(enabled: Boolean, onClick: () -> Unit): Modifier =
    this.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)

private fun Modifier.androidxDivider(): Modifier = this.then(Modifier.background(Q.border))

private fun Modifier.androidxVisRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
