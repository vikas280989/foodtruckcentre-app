package au.com.foodtruckcentre.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.foodtruckcentre.app.R
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.data.SiteContent.Cuisine
import au.com.foodtruckcentre.app.ui.components.FtcCard
import au.com.foodtruckcentre.app.ui.components.IconBadge
import au.com.foodtruckcentre.app.ui.components.PrimaryButton
import au.com.foodtruckcentre.app.ui.components.RemoteImage
import au.com.foodtruckcentre.app.ui.components.SecondaryButton
import au.com.foodtruckcentre.app.ui.components.SectionHeader
import au.com.foodtruckcentre.app.ui.components.openUrl
import au.com.foodtruckcentre.app.ui.theme.Ftc

@Composable
fun HomeScreen(
    content: ContentViewModel,
    onQuote: (Cuisine?, String?) -> Unit,
    onServices: () -> Unit,
    onService: (SiteContent.Service) -> Unit,
    onFeaturing: () -> Unit,
    onContact: () -> Unit,
    onAbout: () -> Unit,
    onLegal: (SiteContent.LegalPage) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().background(Ftc.Cream), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Hero(onQuote = { onQuote(null, null) }, onHowItWorks = onAbout) }
        item { Highlights(Modifier.offset(y = (-36).dp)) }
        item { HowToBook(onQuote = { onQuote(null, null) }, onAbout = onAbout) }
        item {
            SectionHeader(
                "Cuisines", "What would you like to serve?",
                subtitle = "Tap a cuisine to start your quote – choose up to three.",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
            )
        }
        item { CuisineCarousel(onPick = { onQuote(it, null) }) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 40.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                SectionHeader("Catering services", "Catering for every occasion", modifier = Modifier.weight(1f))
                Text(
                    "See all", style = MaterialTheme.typography.labelLarge, color = Ftc.Red,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onServices).padding(8.dp),
                )
            }
        }
        item { ServiceCarousel(onService) }
        item { FeaturedTeaser(content, onFeaturing, Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp)) }
        item { ContactCta(onContact, Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) }
        item { Footer(onLegal, onAbout, Modifier.padding(top = 36.dp)) }
    }
}

@Composable
private fun Hero(onQuote: () -> Unit, onHowItWorks: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxWidth().height(600.dp)) {
        Image(
            painterResource(R.drawable.photo_hero),
            contentDescription = "Guests ordering from a food truck at an outdoor event",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = 1.12f - 0.08f * reveal.value; scaleY = scaleX },
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Ftc.NavyDeep.copy(alpha = 0.55f),
                    0.35f to Ftc.NavyDeep.copy(alpha = 0.15f),
                    0.62f to Ftc.NavyDeep.copy(alpha = 0.72f),
                    1f to Ftc.NavyDeep.copy(alpha = 0.97f),
                )
            )
        )
        Row(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.logo_badge), "Food Truck Centre logo",
                modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.White).padding(3.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Food Truck Centre", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("foodtruckcentre.com.au", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 52.dp)
                .graphicsLayer { alpha = reveal.value; translationY = (1f - reveal.value) * 60f },
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Ftc.Mustard).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalShipping, null, tint = Ftc.Navy, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("FOOD TRUCK CATERING", style = MaterialTheme.typography.labelMedium, color = Ftc.Navy)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                SiteContent.HERO_TITLE, style = MaterialTheme.typography.displayMedium, color = Color.White,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(SiteContent.INTRO_2, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f))
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Request a Quote", onQuote, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(
                "How it works",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(onClick = onHowItWorks).padding(12.dp),
            )
        }
    }
}

@Composable
private fun Highlights(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SiteContent.highlights.forEach { h ->
            FtcCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint, bg) = when (h.kind) {
                        SiteContent.Highlight.Kind.MENUS -> Triple(Icons.Rounded.RestaurantMenu, Ftc.Red, Ftc.RedSoft)
                        SiteContent.Highlight.Kind.EVENTS -> Triple(Icons.Rounded.EventAvailable, Ftc.Navy, Ftc.NavySoft)
                        SiteContent.Highlight.Kind.EXPERIENCE -> Triple(Icons.Rounded.WorkspacePremium, Color(0xFF9A6B00), Ftc.MustardSoft)
                    }
                    IconBadge(icon, tint = tint, background = bg, size = 54.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.title, style = MaterialTheme.typography.titleMedium, color = Ftc.Navy)
                        Text(h.body, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToBook(onQuote: () -> Unit, onAbout: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("How to book", "How to Book with Us")
        Spacer(Modifier.height(16.dp))
        Box {
            Image(
                painterResource(R.drawable.photo_howto), "Guests enjoying food from a food truck at a garden event",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(28.dp)),
            )
            Row(
                Modifier.align(Alignment.BottomStart).padding(14.dp).clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.94f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Star, null, tint = Ftc.Mustard, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Time has changed", style = MaterialTheme.typography.labelLarge, color = Ftc.Navy)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(SiteContent.INTRO, style = MaterialTheme.typography.bodyLarge, color = Ftc.Ink)
        Spacer(Modifier.height(18.dp))
        val steps = listOf(
            Triple(Icons.AutoMirrored.Rounded.MenuBook, "Choose your cuisines", "Tick up to three food truck cuisines."),
            Triple(Icons.Rounded.EventAvailable, "Share your event details", "Date, location, number of meals and site access."),
            Triple(Icons.Rounded.Forum, "Receive quotes and menus", "From a number of sensational food trucks."),
        )
        steps.forEachIndexed { i, (icon, title, body) ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    IconBadge(icon, tint = Color.White, background = if (i == 2) Ftc.Red else Ftc.Navy, size = 46.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. $title", style = MaterialTheme.typography.titleSmall, color = Ftc.Navy)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Get quotes", onQuote, modifier = Modifier.weight(1f), icon = null)
            SecondaryButton("Learn more", onAbout, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CuisineCarousel(onPick: (Cuisine) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(Cuisine.entries, key = { it.name }) { c ->
            val warm = c.ordinal % 2 == 0
            Column(
                Modifier
                    .width(158.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (warm) Color.White else Ftc.Navy)
                    .border(1.dp, if (warm) Ftc.Line else Ftc.Navy, RoundedCornerShape(24.dp))
                    .clickable(onClickLabel = "Request a quote for ${c.label}") { onPick(c) }
                    .padding(16.dp),
            ) {
                IconBadge(
                    cuisineIcon(c),
                    tint = if (warm) Ftc.Red else Ftc.Navy,
                    background = if (warm) Ftc.RedSoft else Ftc.Mustard,
                    size = 50.dp,
                )
                Spacer(Modifier.height(14.dp))
                Text(c.label, style = MaterialTheme.typography.titleSmall, color = if (warm) Ftc.Navy else Color.White, minLines = 2, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(
                    c.description, style = MaterialTheme.typography.bodySmall,
                    color = if (warm) Ftc.Muted else Color.White.copy(alpha = 0.75f), minLines = 2, maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun ServiceCarousel(onService: (SiteContent.Service) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(SiteContent.services, key = { it.id }) { s ->
            FtcCard(Modifier.width(230.dp), onClick = { onService(s) }, padding = 0.dp) {
                Image(
                    painterResource(s.image), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
                Column(Modifier.padding(16.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, color = Ftc.Navy, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text(s.description, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted, minLines = 3, maxLines = 3)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Request a Quote", style = MaterialTheme.typography.labelLarge, color = Ftc.Red)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ftc.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedTeaser(content: ContentViewModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val state = content.featured
    val ready = (state as? Loadable.Ready)?.value
    FtcCard(modifier.fillMaxWidth(), onClick = onOpen, padding = 0.dp, color = Ftc.Navy) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            val img = ready?.imageUrl
            if (img != null) {
                RemoteImage(img, Modifier.fillMaxSize(), contentDescription = ready?.headline)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Ftc.Navy, Ftc.Red))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Photo, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                }
            }
            Box(
                Modifier.padding(14.dp).clip(RoundedCornerShape(50)).background(Ftc.Mustard).padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("FEATURING", style = MaterialTheme.typography.labelMedium, color = Ftc.Navy) }
        }
        Column(Modifier.padding(20.dp)) {
            Text(
                when {
                    ready?.headline != null -> ready.headline
                    state is Loadable.Loading -> "Loading this month's featured truck…"
                    else -> "See who we're featuring"
                },
                style = MaterialTheme.typography.titleLarge, color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View featured truck", style = MaterialTheme.typography.labelLarge, color = Ftc.Mustard)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ftc.Mustard, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ContactCta(onContact: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Ftc.Red, Ftc.RedDeep)))
            .drawWithContent {
                drawContent()
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.35f, center = androidx.compose.ui.geometry.Offset(size.width, 0f))
            }
            .padding(24.dp),
    ) {
        Column {
            Text("Have a question?", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(SiteContent.CONTACT_INTRO + ".", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Contact us", onContact, container = Color.White, content = Ftc.Red)
        }
    }
}

@Composable
fun Footer(onLegal: (SiteContent.LegalPage) -> Unit, onAbout: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxWidth().background(Ftc.NavyDeep).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(R.drawable.logo_badge), "Food Truck Centre logo",
            modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White).padding(4.dp),
        )
        Spacer(Modifier.height(18.dp))
        val links = listOf(
            "How to book" to onAbout,
            "Privacy Policy" to { onLegal(SiteContent.LegalPage.PRIVACY) },
            "Terms and Conditions" to { onLegal(SiteContent.LegalPage.TERMS) },
            "Vendor Terms" to { onLegal(SiteContent.LegalPage.VENDOR) },
            "Instagram" to { context.openUrl(SiteContent.INSTAGRAM_URL) },
            "Visit the website" to { context.openUrl("https://foodtruckcentre.com.au/") },
        )
        links.forEach { (label, action) ->
            Text(
                label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = action).padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(SiteContent.COPYRIGHT, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
    }
}
