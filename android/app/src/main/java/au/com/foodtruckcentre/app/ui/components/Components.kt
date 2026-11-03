package au.com.foodtruckcentre.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.foodtruckcentre.app.ui.theme.Ftc

/* ---------- Buttons ---------- */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowForward,
    container: Color = Ftc.Red,
    content: Color = Color.White,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val bg by animateColorAsState(if (enabled || loading) container else container.copy(alpha = 0.45f), label = "bg")
    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 56.dp)
            .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(50), ambientColor = container, spotColor = container)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(color = Color.White),
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = content, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sending…", style = MaterialTheme.typography.labelLarge, color = content)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, style = MaterialTheme.typography.labelLarge, color = content)
                if (icon != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Ftc.Navy,
    icon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

/* ---------- Layout pieces ---------- */

@Composable
fun SectionHeader(eyebrow: String?, title: String, modifier: Modifier = Modifier, subtitle: String? = null, light: Boolean = false) {
    Column(modifier) {
        if (eyebrow != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(22.dp).height(3.dp).clip(CircleShape).background(Ftc.Mustard))
                Spacer(Modifier.width(8.dp))
                Text(
                    eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (light) Ftc.Mustard else Ftc.Red,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = if (light) Color.White else Ftc.Navy)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (light) Color.White.copy(alpha = 0.8f) else Ftc.Muted)
        }
    }
}

@Composable
fun FtcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val base = modifier
        .shadow(14.dp, shape, ambientColor = Ftc.Navy.copy(alpha = 0.25f), spotColor = Ftc.Navy.copy(alpha = 0.18f))
        .clip(shape)
        .background(color)
    Column(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(padding),
        content = content,
    )
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = Ftc.Red, background: Color = Ftc.RedSoft, size: Dp = 48.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/* ---------- States ---------- */

@Composable
fun Modifier.shimmer(): Modifier {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -600f, targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x",
    )
    return this.background(
        Brush.linearGradient(
            listOf(Ftc.CreamDeep, Color.White, Ftc.CreamDeep),
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x + 500f, 200f),
        )
    )
}

@Composable
fun LoadingBlocks(modifier: Modifier = Modifier, lines: Int = 7) {
    Column(modifier.semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp)).shimmer())
        Box(Modifier.fillMaxWidth(0.7f).height(26.dp).clip(RoundedCornerShape(8.dp)).shimmer())
        repeat(lines) { i ->
            Box(Modifier.fillMaxWidth(if (i % 3 == 2) 0.6f else 1f).height(14.dp).clip(RoundedCornerShape(6.dp)).shimmer())
        }
    }
}

@Composable
fun MessageState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.ErrorOutline,
    offline: Boolean = false,
    actionLabel: String? = "Try again",
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Ftc.MustardSoft).border(6.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (offline) Icons.Rounded.CloudOff else icon, null, tint = Ftc.Navy, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ftc.Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ftc.Muted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            SecondaryButton(actionLabel, onAction, icon = Icons.Rounded.Refresh)
        }
    }
}

/* ---------- Form fields ---------- */

@Composable
fun FtcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int = 200,
    leadingIcon: ImageVector? = null,
    autofill: String? = null,
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            label = { Text(if (required) "$label *" else label) },
            placeholder = placeholder?.let { { Text(it, color = Ftc.Muted.copy(alpha = 0.7f)) } },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = if (error != null) Ftc.Error else Ftc.Navy.copy(alpha = 0.7f)) } },
            isError = error != null,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = keyboardType == KeyboardType.Text && !singleLine),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ftc.Navy,
                unfocusedBorderColor = Ftc.Line,
                focusedLabelColor = Ftc.Navy,
                cursorColor = Ftc.Red,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFFFCF8),
                errorContainerColor = Color(0xFFFFF7F7),
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

@Composable
fun FieldError(error: String?) {
    if (error != null) {
        Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = Ftc.Error, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = Ftc.Error)
        }
    }
}

/** Single-choice pill options (used for the website's dropdown questions). */
@Composable
fun ChoicePills(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            val isSel = option == selected
            val bg by animateColorAsState(if (isSel) Ftc.Navy else Color.White, label = "pill")
            val fg by animateColorAsState(if (isSel) Color.White else Ftc.Navy, label = "pillText")
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(1.5.dp, if (isSel) Ftc.Navy else Ftc.Line, RoundedCornerShape(14.dp))
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium),
                    color = fg,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/* ---------- Intents ---------- */

fun Context.openUrl(url: String) {
    if (!url.startsWith("https://")) return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app available to open this link.", Toast.LENGTH_SHORT).show()
    }
}

fun Context.openEmail(address: String, subject: String = "") {
    val uri = Uri.parse("mailto:$address" + if (subject.isNotEmpty()) "?subject=" + Uri.encode(subject) else "")
    try {
        startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No email app found. Please email $address", Toast.LENGTH_LONG).show()
    }
}
