package az.qoshul.app.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.TaskTier
import az.qoshul.app.core.model.taskDefs
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.XpBar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton

/**
 * Tapşırıqlar Mərkəzi — master-spec §9 (D-155):
 * „Tapşırıqlar bölməsi 3 dərəcəli iyerarxiya üzrə qruplaşdırılmalıdır:
 *  Novice (onboarding) · Daily (gündəlik) · Advanced (irəli)“.
 *
 * Mükafatlar Qoşa Coin (XP) ilə — [taskDefs] kataloqu XpRules ilə sinxrondur.
 * Tamamlanma = engine-in award açarları (idempotent — bir dəfə verilir).
 */
@Composable
fun TaskCenterScreen(engine: AppEngine, nav: NavState) {
    // D-155: award açarları AppUiState güzgüsündən
    val st = engine.state.collectAsStateWithLifecycle().value
    val done: Set<String> = st.awardedKeys

    PushScreenScaffold(title = "Tapşırıqlar", onBack = { nav.pop() }) { pad ->
        LazyColumn(
            pad.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tapşırıqları tamamla, Qoşa Coin qazan. Coin-lər endirim " +
                        "kuponlarına çevrilir — „Endirimlər“ bölməsindən al.",
                    style = T.subtitle,
                )
            }
            for (tier in TaskTier.entries) {
                item(key = tier.name) {
                    TierHeader(tier)
                }
                val tasks = taskDefs.filter { it.tier == tier }
                items(tasks, key = { it.id }) { t ->
                    TaskRow(
                        title = t.title,
                        reward = t.rewardXp,
                        done = done.any { k: String -> k.startsWith(t.awardKey) },
                    )
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun TierHeader(tier: TaskTier) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.RocketLaunch, null, tint = Q.primaryText, modifier = Modifier.height(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(tier.label, style = T.button.copy(fontWeight = FontWeight.W800))
        }
        Text(tier.hint, style = T.meta, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth()) { XpBar(progress = 1f, height = 2.dp) }
    }
}

@Composable
private fun TaskRow(title: String, reward: Int, done: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .border(
                if (done) BorderStroke(1.dp, Q.primary.copy(alpha = 0.5f))
                else BorderStroke(1.dp, Q.border),
                RoundedCornerShape(Q.rLg.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                null,
                tint = if (done) Q.primary else Q.mutedFg,
                modifier = Modifier.height(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = T.body)
                Text(
                    if (done) "Tamamlandı ✓" else "+$reward Qoşa Coin",
                    style = T.meta.copy(
                        color = if (done) Q.primary else Q.mutedFg,
                        fontWeight = FontWeight.W600,
                    ),
                )
            }
        }
    }
}
