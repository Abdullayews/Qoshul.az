package az.qoshul.app.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
// Apple ikonu: material-icons-da yoxdur — PlaceHolder istifadə olunur
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GMobiledata
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.AppConfig
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.FormFieldX
import az.qoshul.app.core.ui.LogoMark
import az.qoshul.app.core.ui.Wordmark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Daxil ol / Qeydiyyat — Flutter auth_screen.dart portu (ROL B, Addım 2).
 * Mock: "Google ilə davam et", "Apple ilə davam et", email + şifrə →
 * 6 rəqəmli təsdiq kodu (istənilən 6 rəqəm).
 *
 * D-050 (PLACEHOLDER rejimi): [engine] verildikdə vəhdət hardcoded DEMO
 * hesabı düyməsi göstərilir — bir toxunuşla dolu profillə tam tətbiq açılır
 * (bütün funksiyaların görünməsi üçün). Manual axın da qalır (onboarding
 * sınağı üçün).
 *
 * Qeyd (qayda ③): bu MOCK autentifikasiyadır — heç bir real təhlükəsizlik
 * iddiası yoxdur; backend fazasında custom JWT (product-spec B) gələcək.
 */
private enum class AuthMode { ENTER, UP, FORGOT, CODE }

@Composable
fun AuthScreen(onSignedIn: () -> Unit, engine: AppEngine? = null) {
    var mode by remember { mutableStateOf(AuthMode.ENTER) }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    /** D-050: vahid demo hesabı — anında dolu profillə daxil ol. */
    fun demoSignIn() {
        scope.launch {
            busy = true
            delay(450)
            engine?.signInDemo()
            busy = false
            onSignedIn()
        }
    }

    fun submit() {
        error = null
        msg = null
        scope.launch {
            busy = true
            delay(800)
            when (mode) {
                AuthMode.CODE -> {
                    if (code.length != 6 || code.toIntOrNull() == null) {
                        error = "Kod 6 rəqəmdən ibarət olmalıdır."
                        busy = false
                    } else {
                        busy = false
                        onSignedIn()
                    }
                }
                AuthMode.UP -> {
                    if (pass.length < 8) {
                        error = "Şifrə ən azı 8 simvol olmalıdır."
                        busy = false
                    } else if (pass != repeat) {
                        error = "Şifrələr eyni deyil."
                        busy = false
                    } else {
                        mode = AuthMode.CODE
                        msg = "E-poçtuna 6 rəqəmli təsdiq kodu göndərildi. (Demo: istənilən 6 rəqəm)"
                        busy = false
                    }
                }
                AuthMode.FORGOT -> {
                    msg = "Şifrəni yeniləmək üçün link e-poçtuna göndərildi."
                    busy = false
                }
                AuthMode.ENTER -> {
                    busy = false
                    onSignedIn()
                }
            }
        }
    }

    fun social() {
        scope.launch {
            busy = true
            delay(900)
            busy = false
            onSignedIn()
        }
    }

    val title = when (mode) {
        AuthMode.ENTER -> "Xoş gəldin!"
        AuthMode.UP -> "Hesab yarat"
        AuthMode.CODE -> "E-poçtunu təsdiqlə"
        AuthMode.FORGOT -> "Şifrəni unutmusan?"
    }
    val subtitle = when (mode) {
        AuthMode.CODE -> "$email ünvanına göndərilən kodu yaz."
        AuthMode.FORGOT -> "E-poçtunu yaz, sənə yeniləmə linki göndərək."
        else -> "Yeni insanlara, yeni planlara qoşul."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Q.background)
            // D-051 fix: məzmun status barın altında çəkilirdi — inset əlavə olundu
            .statusBarsPadding()
            // D-053 fix: aşağıdakı məzmun (demo düyməsi, giriş düyməsi) alt sistem
            // barının altında qalırdı — navbar inseti əlavə olundu
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .imePadding(),
    ) {
        Spacer(Modifier.height(44.dp))
        LogoMark(size = 64.dp)
        Spacer(Modifier.height(18.dp))
        Wordmark(fontSize = 27)
        Spacer(Modifier.height(20.dp))
        Text(title, style = T.h1)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = T.subtitle)
        Spacer(Modifier.height(22.dp))

        // — D-050: vahid hardcoded demo hesabı (yalnız PLACEHOLDER rejimi) —
        val demo = AppConfig.DemoAccount
        if (AppConfig.isPlaceholder && engine != null) {
            Button(
                onClick = { if (!busy) demoSignIn() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Q.primary, contentColor = Q.primaryFg),
            ) {
                Icon(Icons.Filled.Bolt, null, modifier = Modifier.height(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Demo hesabı ilə davam et", style = T.button)
                    Text(
                        demo.EMAIL,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 10.5.sp, color = Q.primaryFg),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Demo rejimi: bütün funksiyalar nümunə məlumatla doludur.",
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 11.sp, color = Q.mutedFg),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
        }

        if (mode == AuthMode.CODE) {
            FormFieldX("6 rəqəmli təsdiq kodu") {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 20.sp,
                        fontWeight = FontWeight.W700, letterSpacing = 6.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = qoshulTextFieldColors(),
                )
            }
        } else {
            OutlinedButton(
                onClick = { if (!busy) social() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rPill),
                border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Q.card, contentColor = Q.foreground
                ),
            ) {
                Icon(Icons.Filled.GMobiledata, null, modifier = Modifier.height(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Google ilə davam et", style = T.button)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { if (!busy) social() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rPill),
                border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Q.card, contentColor = Q.foreground
                ),
            ) {
                Icon(az.qoshul.app.core.ui.appleIcon, null, modifier = Modifier.height(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apple ilə davam et", style = T.button)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "və ya",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 12.5.sp, color = Q.mutedFg
                ),
            )
            Spacer(Modifier.height(6.dp))
            if (mode == AuthMode.UP) {
                FormFieldX("Ad və soyad") {
                    QoshulTextField(value = name, onValueChange = { name = it })
                }
            }
            FormFieldX("E-poçt") {
                QoshulTextField(
                    value = email, onValueChange = { email = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            }
            if (mode != AuthMode.FORGOT) {
                FormFieldX("Şifrə") {
                    QoshulTextField(
                        value = pass, onValueChange = { pass = it },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
            if (mode == AuthMode.UP) {
                FormFieldX("Şifrəni təkrarla") {
                    QoshulTextField(
                        value = repeat, onValueChange = { repeat = it },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 12.5.sp, color = Q.destructive
                ),
            )
        }
        msg?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 12.5.sp,
                    lineHeight = 20.sp, color = Q.mutedFg,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (!busy) submit() },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(Q.rPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = Q.primary, contentColor = Q.primaryFg
            ),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp, color = Q.primaryFg,
                    modifier = Modifier.height(16.dp).width(16.dp),
                )
            } else {
                Text(
                    when (mode) {
                        AuthMode.ENTER -> "Daxil ol"
                        AuthMode.UP -> "Qeydiyyatdan keç"
                        AuthMode.CODE -> "Təsdiqlə"
                        AuthMode.FORGOT -> "Link göndər"
                    },
                    style = T.button,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
        ) {
            when (mode) {
                AuthMode.ENTER -> {
                    TextButton(onClick = { mode = AuthMode.FORGOT }) {
                        Text("Şifrəni unutmusan?", style = qTextButton())
                    }
                    TextButton(onClick = { mode = AuthMode.UP; msg = null }) {
                        Text("Hesabın yoxdur? Qeydiyyatdan keç", style = qTextButton())
                    }
                }
                AuthMode.CODE -> {
                    TextButton(onClick = { mode = AuthMode.UP; msg = null }) {
                        Text("Kod gəlmədi? Yenidən göndər", style = qTextButton())
                    }
                }
                else -> {
                    TextButton(onClick = { mode = AuthMode.ENTER; msg = null }) {
                        Text("Hesabın var? Daxil ol", style = qTextButton())
                    }
                }
            }
        }
    }
}

private fun qTextButton() = androidx.compose.ui.text.TextStyle(
    fontFamily = JakartaSans, fontSize = 12.sp, color = Q.primary
)

/** Vahid mətn sahəsi (theme inputDecorationTheme portu). */
@Composable
fun QoshulTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxLines: Int = 1,
    hint: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = maxLines == 1,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        placeholder = hint?.let {
            { Text(it, style = T.input.copy(color = Q.mutedFg)) }
        },
        textStyle = T.input,
        shape = RoundedCornerShape(Q.rPill),
        colors = qoshulTextFieldColors(),
    )
}

/** Theme InputDecorationTheme portu: ağ fon, border, fokus yaşıl. */
@Composable
fun qoshulTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Q.card,
    unfocusedContainerColor = Q.card,
    focusedBorderColor = Q.primary,
    unfocusedBorderColor = Q.border,
    focusedTextColor = Q.foreground,
    unfocusedTextColor = Q.foreground,
    focusedPlaceholderColor = Q.mutedFg,
    unfocusedPlaceholderColor = Q.mutedFg,
)
