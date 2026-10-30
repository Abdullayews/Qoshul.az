package az.qoshul.app.features.safety

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.safetyTips
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.SafetyNote
import az.qoshul.app.core.ui.ViewHeading

/** Təhlükəsizlik məsləhətləri — Flutter safety_screen.dart portu (ROL I). */
@Composable
fun SafetyScreen(nav: NavState) {
    PushScreenScaffold(title = "Təhlükəsizlik", onBack = { nav.pop() }) { mod ->
        LazyColumn(
            modifier = mod.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
            ),
        ) {
            item { ViewHeading(title = "İlk görüş məsləhətləri",
                subtitle = "Yeni tanışlığa hazırlaş — bu sadə qaydalar çox kömək edir.") }
            items(safetyTips.size) { i ->
                val (title, text) = safetyTips[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .androidxTipCard()
                        .padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top,
                ) {
                    Box(
                        Modifier.size(34.dp).background(Q.secondary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 13.sp,
                            fontWeight = FontWeight.W800, color = Q.primary))
                    }
                    Spacer(Modifier.padding(6.dp))
                    Column {
                        Text(title, style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W700))
                        Spacer(Modifier.height(5.dp))
                        Text(text, style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 12.5.sp,
                            lineHeight = 21.sp, color = Q.mutedFg))
                    }
                }
            }
            item {
                CommunityBanner(
                    icon = Icons.Outlined.Shield,
                    title = "Dəqiq ünvan heç vaxt paylaşılmır",
                    subtitle = "Başqa istifadəçilər yalnız təxmini məsafəni görür " +
                        "(məs. \"~2 km\"). Ev ünvanın və koordinatın API cavablarında gizli qalır.",
                )
            }
            item { SafetyNote() }
        }
    }
}

private fun Modifier.androidxTipCard(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .androidxTipBorder()
)

private fun Modifier.androidxTipBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)
