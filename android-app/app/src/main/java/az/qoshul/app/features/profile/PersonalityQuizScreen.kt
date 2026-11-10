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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import az.qoshul.app.core.theme.OpenSans
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
    // U2.2 (UI-02): test addımı + cavablar saveable (List<Int> — Serializable)
    var step by rememberSaveable { mutableIntStateOf(-1) }
    val answers = rememberSaveable { mutableStateListOf<Int>() }

    PushScreenScaffold(title = "Şəxsiyyət testi", onBack = { nav.pop() }) { pad ->
        // D-063: məzmun yuxarıda topa-laşib, ekranın ~60%-i boş qalırdı
        // (vizual audit: "bottom 60% completely empty — looks like a loading error").
        // fillParentMaxHeight = viewport hündürlüyü; daxildə addımlar şaquli
        // MƏRKƏZLƏŞİR — boş sahə bərabər paylanır, kiçik ekranda da sığışır.
        Box(
            pad
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(Motion.D2, easing = Motion.ease)) +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(Motion.D2, easing = Motion.ease)))
                        .togetherWith(fadeOut(tween(Motion.D2)))
                },
                label = "quizStep",
                modifier = Modifier.fillMaxSize(),
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

// ═══════════════════ Görünümlər ═══════════════════

@Composable
private fun Intro(onStart: () -> Unit) {
    StaggerIn(0) {
        // D-063: şaquli mərkəzləşmə — intro məzmunu ekranın mərkəzində;
        // altdan 90dp — nav pill zonası hesabına vizual mərkəzə düzəliş
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Psychology, null, tint = Q.primaryText, modifier = Modifier.height(56.dp))
                Spacer(Modifier.height(20.dp))
                Text("10 sual · 2 dəqiqə", style = T.h1.copy(textAlign = TextAlign.Center))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Cavablarına görə dominant cəhətini tapırıq. Nəticə yalnız sənin " +
                        "profilində görünür, heç yerə göndərilmir.",
                    style = T.subtitle.copy(textAlign = TextAlign.Center, lineHeight = 21.sp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Nəticə profili tamamlayır: şəkil + bio + maraqlar varsa +30 XP.",
                    style = T.meta.copy(textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                ) {
                    Icon(Icons.Filled.NorthEast, null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Testə başla", style = T.button)
                }
            }
        }
    }
}

@Composable
private fun QuestionStep(index: Int, total: Int, text: String, onAnswer: (Int) -> Unit) {
    // D-063: gedişat yuxarıda SABİT, sual+cavablar qalan sahənin MƏRKƏZİNDƏ —
    // interaktiv məzmun əvvəl yuxarı 40%-də topa-laşırdı, alt 60% boş qalırdı.
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp),
        ) {
            Text("${index + 1}/$total", style = T.button.copy(color = Q.primaryText, fontWeight = FontWeight.W800))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                XpBar(progress = index.toFloat() / total, height = 5.dp)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // D-063: məzmun blokunu böyüdürük — sual 18→20sp, seçim kartları
            // 52→56dp, aralar 10→14dp (ekranın ortasında dolğun blok dayanır)
            Column(Modifier.fillMaxWidth().padding(bottom = 90.dp)) {
                Text(text, style = T.statSm.copy(fontWeight = FontWeight.W800))
                Spacer(Modifier.height(28.dp))
                listOf(
                    "Razıyam" to 2,
                    "Qismən" to 1,
                    "Razı deyiləm" to 0,
                ).forEach { (label, score) ->
                    OutlinedButton(
                        onClick = { onAnswer(score) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        // D-058: seçim kartı — reference dil: 16dp radius, 1.5dp kənar
                        // (düymələrdən fərqli olaraq kapsul DEYİL — reference option kartları 16px)
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Q.card, contentColor = Q.foreground),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Q.border),
                    ) { Text(label, style = T.button.copy(fontWeight = FontWeight.W600)) }
                    Spacer(Modifier.height(14.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cavabın dərhal növbəti suala keçirir — geri qayıtmaq olmur.",
                    style = T.meta,
                )
            }
        }
    }
}

@Composable
private fun QuizResult(
    result: PersonalityResult,
    traitLabels: Map<String, String>,
    onSave: () -> Unit,
) {
    PopIn {
        // D-063: nəticə də mərkəzdə; altdan 90dp — nav pill zonası nəzərə alınır
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().padding(bottom = 90.dp)) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Q.primaryText, modifier = Modifier.height(30.dp))
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
                            Text("$pct%", style = T.meta.copy(color = Q.primaryText, fontWeight = FontWeight.W800))
                        }
                        Spacer(Modifier.height(6.dp))
                        XpBar(progress = pct / 100f, height = 6.dp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = ButtonDefaults.buttonColors(containerColor = Q.primary, contentColor = Q.primaryFg),
                ) {
                    Icon(Icons.Filled.Psychology, null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Profildə göstər", style = T.button)
                }
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
