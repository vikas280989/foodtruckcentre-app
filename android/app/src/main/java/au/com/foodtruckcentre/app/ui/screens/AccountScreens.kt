package au.com.foodtruckcentre.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import au.com.foodtruckcentre.app.R
import au.com.foodtruckcentre.app.data.MyEnquiry
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.ui.components.FieldError
import au.com.foodtruckcentre.app.ui.components.FtcCard
import au.com.foodtruckcentre.app.ui.components.FtcTextField
import au.com.foodtruckcentre.app.ui.components.IconBadge
import au.com.foodtruckcentre.app.ui.components.LoadingBlocks
import au.com.foodtruckcentre.app.ui.components.MessageState
import au.com.foodtruckcentre.app.ui.components.PrimaryButton
import au.com.foodtruckcentre.app.ui.components.ScreenHeader
import au.com.foodtruckcentre.app.ui.components.SecondaryButton
import au.com.foodtruckcentre.app.ui.theme.Ftc
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------------- Login / Create account ---------------- */

@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onLoggedIn: () -> Unit,
    onForgot: () -> Unit,
    onLegal: (SiteContent.LegalPage) -> Unit,
) {
    var createMode by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.ime),
    ) {
        // Brand header
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Brush.verticalGradient(listOf(Ftc.NavyDeep, Ftc.Navy)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 28.dp, bottom = 34.dp, start = 24.dp, end = 24.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.logo_badge), "Food Truck Centre logo",
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(Color.White).padding(4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (createMode) "Create your account" else "Welcome back",
                    style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Log in to request quotes from Australia’s best food trucks and follow your enquiries.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center,
                )
            }
        }

        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModeSwitch(createMode) { createMode = it; vm.clearMessages() }

            AnimatedContent(createMode, label = "auth") { create ->
                FtcCard(Modifier.fillMaxWidth()) {
                    if (create) {
                        FtcTextField(vm.regName, { vm.regName = it; vm.errors.remove("name") }, "Full name", required = true,
                            error = vm.errors["name"], leadingIcon = Icons.Rounded.Person, capitalization = KeyboardCapitalization.Words, maxLength = 100)
                        Spacer(Modifier.height(12.dp))
                        FtcTextField(vm.regEmail, { vm.regEmail = it.trim(); vm.errors.remove("email") }, "Email", required = true,
                            error = vm.errors["email"], leadingIcon = Icons.Rounded.Email, keyboardType = KeyboardType.Email, maxLength = 254)
                        Spacer(Modifier.height(12.dp))
                        FtcTextField(vm.regPhone, { vm.regPhone = it; vm.errors.remove("phone") }, "Phone number", required = true,
                            error = vm.errors["phone"], leadingIcon = Icons.Rounded.Phone, keyboardType = KeyboardType.Phone, maxLength = 20)
                        Spacer(Modifier.height(12.dp))
                        FtcTextField(vm.regPassword, { vm.regPassword = it; vm.errors.remove("password") }, "Password", required = true,
                            error = vm.errors["password"], leadingIcon = Icons.Rounded.Lock, isPassword = true, imeAction = ImeAction.Done, maxLength = 200,
                            placeholder = "8+ characters, a letter and a number")
                        Spacer(Modifier.height(14.dp))
                        AcceptRow(vm.regAccepted, { vm.regAccepted = it; vm.errors.remove("accept") }, onLegal)
                        FieldError(vm.errors["accept"])
                        Spacer(Modifier.height(16.dp))
                        ErrorBanner(vm.banner)
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton("Create my account", { vm.register(onLoggedIn) }, loading = vm.busy, modifier = Modifier.fillMaxWidth())
                    } else {
                        FtcTextField(vm.loginEmail, { vm.loginEmail = it.trim(); vm.errors.remove("email") }, "Email", required = true,
                            error = vm.errors["email"], leadingIcon = Icons.Rounded.Email, keyboardType = KeyboardType.Email, maxLength = 254)
                        Spacer(Modifier.height(12.dp))
                        FtcTextField(vm.loginPassword, { vm.loginPassword = it; vm.errors.remove("password") }, "Password", required = true,
                            error = vm.errors["password"], leadingIcon = Icons.Rounded.Lock, isPassword = true, imeAction = ImeAction.Done, maxLength = 200)
                        Text(
                            "Forgot password?", style = MaterialTheme.typography.labelLarge, color = Ftc.Blue,
                            modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(50)).clickable(onClick = onForgot).padding(10.dp),
                        )
                        ErrorBanner(vm.banner)
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton("Log in", { vm.login(onLoggedIn) }, loading = vm.busy, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Text(
                if (createMode) "Already have an account? Log in" else "New here? Create an account",
                style = MaterialTheme.typography.labelLarge, color = Ftc.Red, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { createMode = !createMode; vm.clearMessages() }.padding(12.dp),
            )
            Text(
                "No payment is taken in this app.", style = MaterialTheme.typography.bodySmall, color = Ftc.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp))
        }
    }
}

@Composable
private fun ModeSwitch(create: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ftc.CreamDeep).padding(5.dp)) {
        listOf(false to "Log in", true to "Create account").forEach { (value, label) ->
            val selected = value == create
            val bg by animateColorAsState(if (selected) Color.White else Color.Transparent, label = "tab")
            Box(
                Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                    .clickable(role = Role.Tab) { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Ftc.Navy else Ftc.Muted)
            }
        }
    }
}

@Composable
private fun AcceptRow(checked: Boolean, onChange: (Boolean) -> Unit, onLegal: (SiteContent.LegalPage) -> Unit) {
    val linkStyle = TextLinkStyles(SpanStyle(color = Ftc.Blue, fontWeight = FontWeight.SemiBold))
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                .background(if (checked) Ftc.Navy else Color.White)
                .border(2.dp, if (checked) Ftc.Navy else Ftc.Line, RoundedCornerShape(8.dp))
                .clickable(role = Role.Checkbox, onClickLabel = "Accept the Terms and Privacy Policy") { onChange(!checked) }
                .semantics { contentDescription = if (checked) "Terms accepted" else "Accept terms" },
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                append("I agree to the ")
                withLink(LinkAnnotation.Clickable("terms", linkStyle) { onLegal(SiteContent.LegalPage.TERMS) }) { append("Terms and Conditions") }
                append(" and ")
                withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onLegal(SiteContent.LegalPage.PRIVACY) }) { append("Privacy Policy") }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall, color = Ftc.Ink, modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/* ---------------- Forgot password ---------------- */

@Composable
fun ForgotPasswordScreen(vm: AuthViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) {
        vm.clearMessages(); vm.forgotSent = null
        if (vm.forgotEmail.isBlank()) vm.forgotEmail = vm.loginEmail
    }
    Column(Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.ime)) {
        ScreenHeader("Reset password", eyebrow = "Your account", subtitle = "We'll email you a secure link to choose a new password.", onBack = onBack)
        Column(Modifier.padding(18.dp)) {
            val sent = vm.forgotSent
            FtcCard(Modifier.fillMaxWidth()) {
                if (sent != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        IconBadge(Icons.Rounded.MarkEmailRead, tint = Ftc.Success, background = Ftc.Success.copy(alpha = 0.12f), size = 72.dp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(sent, style = MaterialTheme.typography.bodyLarge, color = Ftc.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Open the link on this phone, choose a new password, then come back and log in.",
                        style = MaterialTheme.typography.bodySmall, color = Ftc.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton("Back to log in", onBack, modifier = Modifier.fillMaxWidth())
                } else {
                    FtcTextField(vm.forgotEmail, { vm.forgotEmail = it.trim(); vm.errors.remove("email") }, "Email", required = true,
                        error = vm.errors["email"], leadingIcon = Icons.Rounded.Email, keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, maxLength = 254)
                    Spacer(Modifier.height(16.dp))
                    ErrorBanner(vm.banner)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Send reset link", vm::forgot, loading = vm.busy, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/* ---------------- Account: profile + My Enquiries ---------------- */

@Composable
fun AccountScreen(
    vm: AuthViewModel,
    onBack: () -> Unit,
    onNewQuote: () -> Unit,
    onLegal: (SiteContent.LegalPage) -> Unit,
    onLoggedOut: () -> Unit,
) {
    val session by vm.sessions.session.collectAsState()
    val profile = session?.profile
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadEnquiries(); vm.refreshProfile() }

    Column(Modifier.fillMaxSize().background(Ftc.Cream).verticalScroll(rememberScrollState())) {
        ScreenHeader("My account", eyebrow = "Food Truck Centre", subtitle = profile?.email, onBack = onBack)
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            FtcCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Ftc.Red), contentAlignment = Alignment.Center) {
                        Text(
                            profile?.name?.trim()?.split(" ")?.filter { it.isNotEmpty() }?.take(2)?.joinToString("") { it.first().uppercase() } ?: "",
                            style = MaterialTheme.typography.titleLarge, color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, color = Ftc.Navy)
                        Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                        Text(profile?.phone.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Ftc.Muted)
                    }
                    IconButton(onClick = { vm.clearMessages(); editing = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit profile", tint = Ftc.Navy)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My enquiries", style = MaterialTheme.typography.headlineSmall, color = Ftc.Navy, modifier = Modifier.weight(1f))
                IconButton(onClick = vm::loadEnquiries) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Ftc.Navy) }
            }
            when (val st = vm.enquiries) {
                Loadable.Loading -> LoadingBlocks(lines = 3)
                is Loadable.Error -> MessageState("Couldn't load your enquiries", st.message, offline = st.offline, onAction = vm::loadEnquiries)
                is Loadable.Ready -> if (st.value.isEmpty()) {
                    MessageState(
                        "No enquiries yet", "When you request a quote or send us a message, it will appear here with its progress.",
                        icon = Icons.Rounded.Inbox, actionLabel = "Request a Quote", onAction = onNewQuote,
                    )
                } else {
                    st.value.forEach { EnquiryCard(it) }
                }
            }

            FtcCard(Modifier.fillMaxWidth()) {
                AccountRow(Icons.AutoMirrored.Rounded.Logout, "Log out", Ftc.Navy) { confirmLogout = true }
                AccountRow(Icons.Rounded.DeleteOutline, "Delete my account", Ftc.Error) { vm.clearMessages(); deleting = true }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Privacy", { onLegal(SiteContent.LegalPage.PRIVACY) }, modifier = Modifier.weight(1f))
                    SecondaryButton("Terms", { onLegal(SiteContent.LegalPage.TERMS) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp))
        }
    }

    if (editing && profile != null) {
        var name by rememberSaveable { mutableStateOf(profile.name) }
        var phone by rememberSaveable { mutableStateOf(profile.phone) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit profile", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    FtcTextField(name, { name = it; vm.errors.remove("name") }, "Full name", required = true, error = vm.errors["name"],
                        capitalization = KeyboardCapitalization.Words, maxLength = 100)
                    Spacer(Modifier.height(10.dp))
                    FtcTextField(phone, { phone = it; vm.errors.remove("phone") }, "Phone number", required = true, error = vm.errors["phone"],
                        keyboardType = KeyboardType.Phone, maxLength = 20)
                    Spacer(Modifier.height(10.dp))
                    ErrorBanner(vm.banner)
                }
            },
            confirmButton = { TextButton(enabled = !vm.busy, onClick = { vm.updateProfile(name, phone) { editing = false } }) { Text("Save", color = Ftc.Red) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel", color = Ftc.Navy) } },
            containerColor = Color.White,
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to log in again to request quotes.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; vm.logout(); onLoggedOut() }) { Text("Log out", color = Ftc.Red) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel", color = Ftc.Navy) } },
            containerColor = Color.White,
        )
    }

    if (deleting) {
        var password by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete your account?") },
            text = {
                Column {
                    Text(
                        "Your account and login will be permanently deleted. Enquiries you've already sent are kept by Food Truck Centre as business records but are no longer linked to you.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    FtcTextField(password, { password = it; vm.errors.remove("password") }, "Your password", required = true,
                        error = vm.errors["password"], isPassword = true, maxLength = 200)
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(vm.banner)
                }
            },
            confirmButton = {
                TextButton(enabled = !vm.busy, onClick = { vm.deleteAccount(password) { deleting = false; onLoggedOut() } }) {
                    Text("Delete permanently", color = Ftc.Error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel", color = Ftc.Navy) } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun AccountRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

private val submittedFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale("en", "AU"))

@Composable
private fun EnquiryCard(e: MyEnquiry) {
    val (fg, bg) = when (e.stage) {
        "confirmed" -> Ftc.Success to Ftc.Success.copy(alpha = 0.12f)
        "quoted" -> Color(0xFF8A5A00) to Ftc.MustardSoft
        "contacted" -> Ftc.Blue to Ftc.Blue.copy(alpha = 0.10f)
        "closed" -> Ftc.Muted to Ftc.CreamDeep
        else -> Ftc.Red to Ftc.RedSoft
    }
    val submitted = runCatching {
        OffsetDateTime.parse(e.submittedAt).atZoneSameInstant(ZoneId.systemDefault()).format(submittedFormat)
    }.getOrDefault("")
    FtcCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleMedium, color = Ftc.Navy)
                Text(e.enquiryId, style = MaterialTheme.typography.labelMedium, color = Ftc.Muted)
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(e.stageLabel, style = MaterialTheme.typography.labelMedium, color = fg)
            }
        }
        if (e.summary.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(e.summary, style = MaterialTheme.typography.bodyMedium, color = Ftc.Ink)
        }
        Spacer(Modifier.height(8.dp))
        val eventDate = e.eventDate?.let { d -> runCatching { java.time.LocalDate.parse(d).format(QuoteViewModel.dateFormat) }.getOrNull() }
        Text(
            listOfNotNull(eventDate?.let { "Event: $it" }, submitted.takeIf { it.isNotEmpty() }?.let { "Sent $it" }).joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall, color = Ftc.Muted,
        )
    }
}
