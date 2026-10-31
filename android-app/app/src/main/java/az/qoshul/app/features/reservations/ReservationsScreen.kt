package az.qoshul.app.features.reservations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.WhereToVote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.Reservation
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.QrCode
import az.qoshul.app.core.ui.ResvStatusChip
import az.qoshul.app.core.ui.SafetyNote
import az.qoshul.app.core.ui.imagePaint

/** "Rezervlərim" — Flutter reservations_screen.dart portu (ROL E). */
@Composable
fun ReservationsScreen(engine: AppEngine, nav: NavState) {
    val st by engine.state.collectAsState()
    var qrResv by remember { mutableStateOf<Reservation?>(null) }

    PushScreenScaffold(title = "Rezervlərim", onBack = { nav.pop() }) { mod ->
        if (engine.reservationRestricted) {
            Box(mod.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Block,
                    text = "Rezerv hüquğun müvəqqəti məhdudlaşdırıb — 3 gəlməmə " +
                        "qeydə alındı. 7 gün sonra yenidən açılacaq.",
                )
            }
        } else if (st.reservations.isEmpty()) {
            Box(mod.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.TableRestaurant,
                    text = "Hələ rezervin yoxdur.\nXəritədən məkan seç, stol ayır.",
                )
            }
        } else {
            LazyColumn(
                modifier = mod.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
                ),
            ) {
                item {
                    Text(
                        "Gəlməmə sayı: ${st.noShowCount}/3 — 3-dən sonra 7 günlük " +
                            "məhdudiyyət yaranır.",
                        style = T.subtitle,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                itemsIndexed(st.reservations) { _, r ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .androidxResvCard()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = imagePaint(r.venueImage), contentDescription = r.venueName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(52.dp).androidxResvImg(),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.venueName, style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 14.sp,
                                    fontWeight = FontWeight.W800))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${r.date}, ${r.time} · ${r.party} nəfər" +
                                        (r.couponCode?.let { " · kupon: $it" } ?: ""),
                                    style = T.meta,
                                )
                            }
                            ResvStatusChip(status = r.status)
                        }
                        if (r.note.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Qeyd: ${r.note}", style = T.metaSm)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row {
                            OutlinedButton(
                                onClick = { qrResv = r },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(Q.rPill),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Q.card, contentColor = Q.foreground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
                            ) {
                                Icon(Icons.Filled.QrCode2, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("QR", style = T.button)
                            }
                            if (r.canCheckIn) {
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { engine.checkInReservation(r.id) },
                                    modifier = Modifier.weight(2f).height(52.dp),
                                    shape = RoundedCornerShape(Q.rPill),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Q.primary, contentColor = Q.primaryFg),
                                ) {
                                    Icon(Icons.Filled.WhereToVote, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Check-in et", style = T.button)
                                }
                            }
                            if (r.canCancel) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { engine.cancelReservation(r.id) }) {
                                    Icon(Icons.Filled.Close, "Ləğv et", tint = Q.destructive, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        // D-056: gəlməmə axını — engine.markNoShow hazır idi, UI-a bağlı
                        // deyildi; sayğac "X/3" artıq real dəyişir (3 → 7 gün məhdudiyyət)
                        if (r.canCancel) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .androidxResvTap { engine.markNoShow(r.id) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Icon(Icons.Filled.PersonOff, null, tint = Q.destructive, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Gəlməyəcəm — əvvəlcədən bildir",
                                    style = T.meta.copy(color = Q.destructive),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                item { SafetyNote() }
            }
        }
    }

    qrResv?.let { r ->
        AlertDialog(
            onDismissRequest = { qrResv = null },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(r.venueName, style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 15.sp, fontWeight = FontWeight.W800))
                    Spacer(Modifier.height(4.dp))
                    Text("${r.date}, ${r.time} · ${r.party} nəfər", style = T.meta)
                    Spacer(Modifier.height(14.dp))
                    QrCode(payload = r.qrCode)
                    Spacer(Modifier.height(8.dp))
                    Text(r.qrCode, style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W700))
                }
            },
            confirmButton = { TextButton(onClick = { qrResv = null }) { Text("Bağla") } },
        )
    }
}

private fun Modifier.androidxResvCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxResvBorder()
)

private fun Modifier.androidxResvTap(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

private fun Modifier.androidxResvBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxResvImg(): Modifier = this.then(
    Modifier.background(Q.card, RoundedCornerShape(Q.rMd.dp))
)
