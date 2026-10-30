package az.qoshul.app.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.NotifType
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.ViewHeading

/** Bildiriş mərkəzi + tip tercihleri — Flutter notifications_screen.dart portu (ROL H). */
@Composable
fun NotificationsScreen(engine: AppEngine, nav: NavState) {
    var showPrefs by remember { mutableStateOf(false) }
    val st by engine.state.collectAsState()

    PushScreenScaffold(
        title = "Bildirişlər",
        onBack = { nav.pop() },
        actions = {
            TextButton(onClick = { showPrefs = !showPrefs }) {
                Text(if (showPrefs) "Siyahı" else "Tercihlər", style = T.button)
            }
        },
    ) { mod ->
        if (showPrefs) {
            PrefsView(engine, mod)
        } else {
            LazyColumn(
                modifier = mod.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
                ),
            ) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        Text("${engine.unreadCount} oxunmamış", style = T.subtitle,
                            modifier = Modifier.weight(1f))
                        if (engine.unreadCount > 0) {
                            TextButton(onClick = { engine.markAllNotificationsRead() }) {
                                Text("Hamısını oxunmuş et")
                            }
                        }
                    }
                }
                if (st.notifications.isEmpty()) {
                    item {
                        EmptyState(icon = Icons.Filled.NotificationsNone, text = "Hələ bildiriş yoxdur.")
                    }
                }
                itemsIndexed(st.notifications) { i, n ->
                    StaggerIn(i.coerceIn(0, 8)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .androidxNotifCard(n.read)
                                .padding(13.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(notifIcon(n.type), null, tint = Q.primary,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.padding(6.dp))
                                Text(n.title, Modifier.weight(1f),
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = JakartaSans, fontSize = 12.sp,
                                        fontWeight = FontWeight.W700))
                                Text("%02d:%02d".format(n.at.hour, n.at.minute), style = T.metaSm)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(n.body, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 11.5.sp,
                                lineHeight = 18.sp, color = Q.mutedFg))
                            Spacer(Modifier.height(4.dp))
                            Text(n.type.label, style = T.metaSm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefsView(engine: AppEngine, mod: Modifier) {
    LazyColumn(
        modifier = mod.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
        ),
    ) {
        item {
            ViewHeading(
                title = "Bildiriş tercihleri",
                subtitle = "Hər tip ayrıca söndürülə bilər. Marketinq bildirişləri " +
                    "gecə 23:00-08:00 göndərilmir.",
            )
        }
        items(NotifType.entries.size) { i ->
            val t = NotifType.entries[i]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .androidxPrefCard()
                    .padding(start = 12.dp, end = 4.dp),
            ) {
                Icon(notifIcon(t), null, tint = Q.mutedFg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.padding(6.dp))
                Text(t.label, Modifier.weight(1f),
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 12.sp))
                Switch(
                    checked = engine.notifPrefs.isEnabled(t),
                    onCheckedChange = { engine.toggleNotifPref(t, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Q.primary),
                )
            }
        }
    }
}

private fun notifIcon(t: NotifType): ImageVector = when (t) {
    NotifType.MATCH -> Icons.Filled.Favorite
    NotifType.MESSAGE -> Icons.Filled.ChatBubbleOutline
    NotifType.EVENT_JOIN -> Icons.Filled.GroupAdd
    NotifType.EVENT_APPROVAL -> Icons.Filled.CheckCircleOutline
    NotifType.EVENT_REMINDER -> Icons.Filled.Event
    NotifType.RESV_UPDATE -> Icons.Filled.TableRestaurant
    NotifType.RESV_REMINDER -> Icons.Filled.Schedule
    NotifType.LEVEL_UP -> Icons.Filled.EmojiEvents
    NotifType.COUPON_EXPIRING -> Icons.Filled.ConfirmationNumber
    NotifType.NEARBY_EVENT -> Icons.Filled.NearMe
    NotifType.MARKETING -> Icons.Filled.Campaign
}

private fun Modifier.androidxNotifCard(read: Boolean): Modifier = this.then(
    Modifier
        .background(if (read) Q.card else Q.secondary, RoundedCornerShape(Q.rLg.dp))
        .border(
            1.2.dp.takeIf { !read } ?: 1.dp,
            if (read) Q.border else Q.primary,
            RoundedCornerShape(Q.rLg.dp),
        )
)

private fun Modifier.androidxPrefCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxPrefBorder()
)

private fun Modifier.androidxPrefBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)
