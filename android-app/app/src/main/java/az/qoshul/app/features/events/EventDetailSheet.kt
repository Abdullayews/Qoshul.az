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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WhereToVote
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.ShadedImage
import az.qoshul.app.features.auth.QoshulTextField
import az.qoshul.app.core.ui.SheetScaffold

/**
 * Tədbir detalı — Flutter event_detail_sheet.dart portu (bottom sheet).
 * A3 düzəlişi: "Qrup söhbətinə keç" artıq real keçid callback-i alır.
 * D-050: "Yol göstər" aktivləşdi (xəritə fazası gəldi — D-048 MapLibre):
 * [onShowOnMap] xəritə tabına keçib tədbir pininə uçur.
 */
@Composable
fun EventDetailSheet(
    engine: AppEngine,
    event: QoshulEvent,
    onDismiss: () -> Unit,
    onFinishActivity: (QoshulEvent) -> Unit = {},
    onShowOnMap: (QoshulEvent) -> Unit = {},
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var reviewSent by remember { mutableStateOf(false) }
    val st by engine.state.collectAsState()
    val isJoined = event.id in st.joined
    val isPending = event.id in st.pending

    SheetScaffold(onDismiss = onDismiss) {
        // D-051 fix: daxili verticalScroll silindi — SheetScaffold-ın öz scrollable
        // Column-u içində nested scroll = IllegalStateException (infinity constraints).
        Column(Modifier.padding(horizontal = 20.dp)) {
            ShadedImage(assetKey = event.image, height = 180.dp,
                modifier = Modifier.androidxDetailClip())
            Spacer(Modifier.height(14.dp))
            Text(event.title, style = T.h2)
            Spacer(Modifier.height(4.dp))
            Text(
                "${event.host} tərəfindən təşkil olunur" +
                    if (event.approval) " · qoşulma təsdiq tələb edir" else "",
                style = T.subtitle,
            )
            Spacer(Modifier.height(10.dp))
            Text(event.description, style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 12.sp, lineHeight = 22.sp, color = Q.mutedFg))
            Spacer(Modifier.height(12.dp))
            FactsGrid(event)

            // D-026 (L.3): təşkilatçı bitirmə axını
            if (engine.canFinishActivity(event)) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDismiss(); onFinishActivity(event) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                ) {
                    Icon(Icons.Filled.WorkspacePremium, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aktiviteyi bitir və XP payla (+${XpRules.ORGANIZER_XP})", style = T.button)
                }
            } else if (engine.isActivityFinished(event.id)) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {}, enabled = false,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Q.secondary, disabledContentColor = Q.primary),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Aktivite bitib — XP paylandı (+${XpRules.ORGANIZER_XP} təşkilatçı)", style = T.button)
                }
            }

            if (isJoined && !event.past) {
                Spacer(Modifier.height(8.dp))
                if (event.id in st.checkedIn) {
                    Button(
                        onClick = {}, enabled = false,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Q.secondary, disabledContentColor = Q.primary),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check-in edilib (+${XpRules.CHECK_IN_XP} XP qazanıldı)", style = T.button)
                    }
                } else {
                    Button(
                        onClick = { engine.checkInEvent(event.id) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(Q.rMd.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                    ) {
                        Icon(Icons.Filled.WhereToVote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Yerində check-in et (+${XpRules.CHECK_IN_XP} XP)", style = T.button)
                    }
                }
            }
            if (isJoined && event.past && !reviewSent) {
                Spacer(Modifier.height(14.dp))
                Text("Tədbiri qiymətləndir", style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W600))
                Spacer(Modifier.height(8.dp))
                Row {
                    (1..5).forEach { n ->
                        Icon(
                            Icons.Filled.Star, null,
                            tint = if (n <= rating) Q.primary else Q.mutedFg,
                            modifier = Modifier
                                .size(28.dp)
                                .androidxTapStar { rating = n }
                                .padding(3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                QoshulTextField(value = comment, onValueChange = { comment = it },
                    maxLines = 2, hint = "Təcrübən bir cümlə ilə bölüş (istəyə görə)")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { engine.addReview(event.id, rating, comment); reviewSent = true },
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                ) { Text("Rəyi göndər", style = T.button) }
            }
            if (reviewSent) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = Q.primary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rəyin üçün təşəkkürlər! (+${XpRules.REVIEW_XP} XP)",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp, color = Q.mutedFg))
                }
            }
            if (event.format != "Onlayn") {
                Spacer(Modifier.height(12.dp))
                // D-050: "Yol göstər" AKTİV — xəritəyə keçib tədbir pininə uçur
                // (A3-ün "xəritə fazasında" qeydi D-048 MapLibre ilə bağlandı)
                OutlinedButton(
                    onClick = { onDismiss(); onShowOnMap(event) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Q.card, contentColor = Q.foreground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                ) {
                    Icon(Icons.Filled.LocationOn, null, tint = Q.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Yol göstər (${event.distance} km)", style = T.button)
                }
            }
            if (isJoined && !event.past) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { engine.leave(event.id); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Q.card, contentColor = Q.foreground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                ) {
                    Icon(Icons.Filled.Logout, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tədbirdən çıx", style = T.button)
                }
            }
            if (!event.past) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { engine.join(event.id) },
                    enabled = !isJoined && !isPending && !event.isFull,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Q.primary, contentColor = Q.primaryFg,
                        disabledContainerColor = Q.secondary, disabledContentColor = Q.primary),
                ) {
                    when {
                        isJoined -> {
                            Icon(Icons.Filled.NorthEast, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Qoşuldun — qrup söhbətinə bax", style = T.button)
                        }
                        isPending -> {
                            Icon(Icons.Filled.HourglassEmpty, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sorğun təşkilatçıya göndərildi", style = T.button)
                        }
                        event.isFull -> Text("Yer yoxdur", style = T.button)
                        else -> {
                            Icon(Icons.Filled.NorthEast, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (event.approval) "Qoşulma istəyi göndər" else "Tədbirə qoşul",
                                style = T.button,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun Modifier.androidxTapStar(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

private fun Modifier.androidxDetailClip(): Modifier =
    this.then(Modifier.background(Q.card, RoundedCornerShape(Q.rMd.dp)))

/** Facts grid — 2 sütunlu fakt kartı. */
@Composable
private fun FactsGrid(e: QoshulEvent) {
    val rows = buildList {
        add(Icons.Filled.Event to "${e.date}, ${e.time}")
        add(Icons.Filled.Group to "${e.spotsLeft} boş yer")
        add(Icons.Filled.LocationOn to e.location)
        add(Icons.Filled.EmojiEvents to "+${XpRules.JOIN_XP} XP qoşulma")
        add(Icons.Filled.Payments to (e.price?.let { "$it AZN" } ?: "Pulsuz"))
        e.level?.let { add(Icons.Filled.SignalCellularAlt to it) }
        e.language?.let { add(Icons.Filled.Translate to it) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .androidxFactsBorder()
            .padding(12.dp)
    ) {
        rows.chunked(2).forEach { pair ->
            Row {
                pair.forEach { (icon, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 5.dp),
                    ) {
                        Icon(icon, null, tint = Q.mutedFg, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.5.sp, color = Q.mutedFg))
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun Modifier.androidxFactsBorder(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxFactsBorderStroke()
)

private fun Modifier.androidxFactsBorderStroke(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)
