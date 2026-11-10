package az.qoshul.app.features.xp

import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.VenueXpAccount
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.motion.countUpAsState
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.QrCode
import az.qoshul.app.core.ui.ViewHeading
import az.qoshul.app.core.ui.XpBar

/**
 * XP Kartı — Flutter xp_card_screen.dart portu (D-026 L.4).
 *
 * A9 düzəlişi: 60s geri sayım artıq TAM ekranı yenidən qurmır — yalnız
 * lokal `tick` dövləti DynamicQr bölməsini yeniləyir.
 */
@Composable
fun XpCardScreen(engine: AppEngine, nav: NavState) {
    var selectedVenueId by remember { mutableStateOf<String?>(null) }
    val st by engine.state.collectAsStateWithLifecycle()
    val accounts = st.venueXp.values.sortedByDescending { it.balance }

    PushScreenScaffold(title = "XP kartım", onBack = { nav.pop() }) { mod ->
        LazyColumn(
            modifier = mod.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 21.dp, bottom = 32.dp
            ),
        ) {
            item {
                ViewHeading(
                    title = "XP kartım",
                    subtitle = "Endirim XP-ni obyektdə göstər, kassa endirimi alsın. " +
                        "Hər obyektin XP bazası ayrıdır.",
                )
                Spacer(Modifier.height(16.dp))
            }
            // — 1-ci XP: Peşəkarlıq —
            item {
                StaggerIn(1) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .androidxProBg()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = Q.primaryText, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Peşəkarlıq XP: ${st.xp} · ${st.rank}",
                                style = T.metaBold.copy(color = Q.primaryText))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Səviyyə üçün istifadə olunur — xərclənmir. " +
                                    "Səviyyən artdıqca Kəşf-də sənə peşəkar aktivliklər önə düşür.",
                                style = T.chipXs.copy(color = Q.mutedFg),
                            )
                        }
                    }
                }
                // D-063: 18→16dp (vahid bölmə ritmi)
                Spacer(Modifier.height(16.dp))
            }
            // — 2-ci XP: obyekt bazaları —
            itemsIndexed(accounts, key = { _, acct -> acct.venueId }) { i, acct ->
                StaggerIn(i + 2) {
                    AccountCard(
                        account = acct,
                        engine = engine,
                        expanded = selectedVenueId == acct.venueId,
                        onOpen = {
                            selectedVenueId = acct.venueId
                            engine.regenerateXpCard(acct.venueId)
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "XP-nin müddəti yoxdur — balans qazanma − xərcləmədir. " +
                        "Kamera işləməsə QR altındakı 6 rəqəmli kodu de.",
                    style = T.chipXs.copy(color = Q.mutedFg),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: VenueXpAccount,
    expanded: Boolean,
    engine: AppEngine,
    onOpen: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val bal = countUpAsState(account.balance)
    Column(
        Modifier
            .fillMaxWidth()
            .scaleTap(interaction)
            .androidxAcctTap(interaction, onOpen)
            .androidxAcctBg(expanded)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .androidxAcctIconBg(),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Storefront, null, tint = Q.primaryText, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(account.venueName, style = T.h3)
                Spacer(Modifier.height(2.dp))
                Text(account.config.describe(), style = T.chipXs.copy(color = Q.mutedFg))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$bal XP", style = T.titleLg.copy(color = Q.primaryText))
                Text("Endirim XP", style = T.metaSm)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).androidxLedgerDivider())
            Spacer(Modifier.height(14.dp))
            DynamicQrSection(account, engine)
            Spacer(Modifier.height(14.dp))
            Text("Hərəkətlər (ledger)", style = T.chipXs.copy(fontWeight = FontWeight.W700, color = Q.mutedFg))
            Spacer(Modifier.height(8.dp))
            if (account.recentLedger.isEmpty()) {
                Text("Hərəkət yoxdur — aktivite keçir, XP qazan.", style = T.chipXs.copy(color = Q.mutedFg))
            } else {
                account.recentLedger.take(4).forEach { e ->
                    Row(Modifier.padding(bottom = 7.dp)) {
                        Icon(
                            if (e.grant) Icons.Filled.AddCircle else Icons.Filled.RemoveCircle, null,
                            tint = if (e.grant) Q.primaryText else Q.destructive,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(e.note, style = T.bodySm)
                            Text(
                                (if (e.grant) "+" else "−") + "$e.amount XP · " +
                                    "%02d.%02d %02d:%02d".format(
                                        e.at.dayOfMonth, e.at.monthValue, e.at.hour, e.at.minute) +
                                    " · qalıq ${e.balanceAfter}",
                                style = T.metaSm,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dinamik QR — 60s ömür. A9: yalnız bu bölmə yenilənir (tick lokaldır).
 */
@Composable
private fun DynamicQrSection(account: VenueXpAccount, engine: AppEngine) {
    // A9: lokal saniyə sayğıcı — yalnız bu composable recompose olur.
    // U2.5 (UI-14) qeydi: bu, state-POLLING deyil, divar-saatı TAYMERİDİR —
    // canlı saniyə geri sayımı üçün 1s tick məcburidir (vaxt observable state
    // deyil; NIA time-axını eyni patterndədir). delay() cancellable-dir —
    // LaunchedEffect ləğvində dövr avtomatik dayanır. MapScreen-in 1.5s
    // style-polling-i isə snapshotFlow ilə əvəz olundu (orada state VAR idi).
    var tick by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(account.venueId) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tick++
        }
    }
    val card = engine.state.value.xpCard
    // A2: kartın etibarlığı/saniyə sayğacı vahid clock-dan (engine.now())
    val now = engine.now()
    val valid = card != null &&
        engine.state.value.xpCardVenueId == account.venueId && card.isValid(now)
    val seconds = card?.secondsLeft(now) ?: 0

    Column {
        Row(verticalAlignment = Alignment.Top) {
            Box(contentAlignment = Alignment.Center) {
                // U1.6 (UI-42): solğunlaşdırma əvvəl BOŞ Box-a tətbiq olunurdu —
                // heç nə solğunlaşdırmırdı; indi QR-ın öz modifierinə keçirilib.
                PopIn(key = card?.nonce ?: "yox") {
                    QrCode(
                        payload = card?.nonce ?: "expired", size = 124.dp,
                        modifier = Modifier.alphaReduced(if (valid) 1f else 0.35f),
                    )
                }
                if (!valid) {
                    Icon(Icons.Filled.Block, null,
                        tint = Q.destructive.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Dinamik XP kodu", style = T.chipXs.copy(fontWeight = FontWeight.W700, color = Q.mutedFg))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (valid)
                        "Kassaya göstər — $seconds saniyədən sonra yenilənir. " +
                            "Kod tək istifadəlikdir: skan ediləndə bir daha işləmir."
                    else "Kodun vaxtı bitdi və ya işlənib — yenilə.",
                    style = T.chipXs.copy(color = Q.mutedFg),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .androidxFallbackBg()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.Dialpad, null, tint = Q.mutedFg, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        card?.fallbackCode ?: "------",
                        style = T.h3,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                XpBar(
                    progress = if (valid) seconds / XpRules.XP_CARD_TTL_SECONDS.toFloat() else 0f,
                    height = 5.dp,
                )
            }
            Spacer(Modifier.width(10.dp))
            val interaction = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scaleTap(interaction)
                    .androidxRefreshTap(interaction) { engine.regenerateXpCard(account.venueId) }
                    .androidxRefreshBg()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(Icons.Filled.Refresh, null, tint = Q.primaryFg, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text("Yenilə", style = T.button.copy(color = Q.primaryFg))
            }
        }
    }
}

private fun Modifier.alphaReduced(alpha: Float): Modifier =
    this.then(Modifier.androidxDrawAlpha(alpha))

private fun Modifier.androidxAcctTap(interaction: MutableInteractionSource, onOpen: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = interaction, indication = null, onClick = onOpen))

private fun Modifier.androidxAcctBg(expanded: Boolean): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxAcctBorder(expanded)
)

private fun Modifier.androidxAcctBorder(expanded: Boolean): Modifier = this.then(
    Modifier.border(
        1.dp, if (expanded) Q.primaryText else Q.border, RoundedCornerShape(Q.rLg.dp),
    )
)

private fun Modifier.androidxAcctIconBg(): Modifier = this.then(
    Modifier.background(Q.secondary, RoundedCornerShape(Q.rPill))
)

private fun Modifier.androidxProBg(): Modifier = this.then(
    Modifier
        .background(Q.secondary, RoundedCornerShape(Q.rLg.dp))
        .androidxProBorder()
)

private fun Modifier.androidxProBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxLedgerDivider(): Modifier = this.then(Modifier.background(Q.border))

private fun Modifier.androidxFallbackBg(): Modifier = this.then(
    Modifier
        .background(Q.muted, RoundedCornerShape(Q.rPill))
        .androidxFallbackBorder()
)

private fun Modifier.androidxFallbackBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rPill))
)

private fun Modifier.androidxRefreshTap(interaction: MutableInteractionSource, onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick))

private fun Modifier.androidxRefreshBg(): Modifier = this.then(
    Modifier.background(Q.primary, RoundedCornerShape(Q.rPill))
)

private fun Modifier.androidxDrawAlpha(a: Float): Modifier = this.alpha(a)
