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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
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
 */
@Composable
fun ProfileEditScreen(engine: AppEngine, nav: NavState) {
    val st by engine.state.collectAsStateWithLifecycle()
    var first by remember { mutableStateOf(st.firstName) }
    var last by remember { mutableStateOf(st.lastName) }
    var avatar by remember { mutableStateOf(st.avatar) }

    // imagePaint açarları + görünən etiketlər (brend şəkil dəsti)
    val avatarOptions = listOf(
        "coffee" to "Qəhvə",
        "padel" to "Padel",
        "tech" to "Texnologiya",
        "friends_baku" to "Bakı",
    )
    val canSave = first.isNotBlank()

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
            Text("Profil şəkli", style = T.label)
            Spacer(Modifier.height(9.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                avatarOptions.forEach { (key, label) ->
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
                            contentDescription = label,
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
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    engine.setProfileFullName(first.trim(), last.trim())
                    engine.setAvatar(avatar)
                    nav.pop()
                },
                enabled = canSave,
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
}

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
