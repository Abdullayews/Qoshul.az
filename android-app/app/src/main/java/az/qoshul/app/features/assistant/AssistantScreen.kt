package az.qoshul.app.features.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.parseAssistantEvent
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.AiBox
import az.qoshul.app.core.ui.ChatBubble
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.features.auth.QoshulTextField

/**
 * AI köməkçi — Flutter assistant_screen.dart portu: təklif çipləri, axın
 * simulyasiyalı mesajlar (engine coroutine ilə), [TƏDBİR] təklif kartı.
 */
/**
 * "Bakı" → "Bakıda", "Gəncə" → "Gəncədə" (D-057): son saitə görə -da/-də.
 * Arxa saitlər (a,ı,o,u) → "da"; ön saitlər (e,ə,i,ö,ü) → "də".
 */
private fun cityIn(city: String): String {
    val v = city.lastOrNull { c -> c in "aıoueəiüö" } ?: 'a'
    return "$city${if (v in "aıou") "da" else "də"}"
}

@Composable
fun AssistantScreen(engine: AppEngine, nav: NavState, onOpenCreate: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val st by engine.state.collectAsState()
    val listState = rememberLazyListState()

    val suggestions = listOf(
        // D-057: "${st.city}da" sadəlövh birləşdirmə idi — "Gəncə" üçün yanlış
        // olurdu; cityIn son saitiə görə -da/-də seçir (Bakıda ✓, Gəncədə ✓)
        "Bu həftə sonu ${cityIn(st.city)} nə var?",
        "Pulsuz idman tədbirləri hansılardır?",
        "Gəncədə tanışlıq üçün nə tövsiyə edərsən?",
        "Şənbə üçün tədbir fikri ver",
    )

    // Son AI cavabından təklif çıxar
    var proposal by remember { mutableStateOf<az.qoshul.app.core.model.EventProposal?>(null) }
    LaunchedEffect(st.assistantMsgs.size, st.assistantBusy) {
        val lastAi = st.assistantMsgs.lastOrNull { !it.mine }
        proposal = lastAi?.let { parseAssistantEvent(it.body).proposal }
    }
    LaunchedEffect(st.assistantMsgs.size, st.assistantBusy) {
        if (st.assistantMsgs.isNotEmpty()) listState.animateScrollToItem(st.assistantMsgs.size - 1)
    }

    PushScreenScaffold(title = "Qoshul köməkçisi", onBack = { nav.pop() }) { mod ->
        Column(mod.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp, vertical = 12.dp
                ),
            ) {
                if (st.assistantMsgs.isEmpty()) {
                    item {
                        Column {
                            suggestions.forEach { s ->
                                OutlinedButton(
                                    onClick = { engine.sendAssistant(s) },
                                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                                        .height(34.dp),
                                    shape = RoundedCornerShape(Q.rMd.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Q.secondary, contentColor = Q.primary),
                                ) { Text(s, style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 12.5.sp)) }
                            }
                        }
                    }
                }
                itemsIndexed(st.assistantMsgs) { _, m ->
                    ChatBubble(
                        sender = if (m.mine) "Sən" else "Köməkçi",
                        body = parseAssistantEvent(m.body).text,
                        mine = m.mine,
                    )
                }
                if (st.assistantBusy) {
                    item {
                        CircularProgressIndicator(strokeWidth = 2.dp,
                            modifier = Modifier.padding(top = 14.dp).size(16.dp))
                    }
                }
                if (proposal != null && !st.assistantBusy) {
                    item {
                        AiBox {
                            Column {
                                Text("Təsdiqləndi: ${proposal!!.title}",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = JakartaSans, fontSize = 13.sp,
                                        fontWeight = FontWeight.W600))
                                Spacer(Modifier.height(6.dp))
                                Text("${proposal!!.date} ${proposal!!.time} · " +
                                    "${proposal!!.location} · ${proposal!!.capacity} nəfər",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = JakartaSans, fontSize = 12.5.sp,
                                        color = Q.mutedFg))
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = onOpenCreate,
                                    shape = RoundedCornerShape(Q.rMd.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Q.primary, contentColor = Q.primaryFg),
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bu tədbiri yarat", style = T.button)
                                }
                            }
                        }
                    }
                }
            }
            // — yazma zolağı —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .androidxComposerBg()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
                    .imePadding(),
            ) {
                Box(Modifier.weight(1f)) {
                    QoshulTextField(value = text, onValueChange = { text = it },
                        hint = "Sualını yaz...")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { engine.sendAssistant(text); text = "" },
                    enabled = !st.assistantBusy && text.isNotBlank(),
                    modifier = Modifier.size(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    shape = RoundedCornerShape(Q.rMd.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Q.primary, contentColor = Q.primaryFg,
                        disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                ) {
                    Icon(
                        if (st.assistantBusy) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
                        "Göndər", modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun Modifier.androidxComposerBg(): Modifier = this.then(
    Modifier
        .background(Q.card)
        .androidxComposerBorder()
)

private fun Modifier.androidxComposerBorder(): Modifier = this.then(
    Modifier.border(1.dp, Q.border)
)
