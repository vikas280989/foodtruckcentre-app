package au.com.foodtruckcentre.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import au.com.foodtruckcentre.app.R
import au.com.foodtruckcentre.app.data.HtmlBlock
import au.com.foodtruckcentre.app.data.Span
import au.com.foodtruckcentre.app.ui.theme.Ftc
import coil.compose.SubcomposeAsyncImage

/** Navy header used by inner screens: back button, eyebrow, big title, optional subtitle. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Ftc.NavyDeep, Ftc.Navy)))
            .drawBehind {
                // Subtle brand accents: awning yellow and truck red circles.
                drawCircle(Ftc.Mustard.copy(alpha = 0.16f), radius = size.width * 0.32f, center = Offset(size.width * 1.02f, size.height * 0.05f))
                drawCircle(Ftc.Red.copy(alpha = 0.22f), radius = size.width * 0.18f, center = Offset(size.width * 0.92f, size.height * 1.0f))
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 26.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painterResource(R.drawable.logo_badge), contentDescription = "Food Truck Centre",
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                    )
                }
            }
            Column(Modifier.padding(start = 12.dp, top = 10.dp)) {
                if (eyebrow != null) {
                    Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = Ftc.Mustard)
                    Spacer(Modifier.height(6.dp))
                }
                Text(title, style = MaterialTheme.typography.displaySmall, color = Color.White)
                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f))
                }
            }
        }
    }
}

/**
 * Renders website content blocks natively. Links to the website's quote form open the in-app form;
 * other https links open in the browser.
 */
@Composable
fun RichBlocks(
    blocks: List<HtmlBlock>,
    modifier: Modifier = Modifier,
    onOpenQuote: () -> Unit = {},
    showImages: Boolean = true,
) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is HtmlBlock.Heading -> {
                    Spacer(Modifier.height(if (block.level <= 2) 10.dp else 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (block.number != null) {
                            Box(
                                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Ftc.RedSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(block.number.toString(), style = MaterialTheme.typography.labelLarge, color = Ftc.Red)
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            block.text,
                            style = if (block.level <= 2) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                            color = Ftc.Navy,
                        )
                    }
                }
                is HtmlBlock.Paragraph -> Text(
                    spansToAnnotated(block.spans, onOpenQuote, context::openUrl),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ftc.Ink,
                )
                is HtmlBlock.Bullet -> Row(Modifier.padding(start = 4.dp)) {
                    Box(Modifier.padding(top = 10.dp).size(7.dp).clip(CircleShape).background(Ftc.Mustard))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        spansToAnnotated(block.spans, onOpenQuote, context::openUrl),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ftc.Ink,
                    )
                }
                is HtmlBlock.Image -> if (showImages) RemoteImage(block.url, Modifier.fillMaxWidth().height(210.dp))
            }
        }
    }
}

private fun isQuoteLink(url: String): Boolean {
    val u = url.lowercase()
    return (u.startsWith("https://foodtruckcentre.com.au") || u.startsWith("https://www.foodtruckcentre.com.au") || u.startsWith("#")) &&
        (u.contains("#form") || u.trimEnd('/').endsWith("foodtruckcentre.com.au") || u.contains("home-2"))
}

private fun spansToAnnotated(spans: List<Span>, onOpenQuote: () -> Unit, openUrl: (String) -> Unit) = buildAnnotatedString {
    spans.forEach { s ->
        val style = SpanStyle(
            fontWeight = if (s.bold) FontWeight.SemiBold else null,
            fontStyle = if (s.italic) FontStyle.Italic else null,
        )
        val link = s.link
        if (link != null && (link.startsWith("https://") || link.startsWith("#") || link.startsWith("mailto:"))) {
            val annotation = LinkAnnotation.Clickable(
                tag = link,
                styles = TextLinkStyles(SpanStyle(color = Ftc.Blue, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)),
            ) {
                if (isQuoteLink(link)) onOpenQuote() else if (link.startsWith("https://")) openUrl(link)
            }
            withLink(annotation) { withStyle(style) { append(s.text) } }
        } else {
            withStyle(style) { append(s.text) }
        }
    }
}

@Composable
fun RemoteImage(url: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
        loading = { Box(Modifier.fillMaxSize().shimmer()) },
        error = {
            Box(Modifier.fillMaxSize().background(Ftc.CreamDeep), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.logo_badge), null, modifier = Modifier.size(72.dp))
            }
        },
    )
}

