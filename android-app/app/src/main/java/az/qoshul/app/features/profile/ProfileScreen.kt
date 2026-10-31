package az.qoshul.app.features.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.badgeDefs
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.motion.countUpAsState
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.RankBadge
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.Segmented
import az.qoshul.app.core.ui.XpBar
import az.qoshul.app.core.ui.BadgeChip
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.ViewHeading
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.auth.QoshulTextField
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Psychology
import az.qoshul.app.features.events.EventCard
import az.qoshul.app.features.events.EventDetailSheet

/**
 * Profil — Flutter profile_screen.dart portu: başlıq, maraqlar, şəxsiyyət
 * testi, nişanlar, 3 tez keçid, statistika (CountUp), XP progress, 3 tab.
 * D-054: nişanlar və maraqlar FlowRow ilə yan-yana düzülür (əvvəl Column —
 * istifadəçi şikayəti: nişanlar alt-alta düşürdü; indi sətir dolunca qırılır).
 * D-055: "Tədbirlərim" kartları artıq detala açılır (əvvəl onOpen={} idi);
 *        onOpenChat — qrup söhbətinə keçid eyni sheetdən işləyir.
 * D-056: şəxsiyyət testi banneri (personality == null) — Route.Quiz.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    engine: AppEngine,
    nav: NavState,
    onFinishActivity: (QoshulEvent) -> Unit = {},
    onOpenChat: (QoshulEvent) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    var editOpen by remember { mutableStateOf(false) }
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }
    val st by engine.state.collectAsState()
    val prog = remember(st.xp) { az.qoshul.app.core.model.XpRules.xpProgress(st.xp) }

    val mine = st.events.filter { it.id in st.joined || it.id in st.pending }
    val saved = st.events.filter { it.id in st.saved }
    val tabList = if (tab == 1) saved else mine

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 13.dp, bottom = 128.dp
        ),
    ) {
        // redaktə / ayarlar
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { editOpen = true }) {
                    Icon(Icons.Filled.Edit, "Profili redaktə et", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { nav.push(Route.Settings) }) {
                    Icon(Icons.Filled.Settings, "Ayarlar", modifier = Modifier.size(20.dp))
                }
            }
        }
        // profil başlığı
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .size(84.dp)
                        .androidxAvatarRing(),
                ) {
                    Image(
                        painter = imagePaint("coffee"), contentDescription = st.profileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).androidxAvatarClip(),
                    )
                }
                Spacer(Modifier.height(15.dp))
                Text(st.profileName, style = T.h1.copy(fontSize = 24.sp))
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(label = st.rank)
                    if (st.phoneVerified) {
                        Spacer(Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Q.secondary, RoundedCornerShape(Q.rPill))
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Filled.Verified, null, tint = Q.primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Təsdiqlənmiş", style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 11.5.sp,
                                fontWeight = FontWeight.W700, color = Q.primary))
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    st.city + " · " + engine.statusLabel(st.status) +
                        (if (st.university.isNotEmpty()) " · ${st.university}"
                         else if (st.workField.isNotEmpty()) " · ${st.workField}" else ""),
                    style = T.subtitle, modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        // maraqlar (D-054: FlowRow — yan-yana, sətir dolanda qırılır)
        if (st.interests.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    st.interests.forEach { i ->
                        Text(i, style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 11.5.sp, color = Q.primary),
                            modifier = Modifier
                                .background(Q.secondary, RoundedCornerShape(Q.rPill))
                                .padding(horizontal = 11.dp, vertical = 7.dp))
                    }
                }
            }
        }
        // şəxsiyyət testi — D-056: nəticə YOXDURSA CTA banneri, VARSA nəticə + zolaqlar
        if (st.personality == null) {
            item {
                Spacer(Modifier.height(16.dp))
                CommunityBanner(
                    icon = Icons.Filled.Psychology,
                    title = "Şəxsiyyət testi",
                    subtitle = "10 sual · 2 dəqiqə. Nəticə profilində görünür, şəkil+bio+maraqlar varsa +30 XP.",
                    onClick = { nav.push(Route.Quiz) },
                    trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                )
            }
        } else {
            item {
                Spacer(Modifier.height(16.dp))
                CommunityBanner(
                    icon = Icons.Filled.AutoAwesome,
                    title = st.personality!!.type,
                    subtitle = st.personality!!.summary,
                )
                Spacer(Modifier.height(10.dp))
                TraitBars(st.personality!!.traits, engine.dataSource.traitLabels)
            }
        }
        // nişanlar (D-054: FlowRow — yan-yana, alt-alta YOX)
        item {
            Spacer(Modifier.height(18.dp))
            SectionHeading(title = "Nişanlar", icon = Icons.Filled.MilitaryTech)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                badgeDefs.forEachIndexed { i, b ->
                    PopIn(delayMs = Motion.STAGGER_STEP * i, key = b.id) {
                        BadgeChip(badge = b, earned = b.id in st.earnedBadges)
                    }
                }
            }
        }
        // tez keçidlər
        item {
            Spacer(Modifier.height(16.dp))
            Row {
                QuickCard(icon = Icons.Filled.TableRestaurant, title = "Rezervlərim",
                    value = "${st.reservations.size}", modifier = Modifier.weight(1f),
                    onClick = { nav.push(Route.Reservations) })
                Spacer(Modifier.width(10.dp))
                QuickCard(icon = Icons.Filled.QrCode, title = "XP kartım",
                    value = "${st.discountXpTotal} XP", modifier = Modifier.weight(1f),
                    onClick = { nav.push(Route.XpCard) })
                Spacer(Modifier.width(10.dp))
                QuickCard(icon = Icons.Filled.ConfirmationNumber, title = "Kuponlarım",
                    value = "${st.coupons.count { it.isActive }}", modifier = Modifier.weight(1f),
                    onClick = { nav.push(Route.Coupons) })
            }
        }
        // statistika
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .androidxStatsBorders()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatCount(st.xp, "Peşəkar XP")
                    StatCount(st.discountXpTotal, "Endirim XP")
                    StatCount(st.joined.size, "Tədbir")
                    Stat(st.rank, "Səviyyə")
                }
            }
        }
        // XP progress
        item {
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(prog.nextName ?: "Maksimum",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 11.5.sp, fontWeight = FontWeight.W600))
                Text(prog.nextName?.let { "${prog.toNext} XP qalıb" } ?: "", style = T.meta)
            }
            Spacer(Modifier.height(6.dp))
            XpBar(progress = prog.pct / 100f, height = 4.dp)
        }
        // tablar
        item {
            Segmented(
                labels = listOf("Tədbirlərim", "Yadda saxlanılanlar", "Nailiyyətlər"),
                active = tab, onChanged = { tab = it },
            )
        }
        if (tab == 2) {
            item {
                CommunityBanner(
                    icon = Icons.Filled.EmojiEvents, title = st.rank,
                    subtitle = "${st.xp} XP topladın. Növbəti görüşə qoşul, yeni səviyyəyə yüksəl!",
                )
            }
            items(badgeDefs.size) { i ->
                val b = badgeDefs[i]
                val earned = b.id in st.earnedBadges
                Row(Modifier.padding(bottom = 8.dp)) {
                    Icon(
                        if (earned) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                        tint = if (earned) Q.primary else Q.mutedFg, modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${b.name} — ${b.description}",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp, lineHeight = 19.sp,
                            color = if (earned) Q.foreground else Q.mutedFg))
                }
            }
        } else if (tabList.isEmpty()) {
            item {
                EmptyState(icon = Icons.Filled.Event,
                    text = "Burada hələ tədbir yoxdur.\nYeni bir plana qoşulmağa nə deyirsən?")
            }
        } else {
            items(tabList.size) { i ->
                val e = tabList[i]
                EventCard(
                    event = e, joined = e.id in st.joined, pending = e.id in st.pending,
                    saved = e.id in st.saved, showLevel = false,
                    onToggleSave = { engine.toggleSave(e.id) },
                    // D-055: ölü klik YOX — kart tədbir detal sheetini açır
                    onOpen = { detailEvent = e },
                    onJoin = { engine.join(e.id) },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // — D-055: tədbir detal sheeti (profil kartlarından) —
    detailEvent?.let { ev ->
        EventDetailSheet(
            engine = engine, event = ev,
            onDismiss = { detailEvent = null },
            onFinishActivity = onFinishActivity,
            onOpenChat = onOpenChat,
        )
    }

    if (editOpen) {
        var name by remember { mutableStateOf(st.profileName) }
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text("Profili redaktə et") },
            text = {
                Column {
                    Text("Səni necə çağıraq?", style = T.subtitle)
                    Spacer(Modifier.height(8.dp))
                    QoshulTextField(value = name, onValueChange = { name = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) engine.setProfileName(name.trim())
                    editOpen = false
                }) { Text("Yadda saxla", style = T.button) }
            },
            dismissButton = {
                TextButton(onClick = { editOpen = false }) { Text("Ləğv et", style = T.button) }
            },
        )
    }
}

@Composable
private fun StatCount(value: Int, label: String) {
    val shown = countUpAsState(value)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$shown", style = T.stat)
        Spacer(Modifier.height(3.dp))
        Text(label, style = T.meta)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.stat)
        Spacer(Modifier.height(3.dp))
        Text(label, style = T.meta)
    }
}

@Composable
private fun QuickCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, value: String, modifier: Modifier, onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .androidxQuickBg()
            .clickable(onClick = onClick)
            .padding(13.dp),
    ) {
        Icon(icon, null, tint = Q.primary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700))
            Text(value, style = T.stat.copy(fontSize = 17.sp))
        }
    }
}

@Composable
private fun TraitBars(traits: Map<String, Int>, labels: Map<String, String>) {
    Column {
        labels.forEach { (key, label) ->
            traits[key]?.let { v ->
                Column(Modifier.padding(bottom = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 11.5.sp, color = Q.mutedFg))
                        Text("$v", style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 11.5.sp, fontWeight = FontWeight.W600))
                    }
                    Spacer(Modifier.height(4.dp))
                    XpBar(progress = v / 100f, height = 4.dp)
                }
            }
        }
    }
}

private fun Modifier.androidxAvatarRing(): Modifier = this.then(
    Modifier
        .background(Q.card, CircleShape)
        .border(4.dp, Q.card, CircleShape)
        .androidxGreenRing()
)

private fun Modifier.androidxGreenRing(): Modifier = this.then(
    Modifier.border(2.dp, Q.primary, CircleShape)
)

private fun Modifier.androidxAvatarClip(): Modifier =
    this.then(Modifier.androidxClipCircle())

private fun Modifier.androidxStatsBorders(): Modifier = this.then(
    Modifier
        .androidxStatTopBorder()
        .androidxStatBottomBorder()
)

private fun Modifier.androidxStatTopBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border)
)

private fun Modifier.androidxStatBottomBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border)
)

private fun Modifier.androidxClipCircle(): Modifier =
    this.then(Modifier.clip(CircleShape))

private fun Modifier.androidxQuickBg(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)
