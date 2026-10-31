package az.qoshul.app.features.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.PersonalityResult
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import androidx.compose.material3.ButtonColors
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.PushScreenScaffold
import az.qoshul.app.core.ui.XpBar

/**
 * Şəxsiyyət testi (D-056) — FakeData-da 10 sual + 5 trait əvvəldən var idi,
 * amma heç bir UI bunları istifadə etmirdi (PersonalityResult həmişə null →
 * profildə TraitBars heç vaxt göstərilmirdi).
 *
 * 3 bal: razıyam(2) / qismən(1) / razı deyiləm(0); tərs suallarda çevrilir.
 * Nəticə dominant cəhətə görə arxeTip adı alır və profildə görünür.
 * Yalnız local hesab — heç bir məlumat çıxarılmır.
 */
@Composable
fun PersonalityQuizScreen(engine: AppEngine, nav: NavState) {
    val questions = engine.dataSource.personalityQuestions
    val traitLabels = engine.dataSource.traitLabels

    // -1 = intro, 0..size-1 = sual, size = nəticə
    var step by remember { mutableIntStateOf(-1) }
    val answers = remember { mutableStateListOf<Int>() }

    PushScreenScaffold(title = "Şəxsiyyət testi", onBack = { nav.pop() }) { pad ->
        LazyColumn(
            modifier = pad.padding(horizontal = 20.dp),
        ) {
            item {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn(tween(Motion.D2, easing = Motion.ease)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(Motion.D2, easing = Motion.ease)))
                            .togetherWith(fadeOut(tween(Motion.D2)))
                    },
                    label = "quizStep",
                ) { s ->
                    when {
                        s < 0 -> Intro(onStart = { step = 0 })
                        s < questions.size -> {
                            val (text, _) = questions[s]
                            QuestionStep(
                                index = s, total = questions.size, text = text,
                                onAnswer = { a ->
                                    answers.add(a)
                                    step = s + 1
                                },
                            )
                        }
                        else -> {
                            val result = computeResult(questions, answers.toList(), traitLabels)
                            QuizResult(
                                result = result, traitLabels = traitLabels,
                                onSave = {
                                    engine.setPersonality(result)
                                    nav.pop()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════ Görünümlər ═══════════════════

@Composable
private fun Intro(onStart: () -> Unit) {
    StaggerIn(0) {
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Psychology, null, tint = Q.primary, modifier = Modifier.height(34.dp))
            Spacer(Modifier.height(12.dp))
            Text("10 sual · 2 dəqiqə", style = T.h1.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(8.dp))
            Text(
                "Cavablarına görə dominant cəhətini tapırıq. Nəticə yalnız sənin " +
                    "profilində görünür, heç yerə göndərilmir.",
                style = T.subtitle.copy(textAlign = TextAlign.Center, lineHeight = 21.sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Nəticə profili tamamlayır: şəkil + bio + maraqlar varsa +30 XP.",
                style = T.meta.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(Q.rMd.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
            ) {
                Icon(Icons.Filled.NorthEast, null, modifier = Modifier.height(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Testə başla", style = T.button)
            }
        }
    }
}

@Composable
private fun QuestionStep(index: Int, total: Int, text: String, onAnswer: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        // — gedişat —
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}/$total", style = T.button.copy(color = Q.primary, fontWeight = FontWeight.W800))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                XpBar(progress = index.toFloat() / total, height = 5.dp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(text, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 17.sp, fontWeight = FontWeight.W800,
            lineHeight = 26.sp))
        Spacer(Modifier.height(24.dp))
        listOf(
            "Razıyam" to 2,
            "Qismən" to 1,
            "Razı deyiləm" to 0,
        ).forEach { (label, score) ->
            OutlinedButton(
                onClick = { onAnswer(score) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(Q.rMd.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Q.card, contentColor = Q.foreground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Q.border),
            ) { Text(label, style = T.button) }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Cavabın dərhal növbəti suala keçirir — geri qayıtmaq olmur.",
            style = T.meta,
        )
    }
}

@Composable
private fun QuizResult(
    result: PersonalityResult,
    traitLabels: Map<String, String>,
    onSave: () -> Unit,
) {
    PopIn {
        Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Q.primary, modifier = Modifier.height(30.dp))
            Spacer(Modifier.height(10.dp))
            Text(result.type, style = T.h1)
            Spacer(Modifier.height(8.dp))
            Text(result.summary, style = T.subtitle.copy(lineHeight = 21.sp))
            Spacer(Modifier.height(18.dp))
            // — trait zolaqları (ProfileScreen TraitBars pariteti) —
            result.traits.forEach { (trait, pct) ->
                Column(Modifier.padding(vertical = 7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(traitLabels[trait] ?: trait, style = T.meta.copy(fontWeight = FontWeight.W700))
                        Text("$pct%", style = T.meta.copy(color = Q.primary, fontWeight = FontWeight.W800))
                    }
                    Spacer(Modifier.height(6.dp))
                    XpBar(progress = pct / 100f, height = 6.dp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(Q.rMd.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
            ) {
                Icon(Icons.Filled.Psychology, null, modifier = Modifier.height(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Profildə göstər", style = T.button)
            }
        }
    }
}

// ═══════════════════ Hesablama (pure, JVM-test oluna bilər) ═══════════════════

/** Dominant cəhət arxeTip adları. */
private val archetypeNames = mapOf(
    "extraversion" to "Sosial Ruh",
    "openness" to "Kəşfiyyatçı",
    "conscientiousness" to "Plançı",
    "agreeableness" to "Meyilli Ürək",
    "stability" to "Sakit Sima",
)

/**
 * Cavablardan PersonalityResult: hər trait üçün 0-2 bal, tərs suallarda çevrilir;
 * faizlər 25-95 aralığına sıxılır (0%/100% qəribə görünür, demo yumşaldması).
 */
fun computeResult(
    questions: List<Pair<String, Pair<String, Boolean>>>,
    answers: List<Int>,
    traitLabels: Map<String, String>,
): PersonalityResult {
    val scores = mutableMapOf<String, Int>()
    val maxima = mutableMapOf<String, Int>()
    questions.forEachIndexed { i, (_, traitRev) ->
        val (trait, reversed) = traitRev
        val raw = answers.getOrElse(i) { 1 }
        val score = if (reversed) 2 - raw else raw
        scores[trait] = (scores[trait] ?: 0) + score
        maxima[trait] = (maxima[trait] ?: 0) + 2
    }
    val traits = scores.mapValues { (t, s) ->
        val pct = 100 * s / (maxima[t] ?: 1)
        pct.coerceIn(25, 95)
    }
    val ranked = traits.entries.sortedByDescending { it.value }
    val top = ranked.getOrNull(0)
    val second = ranked.getOrNull(1)
    val topLabel = traitLabels[top?.key ?: ""] ?: (top?.key ?: "")
    val secondLabel = traitLabels[second?.key ?: ""] ?: (second?.key ?: "")
    val type = archetypeNames[top?.key] ?: "Kəşfiyyatçı"
    val summary = if (top != null && second != null) {
        "Dominant cəhətin $topLabel (${top.value}%). İkinci güclü cəhət: " +
            "$secondLabel (${second.value}%). Bu nəticə profilində görünür və " +
            "tədbir tövsiyələrində nəzərə alınır."
    } else {
        "Cavablar qeydə alındı — nəticəni profilində görəcəksən."
    }
    return PersonalityResult(type = type, summary = summary, traits = traits)
}
