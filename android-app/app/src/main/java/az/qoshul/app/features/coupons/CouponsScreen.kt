package az.qoshul.app.features.coupons

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.OfferCard
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.QrPlaceholder
import az.qoshul.app.core.ui.Segmented

/**
 * Endirimlər və kuponlar — Flutter coupons_screen.dart portu (ROL G):
 * a) səviyyəyə görə avtomatik; b) XP ilə "al" (D-026 ayrı baza qaydası).
 */
@Composable
fun CouponsScreen(engine: AppEngine, nav: NavState) {
    var tab by remember { mutableIntStateOf(0) }
    var qrCoupon by remember { mutableStateOf<az.qoshul.app.core.model.UserCoupon?>(null) }
    val st by engine.state.collectAsState()

    PushScreenScaffold(title = "Endirimlər", onBack = { nav.pop() }) { mod ->
        Column(mod.fillMaxSize()) {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
                ),
            ) {
                item {
                    // Banner: D-026 ayrı baza qaydası
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .androidxBannerBg()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Toll, null, tint = Q.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.padding(6.dp))
                        Text(
                            "Kuponlar obyektin Endirim XP balansından alınır — D-026 ayrı baza " +
                                "qaydası (Xankəndi XP-si yalnız Xankəndidə işləyir)",
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.5.sp,
                                fontWeight = FontWeight.W600, color = Q.primary, lineHeight = 19.sp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Segmented(listOf("Endirimlər", "Kuponlarım"), tab) { tab = it }
                }
                if (tab == 0) {
                    val visible = engine.visibleOffers
                    if (visible.isEmpty()) {
                        item { EmptyState(icon = Icons.Filled.Percent, text = "Hazırda təklif yoxdur.") }
                    } else {
                        itemsIndexed(visible) { i, o ->
                            StaggerIn(i.coerceIn(0, 8)) {
                                OfferCard(
                                    venueName = o.venueName,
                                    label = o.label,
                                    requirement = if (o.autoByLevel)
                                        "Səviyyə: ${XpRules.rankNames[o.minRankIndex]}+ (avtomatik aktiv)"
                                    else "${o.xpPrice} XP · balansın: ${st.venueXp[o.venueId]?.balance ?: 0} XP",
                                    autoByLevel = o.autoByLevel,
                                    unlocked = o.unlockedFor(st.rankIndex),
                                    action = if (o.autoByLevel) {
                                        if (o.unlockedFor(st.rankIndex)) {
                                            { Icon(Icons.Filled.CheckCircle, null, tint = Q.primary, modifier = Modifier.size(20.dp)) }
                                        } else null
                                    } else {
                                        {
                                            Button(
                                                onClick = { engine.buyCoupon(o.id) },
                                                enabled = o.unlockedFor(st.rankIndex) &&
                                                    (st.venueXp[o.venueId]?.balance ?: 0) >= o.xpPrice,
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                                                shape = RoundedCornerShape(Q.rMd.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Q.primary, contentColor = Q.primaryFg,
                                                    disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                                            ) { Text("XP ilə al", style = T.button) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                } else {
                    if (st.coupons.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.ConfirmationNumber,
                                text = "Hələ kuponun yoxdur.\n\"Endirimlər\" siyahısından XP ilə al.",
                            )
                        }
                    } else {
                        itemsIndexed(st.coupons) { _, c ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .androidxCouponCard(c.isActive)
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(46.dp)
                                            .androidxCouponIconBg(c.isActive),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (c.isActive) Icons.Filled.ConfirmationNumber else Icons.Filled.Block,
                                            null, tint = if (c.isActive) Q.accentFg else Q.mutedFg,
                                            modifier = Modifier.size(21.dp),
                                        )
                                    }
                                    Spacer(Modifier.padding(6.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.label, style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = JakartaSans, fontSize = 13.sp,
                                            fontWeight = FontWeight.W800))
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            c.venueName + " · " + c.code + "\n" + c.statusLabel +
                                                (if (c.isActive) " · ${c.daysLeft} gün qalıb" else ""),
                                            style = T.meta,
                                        )
                                    }
                                    TextButton(
                                        onClick = { qrCoupon = c },
                                        enabled = c.isActive,
                                    ) { Text("QR göstər") }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Vaxtı bitən istifadə olunmamış kupon üçün XP geri qaytarılmır.",
                        textAlign = TextAlign.Center, style = T.meta,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    qrCoupon?.let { c ->
        AlertDialog(
            onDismissRequest = { qrCoupon = null },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(c.venueName, style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 14.sp, fontWeight = FontWeight.W800))
                    Spacer(Modifier.height(10.dp))
                    QrPlaceholder(code = c.code)
                    Spacer(Modifier.height(8.dp))
                    Text(c.code, style = T.meta)
                }
            },
            confirmButton = {
                TextButton(onClick = { qrCoupon = null }) { Text("Bağla") }
            },
        )
    }
}

private fun Modifier.androidxBannerBg(): Modifier = this.then(
    Modifier.background(Q.secondary, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxCouponCard(active: Boolean): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxCouponCardBorder(active)
)

private fun Modifier.androidxCouponCardBorder(active: Boolean): Modifier = this.then(
    Modifier.border(
        1.2.dp.takeIf { active } ?: 1.dp,
        if (active) Q.primary else Q.border,
        RoundedCornerShape(Q.rLg.dp),
    )
)

private fun Modifier.androidxCouponIconBg(active: Boolean): Modifier = this.then(
    Modifier
        .background(if (active) Q.rankBadgeBg else Q.muted, RoundedCornerShape(Q.rMd.dp))
)
