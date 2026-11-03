package au.com.foodtruckcentre.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.foodtruckcentre.app.R
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.ui.components.ChoicePills
import au.com.foodtruckcentre.app.ui.components.FtcCard
import au.com.foodtruckcentre.app.ui.components.FtcTextField
import au.com.foodtruckcentre.app.ui.components.IconBadge
import au.com.foodtruckcentre.app.ui.components.LoadingBlocks
import au.com.foodtruckcentre.app.ui.components.MessageState
import au.com.foodtruckcentre.app.ui.components.PrimaryButton
import au.com.foodtruckcentre.app.ui.components.RemoteImage
import au.com.foodtruckcentre.app.ui.components.RichBlocks
import au.com.foodtruckcentre.app.ui.components.ScreenHeader
import au.com.foodtruckcentre.app.ui.components.SecondaryButton
import au.com.foodtruckcentre.app.ui.components.openEmail
import au.com.foodtruckcentre.app.ui.components.openUrl
import au.com.foodtruckcentre.app.ui.theme.Ftc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ---------------- Services ---------------- */

@Composable
fun ServicesScreen(onQuote: (SiteContent.Service) -> Unit, onBack: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState())) {
        ScreenHeader(
            "Catering Services", eyebrow = "What we cater",
            subtitle = "Delicious and flexible food truck catering for corporate parties, weddings and other events.",
            onBack = onBack,
        )
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SiteContent.services.forEachIndexed { i, s ->
                FtcCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(s.image), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(104.dp).clip(RoundedCornerShape(20.dp)),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("0${i + 1}", style = MaterialTheme.typography.labelMedium, color = Ftc.Red)
                            Text(s.title, style = MaterialTheme.typography.titleMedium, color = Ftc.Navy)
                            Spacer(Modifier.height(4.dp))
                            Text(s.description, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Ftc.Line))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (s.presetEventType != null) "Event type: ${s.presetEventType}" else "Tell us about your event",
                            style = MaterialTheme.typography.bodySmall, color = Ftc.Muted, modifier = Modifier.weight(1f),
                        )
                        PrimaryButton("Request a Quote", { onQuote(s) }, icon = Icons.AutoMirrored.Rounded.ArrowForward)
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

/* ---------------- Featuring ---------------- */

@Composable
fun FeaturingScreen(content: ContentViewModel, onQuote: (SiteContent.Cuisine?) -> Unit, onBack: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState())) {
        ScreenHeader("Featuring", eyebrow = "This month", subtitle = "The food truck we're featuring right now, straight from our website.", onBack = onBack)
        Column(Modifier.padding(18.dp)) {
            when (val state = content.featured) {
                Loadable.Loading -> LoadingBlocks()
                is Loadable.Error -> MessageState(
                    title = if (state.offline) "You're offline" else "Couldn't load the featured truck",
                    message = state.message, offline = state.offline, onAction = content::loadFeatured,
                )
                is Loadable.Ready -> {
                    val f = state.value
                    if (f == null) {
                        MessageState(
                            title = "No featured truck right now",
                            message = "Check back soon – we'll feature a new food truck here. You can still request quotes from our food trucks at any time.",
                            icon = Icons.Rounded.Storefront,
                            actionLabel = "Request a Quote", onAction = { onQuote(null) },
                        )
                    } else {
                        FtcCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                            if (f.imageUrl != null) {
                                RemoteImage(
                                    f.imageUrl, Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                    contentDescription = f.headline ?: "Featured food truck",
                                )
                            }
                            Column(Modifier.padding(20.dp)) {
                                Row(
                                    Modifier.clip(RoundedCornerShape(50)).background(Ftc.MustardSoft).padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.Verified, null, tint = Ftc.Navy, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("FEATURED FOOD TRUCK", style = MaterialTheme.typography.labelMedium, color = Ftc.Navy)
                                }
                                if (f.headline != null) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(f.headline, style = MaterialTheme.typography.headlineSmall, color = Ftc.Navy)
                                }
                                if (f.cuisine != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconBadge(cuisineIcon(f.cuisine), size = 40.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text("Cuisine", style = MaterialTheme.typography.labelSmall, color = Ftc.Muted)
                                            Text(f.cuisine.label, style = MaterialTheme.typography.titleSmall, color = Ftc.Ink)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                RichBlocks(f.blocks, onOpenQuote = { onQuote(f.cuisine) }, showImages = false)
                                Spacer(Modifier.height(20.dp))
                                PrimaryButton(
                                    if (f.cuisine != null) "Request a Quote – ${f.cuisine.label}" else "Request a Quote",
                                    { onQuote(f.cuisine) }, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

/* ---------------- Contact ---------------- */

@Composable
fun ContactScreen(vm: ContactViewModel, content: ContentViewModel, onLegal: (SiteContent.LegalPage) -> Unit, onBack: (() -> Unit)?) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.touch(); content.refreshContactEmail() }
    Column(
        Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.ime),
    ) {
        Box {
            Image(
                painterResource(R.drawable.photo_contact), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(250.dp),
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Ftc.NavyDeep.copy(alpha = 0.55f), Ftc.NavyDeep.copy(alpha = 0.9f)))))
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 22.dp, end = 22.dp, top = 60.dp)) {
                Text("GET IN TOUCH", style = MaterialTheme.typography.labelMedium, color = Ftc.Mustard)
                Spacer(Modifier.height(6.dp))
                Text("Contact Us", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(SiteContent.CONTACT_INTRO, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Column(Modifier.padding(horizontal = 18.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val sent = vm.sentId
            if (sent != null) {
                FtcCard(Modifier.fillMaxWidth()) {
                    SuccessBadge()
                    Spacer(Modifier.height(16.dp))
                    Text("Message sent", style = MaterialTheme.typography.headlineSmall, color = Ftc.Navy, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Thanks for getting in touch. Your message has been delivered to our team and we'll reply by email.",
                        style = MaterialTheme.typography.bodyMedium, color = Ftc.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Reference: $sent", style = MaterialTheme.typography.labelMedium, color = Ftc.Navy, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(18.dp))
                    SecondaryButton("Send another message", { vm.sentId = null }, modifier = Modifier.fillMaxWidth())
                }
            } else {
                FtcCard(Modifier.fillMaxWidth()) {
                    Text("Send us a message", style = MaterialTheme.typography.titleLarge, color = Ftc.Navy)
                    Spacer(Modifier.height(14.dp))
                    Text("Topic", style = MaterialTheme.typography.titleSmall, color = Ftc.Ink)
                    Spacer(Modifier.height(8.dp))
                    ChoicePills(vm.topics.take(2), vm.topic, { vm.topic = it })
                    Spacer(Modifier.height(8.dp))
                    ChoicePills(vm.topics.drop(2), vm.topic, { vm.topic = it })
                    Spacer(Modifier.height(16.dp))
                    FtcTextField(
                        vm.name, { vm.name = it; vm.errors.remove("name") }, "Your name", required = true,
                        error = vm.errors["name"], leadingIcon = Icons.Rounded.Person, capitalization = KeyboardCapitalization.Words, maxLength = 100,
                    )
                    Spacer(Modifier.height(12.dp))
                    FtcTextField(
                        vm.email, { vm.email = it.trim(); vm.errors.remove("email") }, "Your email", required = true,
                        error = vm.errors["email"], leadingIcon = Icons.Rounded.Email, keyboardType = KeyboardType.Email, maxLength = 254,
                    )
                    Spacer(Modifier.height(12.dp))
                    FtcTextField(
                        vm.message, { vm.message = it; vm.errors.remove("message") }, "Message", required = true,
                        placeholder = "How can we help?", error = vm.errors["message"], singleLine = false, minLines = 5,
                        capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, maxLength = 3000,
                    )
                    Spacer(Modifier.height(16.dp))
                    ErrorBanner(vm.banner)
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton("Send Message", vm::submit, loading = vm.submitting, icon = Icons.AutoMirrored.Rounded.Send, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    ConsentText(onLegal)
                }
            }

            val email = content.contactEmail
            FtcCard(Modifier.fillMaxWidth()) {
                Text("Other ways to reach us", style = MaterialTheme.typography.titleMedium, color = Ftc.Navy)
                Spacer(Modifier.height(12.dp))
                if (email != null) {
                    ContactRow(Icons.Rounded.Email, "Email", email) { context.openEmail(email, "Food Truck Centre enquiry") }
                    Spacer(Modifier.height(10.dp))
                }
                ContactRow(Icons.Rounded.PhotoCamera, "Instagram", "@foodtruckcentre") { context.openUrl(SiteContent.INSTAGRAM_URL) }
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ftc.Cream)
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Ftc.Muted)
            Text(value, style = MaterialTheme.typography.titleSmall, color = Ftc.Navy)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ftc.Red, modifier = Modifier.size(18.dp))
    }
}

/* ---------------- Legal ---------------- */

@Composable
fun LegalScreen(page: SiteContent.LegalPage, content: ContentViewModel, onQuote: () -> Unit, onBack: () -> Unit) {
    LaunchedEffect(page) { content.loadLegal(page) }
    Column(Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState())) {
        ScreenHeader(page.title, eyebrow = "Food Truck Centre (FTC)", onBack = onBack)
        Column(Modifier.padding(18.dp)) {
            when (val state = content.legal[page] ?: Loadable.Loading) {
                Loadable.Loading -> LoadingBlocks(lines = 12)
                is Loadable.Error -> MessageState(
                    title = "Couldn't load this page", message = state.message, offline = state.offline,
                    onAction = { content.loadLegal(page, force = true) },
                )
                is Loadable.Ready -> {
                    val doc = state.value
                    if (doc.offlineCopy) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ftc.MustardSoft).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Info, null, tint = Ftc.Navy)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "You're offline, so this is the copy saved in the app (website version of ${doc.updated.take(10)}). Connect to the internet to see the latest version.",
                                style = MaterialTheme.typography.bodySmall, color = Ftc.Ink, modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    FtcCard(Modifier.fillMaxWidth()) {
                        RichBlocks(doc.blocks.filterNot { it is au.com.foodtruckcentre.app.data.HtmlBlock.Heading && it.text.equals("FOOD TRUCK CENTRE (FTC)", true) }, onOpenQuote = onQuote)
                    }
                    if (doc.updated.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Last updated on the website: ${doc.updated.take(10)}", style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

/* ---------------- About / How to book ---------------- */

@Composable
fun AboutScreen(onQuote: () -> Unit, onLegal: (SiteContent.LegalPage) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState())) {
        ScreenHeader("How to Book with Us", eyebrow = "About Food Truck Centre", subtitle = "Time has changed", onBack = onBack)
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Image(
                painterResource(R.drawable.photo_howto), "Guests at a food truck event", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(28.dp)),
            )
            FtcCard(Modifier.fillMaxWidth()) {
                Text(SiteContent.INTRO, style = MaterialTheme.typography.bodyLarge, color = Ftc.Ink)
                Spacer(Modifier.height(12.dp))
                Text(SiteContent.INTRO_2, style = MaterialTheme.typography.bodyLarge, color = Ftc.Ink)
                Spacer(Modifier.height(12.dp))
                Text(SiteContent.INTRO_3.replace(" below", ""), style = MaterialTheme.typography.bodyLarge, color = Ftc.Ink)
            }
            Text("The process", style = MaterialTheme.typography.headlineSmall, color = Ftc.Navy)
            val steps = listOf(
                Triple(Icons.AutoMirrored.Rounded.MenuBook, "Choose up to three cuisines", "From Burgers / American to Desserts / Coffee / Sweets – tick 3 max."),
                Triple(Icons.Rounded.EventAvailable, "Share your event details", "Your date, serving time, address and postcode, type of event, number of meals (minimum 50) and site details like power and truck access."),
                Triple(Icons.Rounded.Forum, "Receive quotes and menus", "Your enquiry is shared so you can receive quotes and menus from a number of sensational food trucks."),
                Triple(Icons.Rounded.Check, "Confirm directly with the vendor", "All bookings are subject to vendor acceptance. Once you accept a vendor's quote, the booking becomes an agreement between you and that vendor."),
            )
            steps.forEachIndexed { i, (icon, title, body) ->
                FtcCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(icon, tint = Color.White, background = if (i == 3) Ftc.Red else Ftc.Navy, size = 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Step ${i + 1}", style = MaterialTheme.typography.labelMedium, color = Ftc.Red)
                            Text(title, style = MaterialTheme.typography.titleMedium, color = Ftc.Navy)
                            Spacer(Modifier.height(4.dp))
                            Text(body, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ftc.MustardSoft).padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.Info, null, tint = Ftc.Navy)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Sending an enquiry is not a confirmed booking. Pricing, deposits and payment are agreed directly with the food truck vendor – no payment is taken in this app.",
                    style = MaterialTheme.typography.bodySmall, color = Ftc.Ink,
                )
            }
            PrimaryButton("Request a Quote", onQuote, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Terms", { onLegal(SiteContent.LegalPage.TERMS) }, modifier = Modifier.weight(1f))
                SecondaryButton("Vendor Terms", { onLegal(SiteContent.LegalPage.VENDOR) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(30.dp))
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

/* ---------------- Success ---------------- */

@Composable
fun SuccessBadge() {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(112.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .clip(CircleShape).background(Ftc.Success.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(78.dp).clip(CircleShape).background(Ftc.Success), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, "Success", tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
fun SuccessScreen(enquiryId: String, firstName: String, onHome: () -> Unit, onAbout: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(200); reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ftc.Navy, Ftc.NavyDeep))),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Image(
                painterResource(R.drawable.logo_badge), "Food Truck Centre",
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White).padding(3.dp),
            )
            Spacer(Modifier.height(28.dp))
            FtcCard(Modifier.fillMaxWidth().graphicsLayer { alpha = reveal.value; translationY = (1 - reveal.value) * 80f }, padding = 24.dp) {
                SuccessBadge()
                Spacer(Modifier.height(18.dp))
                Text(
                    if (firstName.isNotBlank()) "Thank you, $firstName!" else "Thank you!",
                    style = MaterialTheme.typography.headlineMedium, color = Ftc.Navy, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your quote request has been sent to the Food Truck Centre team.",
                    style = MaterialTheme.typography.bodyLarge, color = Ftc.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ftc.Cream)
                        .border(1.dp, Ftc.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your enquiry reference", style = MaterialTheme.typography.labelSmall, color = Ftc.Muted)
                        Text(enquiryId, style = MaterialTheme.typography.titleLarge, color = Ftc.Navy)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("What happens next", style = MaterialTheme.typography.titleMedium, color = Ftc.Navy)
                Spacer(Modifier.height(10.dp))
                NextStep("1", "We review your event details.")
                NextStep("2", "You'll be contacted by email or phone with quotes and menus from suitable food trucks.")
                NextStep("3", "Your booking is confirmed only once you accept a vendor's quote.")
                Spacer(Modifier.height(22.dp))
                PrimaryButton("Back to Home", onHome, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                SecondaryButton("How booking works", onAbout, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun NextStep(n: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(Ftc.MustardSoft), contentAlignment = Alignment.Center) {
            Text(n, style = MaterialTheme.typography.labelMedium, color = Ftc.Navy)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ftc.Ink, modifier = Modifier.weight(1f))
    }
}

/* ---------------- Animated intro (after the system splash) ---------------- */

@Composable
fun IntroScreen(onDone: () -> Unit) {
    val logo = remember { Animatable(0.4f) }
    val truck = remember { Animatable(-1f) }
    val text = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        launch { truck.animateTo(0f, tween(900, easing = FastOutSlowInEasing)) }
        delay(350)
        text.animateTo(1f, tween(600))
        delay(650)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ftc.Cream, Color.White, Ftc.Cream))), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(top = 0.dp)) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Ftc.Navy.copy(alpha = 0.08f)))),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.logo_badge), "Food Truck Centre",
                modifier = Modifier.size(210.dp).graphicsLayer {
                    scaleX = logo.value; scaleY = logo.value; translationX = truck.value * 300f
                },
            )
            Spacer(Modifier.height(18.dp))
            Column(Modifier.graphicsLayer { alpha = text.value; translationY = (1 - text.value) * 30f }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Food Truck Centre", style = MaterialTheme.typography.headlineMedium, color = Ftc.Navy)
                Spacer(Modifier.height(4.dp))
                Text("Book Australia’s best food trucks", style = MaterialTheme.typography.bodyMedium, color = Ftc.Muted)
            }
        }
    }
}
