package az.qoshul.app.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.R
import az.qoshul.app.core.model.BadgeDef
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.ResvStatus
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.countUpAsState
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.theme.JakartaSans

// ═══════════════════ Şəkil açarları → res ═══════════════════

/** Mock şəkil açarını rəsm resursuna çevirir (models saf qalır). */
@Composable
fun imagePaint(key: String): Painter = when (key) {
    "coffee" -> painterResource(R.drawable.coffee)
    "padel" -> painterResource(R.drawable.padel)
    "tech" -> painterResource(R.drawable.tech)
    else -> painterResource(R.drawable.friends_baku)
}

// ═══════════════════ Kiçik tip köməkçiləri ═══════════════════

private fun tStyle(size: Double, w: FontWeight, color: Color = Q.foreground) =
    TextStyle(fontFamily = JakartaSans, fontSize = size.sp, fontWeight = w, color = color)

// ═══════════════════ .section-heading + .section-link ═══════════════════

@Composable
fun SectionHeading(
    title: String,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Q.primary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = tStyle(19.0, FontWeight.W800))
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionLink(text: String, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick?.invoke() },
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Q.primary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = tStyle(11.5, FontWeight.W600, Q.primary))
    }
}

// ═══════════════════ .rank-badge ═══════════════════

@Composable
fun RankBadge(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Q.rankBadgeBg, RoundedCornerShape(Q.rMd.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Icon(Icons.Filled.EmojiEvents, null, tint = Q.accentFg, modifier = Modifier.size(13.5.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = tStyle(11.5, FontWeight.W700, Q.accentFg))
    }
}

// ═══════════════════ .xp-line + .xp-track ═══════════════════

@Composable
fun XpTrack(xp: Int) {
    val prog = XpRules.xpProgress(xp)
    val shownXp = countUpAsState(xp)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$shownXp XP", style = tStyle(12.0, FontWeight.W600))
            Text(
                if (prog.nextName == null) "Ən yüksək səviyyədəsən!"
                else "${prog.nextName} səviyyəsinə ${prog.toNext} XP",
                style = T.meta,
            )
        }
        Spacer(Modifier.height(6.dp))
        XpBar(progress = prog.pct / 100f, height = 6.dp)
    }
}

/** Animasiyalı progress zolağı (TweenAnimationBuilder portu). */
@Composable
fun XpBar(progress: Float, height: Dp = 4.dp) {
    val target by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(Motion.D4, easing = Motion.ease),
        label = "xpBar",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(Q.border)
    ) {
        Box(
            Modifier
                .fillMaxWidth(target)
                .height(height)
                .background(Q.primary)
        )
    }
}

// ═══════════════════ .segmented ═══════════════════

@Composable
fun Segmented(labels: List<String>, active: Int, onChanged: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 22.dp)
            .background(Q.muted, RoundedCornerShape(9.dp))
            .padding(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val isActive = i == active
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(if (isActive) Q.card else Color.Transparent, RoundedCornerShape(7.dp))
                    .clickable { onChanged(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = tStyle(11.5, if (isActive) FontWeight.W700 else FontWeight.W500,
                        if (isActive) Q.primary else Q.mutedFg),
                )
            }
        }
    }
}

// ═══════════════════ .search-bar (D-029 cilalı) ═══════════════════

@Composable
fun SearchBarField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = T.input.copy(color = Q.foreground),
        cursorBrush = SolidColor(Q.primary),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Q.card, RoundedCornerShape(15.dp))
                    .border(
                        1.4.dp.takeIf { focused } ?: 1.dp,
                        if (focused) Q.primary else Q.border,
                        RoundedCornerShape(15.dp),
                    )
                    .padding(horizontal = 14.dp),
            ) {
                Icon(
                    Icons.Filled.Search, null,
                    tint = if (focused) Q.primary else Q.mutedFg,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(hint, style = T.input.copy(color = Q.mutedFg), maxLines = 1)
                    }
                    inner()
                }
            }
        },
    )
}

// ═══════════════════ .category-chip ═══════════════════

@Composable
fun CategoryChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1f,
        animationSpec = tween(Motion.D2, easing = if (active) Motion.enter else Motion.ease),
        label = "chipScale",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .scale(scale)
            .height(36.dp)
            .background(
                if (active) Q.primary else Q.card,
                RoundedCornerShape(Q.rMd.dp),
            )
            .border(
                1.dp, if (active) Q.primary else Q.border,
                RoundedCornerShape(Q.rMd.dp),
            )
            .padding(horizontal = 13.dp),
    ) {
        Icon(icon, null, tint = if (active) Q.primaryFg else Q.mutedFg, modifier = Modifier.size(14.5.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = if (active) T.chipActive else T.chip)
    }
}

// ═══════════════════ .community-banner (sol yaşıl zolaqlı) ═══════════════════

@Composable
fun CommunityBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .scaleTap(interaction, enabled = onClick != null)
            .clickable(interactionSource = interaction, indication = null, enabled = onClick != null) {
                onClick?.invoke()
            }
            .background(Q.secondary)
            .border(1.dp, Color.Transparent)
            .padding(horizontal = 18.dp, vertical = 17.dp),
    ) {
        // Sol yaşıl zolaq (web border-left: 3px primary)
        Box(
            Modifier
                .width(3.dp)
                .height(44.dp)
                .background(Q.primary)
        ) { }
        Spacer(Modifier.width(13.dp))
        Box(
            Modifier
                .size(40.dp)
                .background(Q.card, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Q.primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = tStyle(13.5, FontWeight.W800))
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = tStyle(11.5, FontWeight.W400, Q.mutedFg).copy(lineHeight = 18.sp))
        }
        trailing?.invoke()
    }
}

// ═══════════════════ .ai-box ═══════════════════

@Composable
fun AiBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(Q.aiBoxBg, RoundedCornerShape(Q.rAiBox.dp))
            .border(1.2.dp, Q.primary, RoundedCornerShape(Q.rAiBox.dp))
            .padding(12.dp)
    ) { content() }
}

// ═══════════════════ .empty-state ═══════════════════

@Composable
fun EmptyState(icon: ImageVector, text: String, action: (@Composable () -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 45.dp),
    ) {
        Icon(icon, null, tint = Q.primary, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            text, textAlign = TextAlign.Center,
            style = tStyle(13.0, FontWeight.W400, Q.mutedFg).copy(lineHeight = 23.sp),
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

// ═══════════════════ .safety-note ═══════════════════

@Composable
fun SafetyNote() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
    ) {
        Icon(Icons.Filled.VerifiedUser, null, tint = Q.primary, modifier = Modifier.size(14.5.dp))
        Spacer(Modifier.width(6.dp))
        Text("Birlikdə daha güvənli, birlikdə daha gözəl.", style = tStyle(11.0, FontWeight.W400, Q.mutedFg))
    }
}

// ═══════════════════ .form-field ═══════════════════

@Composable
fun FormFieldX(
    label: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(top = 15.dp)) {
        Text(label, style = T.label)
        Spacer(Modifier.height(7.dp))
        content()
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(hint, style = tStyle(12.0, FontWeight.W400, Q.mutedFg))
        }
    }
}

// ═══════════════════ .view-heading ═══════════════════

@Composable
fun ViewHeading(title: String, subtitle: String, leading: (@Composable () -> Unit)? = null) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(title, style = T.h1)
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = T.subtitle)
    }
}

// ═══════════════════ .chat-message ═══════════════════

@Composable
fun ChatBubble(sender: String, body: String, mine: Boolean, status: String? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 15.dp),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .background(
                    if (mine) Q.secondary else Q.muted,
                    RoundedCornerShape(Q.rLg.dp),
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(sender, style = tStyle(10.5, FontWeight.W700, Q.primary))
            Spacer(Modifier.height(3.dp))
            Text(body, style = tStyle(13.0, FontWeight.W400).copy(lineHeight = 22.sp))
            if (mine && status != null) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (status == "oxundu") Icons.Filled.DoneAll else Icons.Filled.Done,
                        null,
                        tint = if (status == "oxundu") Q.primary else Q.mutedFg,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(status, style = tStyle(9.5, FontWeight.W400, Q.mutedFg))
                }
            }
        }
    }
}

// ═══════════════════ .live-badge ═══════════════════

@Composable
fun LiveBadge(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Q.glass, RoundedCornerShape(5.dp))
            .border(1.dp, Q.glassBorder, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Box(Modifier.size(5.dp).background(Q.accent, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = tStyle(10.0, FontWeight.W500, Color.White))
    }
}

// ═══════════════════ Şəkil + kölgə qradiyenti ═══════════════════

@Composable
fun ShadedImage(assetKey: String, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Image(
            painter = imagePaint(assetKey),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(height),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    Brush.verticalGradient(
                        0.0f to Q.photoShade.copy(alpha = 0f),
                        1.0f to Q.photoShade.copy(alpha = 0.5f),
                        startY = 0f,
                        endY = height.value * 2.5f,
                    )
                )
        )
    }
}

// ═══════════════════ ROL widgetları (rol.dart portu) ═══════════════════

/** Rezerv status çipi — rəng ROL E statusuna görə. */
@Composable
fun ResvStatusChip(status: ResvStatus) {
    val (bg, fg) = when (status) {
        ResvStatus.PENDING -> Q.rankBadgeBg to Q.accentFg
        ResvStatus.CONFIRMED -> Q.secondary to Q.primary
        ResvStatus.USED, ResvStatus.CANCELLED -> Q.muted to Q.mutedFg
        ResvStatus.NO_SHOW, ResvStatus.REJECTED -> Q.resvRejectedBg to Q.destructive
    }
    Text(
        status.label,
        style = tStyle(10.5, FontWeight.W700, fg),
        modifier = Modifier
            .background(bg, RoundedCornerShape(Q.rMd.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
    )
}

/** QR placeholder — koddan deterministik naxış (real QR backend fazasında). */
@Composable
fun QrPlaceholder(code: String, size: Dp = 132.dp) {
    val shape = RoundedCornerShape(Q.rLg.dp)
    Box(
        Modifier
            .size(size)
            .background(Color.White, shape)
            .border(1.dp, Q.border, shape)
            .padding(8.dp)
    ) {
        Canvas(Modifier.size(size - 16.dp)) {
            val n = 11f
            val cell = this.size.width / n
            // künc "gözləri"
            val eyes = listOf(0f to 0f, n - 3f to 0f, 0f to (n - 3f))
            for ((ox, oy) in eyes) {
                drawRoundRect(
                    color = Q.foreground,
                    topLeft = androidx.compose.ui.geometry.Offset(ox * cell, oy * cell),
                    size = androidx.compose.ui.geometry.Size(3 * cell, 3 * cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = androidx.compose.ui.geometry.Offset((ox + 0.6f) * cell, (oy + 0.6f) * cell),
                    size = androidx.compose.ui.geometry.Size(1.8f * cell, 1.8f * cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f),
                )
                drawRoundRect(
                    color = Q.foreground,
                    topLeft = androidx.compose.ui.geometry.Offset((ox + 1.05f) * cell, (oy + 1.05f) * cell),
                    size = androidx.compose.ui.geometry.Size(0.9f * cell, 0.9f * cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f),
                )
            }
            // deterministik hücrələr — Dart _cell(x,y) portu
            for (x in 0 until n.toInt()) {
                for (y in 0 until n.toInt()) {
                    val inEye = (x < 4 && y < 4) || (x > n - 5 && y < 4) || (x < 4 && y > n - 5)
                    if (inEye) continue
                    val v = (code[(x * 7 + y * 13) % code.length].code + x * 31 + y * 17) % 5
                    if (v == 0) {
                        drawRoundRect(
                            color = Q.foreground,
                            topLeft = androidx.compose.ui.geometry.Offset(x * cell + 1, y * cell + 1),
                            size = androidx.compose.ui.geometry.Size(cell - 2, cell - 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f),
                        )
                    }
                }
            }
        }
    }
}

/** Ulduz reytingi (yalnız göstəriş). */
@Composable
fun RatingStars(rating: Double, size: Dp = 14.dp) {
    Row {
        for (i in 1..5) {
            Icon(
                Icons.Filled.Star, null,
                tint = if (i <= kotlin.math.round(rating).toInt()) Q.accent else Q.border,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Nişan çipi (ROL F badges). */
@Composable
fun BadgeChip(badge: BadgeDef, earned: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (earned) Q.secondary else Q.muted,
                RoundedCornerShape(Q.rMd.dp),
            )
            .border(
                1.dp, if (earned) Q.primary else Q.border,
                RoundedCornerShape(Q.rMd.dp),
            )
            .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Icon(
            if (earned) Icons.Filled.MilitaryTech else Icons.Outlined.MilitaryTech,
            null, tint = if (earned) Q.primary else Q.mutedFg,
            modifier = Modifier.size(15.5.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(badge.name, style = tStyle(11.5, FontWeight.W700, if (earned) Q.primary else Q.mutedFg))
    }
}

/** Məkan mini kartı (Kəşf "Populyar məkanlar" / Xəritə alt kartı). */
@Composable
fun VenueMiniCard(
    imageKey: String, name: String, category: String, priceRange: String,
    rating: Double, discount: Int? = null, onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(174.dp)
            .scaleTap(interaction, enabled = onClick != null)
            .clickable(interactionSource = interaction, indication = null, enabled = onClick != null) {
                onClick?.invoke()
            }
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
    ) {
        Box(Modifier.fillMaxWidth().height(98.dp)) {
            Image(
                painter = imagePaint(imageKey), contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(98.dp),
            )
            if (discount != null && discount > 0) {
                Text(
                    "-$discount%",
                    style = tStyle(10.5, FontWeight.W800, Q.accentFg),
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Q.rankBadgeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Column(Modifier.padding(11.dp)) {
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = tStyle(13.5, FontWeight.W800))
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = Q.accent, modifier = Modifier.size(12.5.dp))
                Spacer(Modifier.width(3.dp))
                Text(String.format("%.1f", rating), style = tStyle(11.5, FontWeight.W600))
                Spacer(Modifier.width(6.dp))
                Text(
                    "$category · $priceRange",
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = T.meta,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Şəxs mini kartı (Kəşf "Sənə uyğun insanlar"). */
@Composable
fun PersonMiniCard(
    imageKey: String, name: String, age: Int, distanceLabel: String,
    matchPercent: Int, onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(132.dp)
            .scaleTap(interaction, enabled = onClick != null)
            .clickable(interactionSource = interaction, indication = null, enabled = onClick != null) {
                onClick?.invoke()
            }
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
            .padding(11.dp),
    ) {
        Box {
            Image(
                painter = imagePaint(imageKey), contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(CircleShape),
            )
            Text(
                "$matchPercent%",
                style = tStyle(10.5, FontWeight.W800, Q.primaryFg),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Q.primary, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 3.5.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("$name, $age", maxLines = 1, overflow = TextOverflow.Ellipsis, style = tStyle(12.5, FontWeight.W800))
        Spacer(Modifier.height(3.dp))
        Text(distanceLabel, style = T.meta)
    }
}

/** Təklif kartı (Endirimlər bölməsi). */
@Composable
fun OfferCard(
    venueName: String, label: String, requirement: String,
    autoByLevel: Boolean, unlocked: Boolean, action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .border(
                1.dp, if (unlocked) Q.border else Q.muted,
                RoundedCornerShape(Q.rLg.dp),
            )
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .background(
                    if (unlocked) Q.rankBadgeBg else Q.muted,
                    RoundedCornerShape(Q.rMd.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (autoByLevel) Icons.Filled.WorkspacePremium else Icons.Filled.ConfirmationNumber,
                null, tint = if (unlocked) Q.accentFg else Q.mutedFg,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = tStyle(14.5, FontWeight.W800))
                if (autoByLevel) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Səviyyən ilə açıq",
                        style = tStyle(9.5, FontWeight.W700, Q.primary),
                        modifier = Modifier
                            .background(Q.secondary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("$venueName · $requirement", maxLines = 1, overflow = TextOverflow.Ellipsis, style = T.meta)
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            action()
        }
    }
}

// ═══════════════════ Brend (brand.dart portu) ═══════════════════

/** "qoshul.az" wordmark — Link ikonu -12°, qoshul yaşıl + .az tünd. */
@Composable
fun Wordmark(fontSize: Int = 25) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Link, null, tint = Q.primary,
            modifier = Modifier
                .size((fontSize + 2).dp)
                .androidxScaleRotate(),
        )
        Spacer(Modifier.width(7.dp))
        Text("qoshul", style = tStyle(fontSize.toDouble(), FontWeight.W800, Q.primary))
        Text(".az", style = tStyle(fontSize.toDouble(), FontWeight.W800, Q.foreground))
    }
}

/** Link ikonunun -12° fırlanması (web wordmark). */
private fun Modifier.androidxScaleRotate(): Modifier =
    this.then(Modifier.graphicsLayer { rotationZ = -12f })
