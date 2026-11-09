package az.qoshul.app.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T

/**
 * Xəritə kanvasının ortaq xromu (D-108) — hər iki backend (MapLibre /
 * Google) eyni zoom həblərini və xəta kartını işlədir; yalnız kamera
 * əmrləri backend-ə xasdır ([onZoom] delta alır).
 */
@Composable
internal fun BoxScope.MapZoomControls(onZoom: (Double) -> Unit) {
    Column(
        Modifier
            .align(Alignment.TopEnd)
            .padding(top = 170.dp, end = 12.dp)
    ) {
        // U3.1 (UI-06): zoom pilləri glass deyil (D-096) — opaq Q.card dili.
        // IconButton 48dp hədəfi qorunur (U1.3/UI-04).
        Box(
            Modifier
                .background(Q.card, RoundedCornerShape(Q.rPill))
                .border(1.dp, Q.border, RoundedCornerShape(Q.rPill))
                .padding(7.dp)
        ) {
            IconButton(onClick = { onZoom(+1.0) }) {
                Icon(Icons.Filled.Add, "Yaxınlaşdır", tint = Q.foreground, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .background(Q.card, RoundedCornerShape(Q.rPill))
                .border(1.dp, Q.border, RoundedCornerShape(Q.rPill))
                .padding(7.dp)
        ) {
            // U1.3 (UI-04): 16dp clickable → IconButton 48dp hədəfi.
            IconButton(onClick = { onZoom(-1.0) }) {
                Icon(Icons.Filled.Remove, "Uzaqlaşdır", tint = Q.foreground, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * D-061: xəritə stili yüklənməyəndə fallback kart — „nə olar əgər internet
 * yoxdur / tile server çatılmır“ vəziyyətinin UI placeholder-i.
 * Filtr həbləri və sayğac işləməyə davam edir (data lokaldır); yalnız tile
 * səthi boşdur — kart bunu aydın deyir və təkrar cəhd verir.
 */
@Composable
internal fun MapLoadFailedCard(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Q.card, RoundedCornerShape(Q.rXl.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rXl.dp))
            .padding(horizontal = 26.dp, vertical = 20.dp),
    ) {
        Icon(Icons.Filled.CloudOff, null, tint = Q.destructive, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(10.dp))
        Text("Xəritə yüklənmədi", style = T.h2)
        Spacer(Modifier.height(6.dp))
        Text(
            "İnternet bağlantısı yoxdur və ya xəritə serveri cavab vermir. " +
                "Məkan və tədbir siyahıları işləyir — bağlantı gələndə yenidən yoxla.",
            style = T.chipXs.copy(color = Q.mutedFg),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .scaleTap(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onRetry)
                .background(Q.primary, RoundedCornerShape(Q.rPill))
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Filled.Refresh, null, tint = Q.primaryFg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Yenidən dənə", style = T.bodyBold.copy(color = Q.primaryFg))
        }
    }
}
