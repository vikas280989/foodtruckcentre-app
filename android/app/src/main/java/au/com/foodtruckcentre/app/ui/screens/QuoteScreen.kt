package au.com.foodtruckcentre.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.data.SiteContent.Cuisine
import au.com.foodtruckcentre.app.ui.components.ChoicePills
import au.com.foodtruckcentre.app.ui.components.FieldError
import au.com.foodtruckcentre.app.ui.components.FtcCard
import au.com.foodtruckcentre.app.ui.components.FtcTextField
import au.com.foodtruckcentre.app.ui.components.PrimaryButton
import au.com.foodtruckcentre.app.ui.components.ScreenHeader
import au.com.foodtruckcentre.app.ui.theme.Ftc
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@Composable
fun QuoteScreen(
    vm: QuoteViewModel,
    onSuccess: (QuoteViewModel.Success) -> Unit,
    onOpenLegal: (SiteContent.LegalPage) -> Unit,
    onBack: (() -> Unit)?,
) {
    val scroll = rememberScrollState()
    val offsets = remember { mutableStateMapOf<String, Int>() }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.touch() }
    LaunchedEffect(vm.success) { vm.success?.let { onSuccess(it); vm.success = null } }
    LaunchedEffect(vm.scrollTarget) {
        val key = vm.scrollTarget ?: return@LaunchedEffect
        offsets[key]?.let { scroll.animateScrollTo((it - 260).coerceAtLeast(0)) }
        vm.scrollTarget = null
    }

    // Content offset of each field = position on screen + current scroll.
    fun Modifier.anchor(key: String) = this.onGloballyPositioned { offsets[key] = it.positionInRoot().y.toInt() + scroll.value }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ftc.Cream)
            .verticalScroll(scroll)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        ScreenHeader(
            title = "Request a Quote",
            eyebrow = "Event catering enquiry",
            subtitle = SiteContent.FORM_INTRO,
            onBack = onBack,
        )

        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(2.dp))
            NoticeStrip()

            // 1. Cuisines
            FtcCard(Modifier.fillMaxWidth().anchor(QuoteField.CUISINES)) {
                StepTitle(1, "Select Food Truck Cuisines", trailing = { CuisineCounter(vm.selectedCuisines.size) })
                Text("tick 3 max.", style = MaterialTheme.typography.bodyMedium, color = Ftc.Muted, modifier = Modifier.padding(start = 44.dp))
                Spacer(Modifier.height(14.dp))
                CuisineLimitMessage(vm.cuisineLimitHit)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Cuisine.entries.forEach { c ->
                        CuisineRow(c, selected = c in vm.selectedCuisines, disabled = c !in vm.selectedCuisines && vm.selectedCuisines.size >= SiteContent.MAX_CUISINES) {
                            vm.toggleCuisine(c)
                        }
                    }
                }
                FieldError(vm.errors[QuoteField.CUISINES])
            }

            // 2. Customer details
            FtcCard(Modifier.fillMaxWidth()) {
                StepTitle(2, "Your details")
                Spacer(Modifier.height(16.dp))
                FtcTextField(
                    vm.name, { vm.name = it; vm.clearError(QuoteField.NAME) }, "Name", required = true,
                    placeholder = "Your Full Name", error = vm.errors[QuoteField.NAME], leadingIcon = Icons.Rounded.Person,
                    capitalization = KeyboardCapitalization.Words, maxLength = 100, modifier = Modifier.anchor(QuoteField.NAME),
                )
                Spacer(Modifier.height(12.dp))
                FtcTextField(
                    vm.email, { vm.email = it.trim(); vm.clearError(QuoteField.EMAIL) }, "Email", required = true,
                    placeholder = "Your Email Address", error = vm.errors[QuoteField.EMAIL], leadingIcon = Icons.Rounded.Email,
                    keyboardType = KeyboardType.Email, maxLength = 254, modifier = Modifier.anchor(QuoteField.EMAIL),
                )
                Spacer(Modifier.height(12.dp))
                FtcTextField(
                    vm.phone, { vm.phone = it; vm.clearError(QuoteField.PHONE) }, "Phone Number", required = true,
                    placeholder = "Your Phone Number", error = vm.errors[QuoteField.PHONE], leadingIcon = Icons.Rounded.Phone,
                    keyboardType = KeyboardType.Phone, maxLength = 20, modifier = Modifier.anchor(QuoteField.PHONE),
                )
            }

            // 3. Event details
            FtcCard(Modifier.fillMaxWidth()) {
                StepTitle(3, "Event details")
                Spacer(Modifier.height(16.dp))
                PickerField(
                    label = "When is the event *",
                    value = vm.eventDate?.format(QuoteViewModel.dateFormat),
                    placeholder = "Choose a date",
                    icon = Icons.Rounded.CalendarMonth,
                    error = vm.errors[QuoteField.EVENT_DATE],
                    modifier = Modifier.anchor(QuoteField.EVENT_DATE),
                ) { showDate = true }
                Spacer(Modifier.height(12.dp))
                PickerField(
                    label = "What time is food to be served",
                    value = vm.foodTime.ifBlank { null },
                    placeholder = "eg. 12:00pm",
                    icon = Icons.Rounded.Schedule,
                    error = null,
                    onClear = if (vm.foodTime.isNotBlank()) ({ vm.foodTime = "" }) else null,
                    modifier = Modifier.anchor(QuoteField.FOOD_TIME),
                ) { showTime = true }
                Spacer(Modifier.height(12.dp))
                FtcTextField(
                    vm.address, { vm.address = it }, "Address of the event", placeholder = "Full address of the event",
                    leadingIcon = Icons.Rounded.LocationOn, capitalization = KeyboardCapitalization.Words, maxLength = 250,
                    modifier = Modifier.anchor(QuoteField.ADDRESS),
                )
                Spacer(Modifier.height(12.dp))
                FtcTextField(
                    vm.postcode, { v -> vm.postcode = v.filter { it.isDigit() }.take(4); vm.clearError(QuoteField.POSTCODE) },
                    "Postcode", required = true, placeholder = "Postcode", error = vm.errors[QuoteField.POSTCODE],
                    leadingIcon = Icons.Rounded.PinDrop, keyboardType = KeyboardType.Number, maxLength = 4,
                    modifier = Modifier.anchor(QuoteField.POSTCODE),
                )
                Spacer(Modifier.height(18.dp))
                Question("What is the event *", Modifier.anchor(QuoteField.EVENT_TYPE))
                ChoicePills(SiteContent.eventTypes, vm.eventType, { vm.eventType = it; vm.clearError(QuoteField.EVENT_TYPE) })
                FieldError(vm.errors[QuoteField.EVENT_TYPE])
                Spacer(Modifier.height(18.dp))
                FtcTextField(
                    vm.meals, { v -> vm.meals = v.filter { it.isDigit() }.take(6); vm.clearError(QuoteField.MEALS) },
                    "How many meals to be served (minimum 50)", required = true, placeholder = "Minimum 50",
                    error = vm.errors[QuoteField.MEALS], leadingIcon = Icons.Rounded.Restaurant,
                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Done, maxLength = 6,
                    modifier = Modifier.anchor(QuoteField.MEALS),
                )
            }

            // 4. Event requirements
            FtcCard(Modifier.fillMaxWidth()) {
                StepTitle(4, "Event requirements")
                Spacer(Modifier.height(16.dp))
                Question("Who will be paying for food *", Modifier.anchor(QuoteField.PAYING))
                ChoicePills(SiteContent.payers, vm.paying, { vm.paying = it; vm.clearError(QuoteField.PAYING) })
                FieldError(vm.errors[QuoteField.PAYING])
                Spacer(Modifier.height(18.dp))
                Question("Is there power available at the address *", Modifier.anchor(QuoteField.POWER))
                ChoicePills(SiteContent.yesNo, vm.power, { vm.power = it; vm.clearError(QuoteField.POWER) })
                FieldError(vm.errors[QuoteField.POWER])
                Spacer(Modifier.height(18.dp))
                Question("Will the truck be parked on private land *", Modifier.anchor(QuoteField.PRIVATE_LAND))
                ChoicePills(SiteContent.yesNo, vm.privateLand, { vm.privateLand = it; vm.clearError(QuoteField.PRIVATE_LAND) })
                FieldError(vm.errors[QuoteField.PRIVATE_LAND])
                Spacer(Modifier.height(18.dp))
                Question("Is the entrance wider than 3m and at least 5m in length without overhanging obstacles *", Modifier.anchor(QuoteField.ENTRANCE))
                ChoicePills(SiteContent.yesNo, vm.entrance, { vm.entrance = it; vm.clearError(QuoteField.ENTRANCE) })
                FieldError(vm.errors[QuoteField.ENTRANCE])
            }

            // 5. Additional info
            FtcCard(Modifier.fillMaxWidth()) {
                StepTitle(5, "Additional information")
                Spacer(Modifier.height(16.dp))
                FtcTextField(
                    vm.info, { vm.info = it; vm.clearError(QuoteField.INFO) }, "Additional information",
                    placeholder = "Any other details you would like to share", singleLine = false, minLines = 4,
                    capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, maxLength = 3000,
                    error = vm.errors[QuoteField.INFO], modifier = Modifier.anchor(QuoteField.INFO),
                )
            }

            ErrorBanner(vm.banner)

            PrimaryButton(
                text = "Send Quote Request",
                onClick = vm::submit,
                loading = vm.submitting,
                icon = Icons.AutoMirrored.Rounded.Send,
                modifier = Modifier.fillMaxWidth(),
            )
            ConsentText(onOpenLegal)
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showDate) EventDateDialog(vm.eventDate, onDismiss = { showDate = false }) {
        vm.eventDate = it; vm.clearError(QuoteField.EVENT_DATE); showDate = false
    }
    if (showTime) FoodTimeDialog(onDismiss = { showTime = false }) {
        vm.foodTime = it; showTime = false
    }
}

@Composable
private fun NoticeStrip() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Ftc.MustardSoft)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.Info, null, tint = Ftc.Navy, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Share your event details to receive quotes and menus from food trucks. Sending an enquiry doesn't confirm a booking, and no payment is taken in this app.",
            style = MaterialTheme.typography.bodySmall,
            color = Ftc.Ink,
        )
    }
}

@Composable
private fun StepTitle(step: Int, title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(Ftc.Navy), contentAlignment = Alignment.Center) {
            Text(step.toString(), style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ftc.Navy, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun CuisineCounter(count: Int) {
    val full = count >= SiteContent.MAX_CUISINES
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (full) Ftc.Red else Ftc.NavySoft).padding(horizontal = 12.dp, vertical = 5.dp)
            .semantics { contentDescription = "$count of ${SiteContent.MAX_CUISINES} cuisines selected" },
    ) {
        Text("$count/${SiteContent.MAX_CUISINES}", style = MaterialTheme.typography.labelMedium, color = if (full) Color.White else Ftc.Navy)
    }
}

@Composable
private fun CuisineLimitMessage(trigger: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) { visible = true; delay(3500); visible = false }
    }
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(14.dp)).background(Ftc.RedSoft).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.WarningAmber, null, tint = Ftc.Red, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "You can select a maximum of three cuisines. Untick one to choose another.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Ftc.RedDeep,
            )
        }
    }
}

@Composable
private fun CuisineRow(c: Cuisine, selected: Boolean, disabled: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) Ftc.Red else Ftc.Line, label = "border")
    val bg by animateColorAsState(if (selected) Ftc.RedSoft.copy(alpha = 0.55f) else Color.White, label = "bg")
    val alpha by animateFloatAsState(if (disabled) 0.55f else 1f, label = "alpha")
    val checkScale by animateFloatAsState(if (selected) 1f else 0.6f, label = "check")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(if (selected) Ftc.Red else Ftc.CreamDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(cuisineIcon(c), null, tint = if (selected) Color.White else Ftc.Navy.copy(alpha = alpha), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.label, style = MaterialTheme.typography.titleSmall, color = Ftc.Navy.copy(alpha = alpha))
            Text(c.description, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted.copy(alpha = alpha))
        }
        Box(
            Modifier.size(26.dp).scale(checkScale).clip(CircleShape)
                .background(if (selected) Ftc.Red else Color.Transparent)
                .border(2.dp, if (selected) Ftc.Red else Ftc.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Question(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Ftc.Ink, modifier = modifier.padding(bottom = 10.dp))
}

@Composable
private fun PickerField(
    label: String,
    value: String?,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (error != null) Color(0xFFFFF7F7) else Color(0xFFFFFCF8))
                .border(if (error != null) 2.dp else 1.dp, if (error != null) Ftc.Error else Ftc.Line, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (error != null) Ftc.Error else Ftc.Navy.copy(alpha = 0.7f))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Ftc.Muted)
                Text(
                    value ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value != null) Ftc.Ink else Ftc.Muted.copy(alpha = 0.7f),
                )
            }
            if (onClear != null) {
                IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Ftc.Muted) }
            }
        }
        FieldError(error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDateDialog(current: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val todayUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val maxUtc = LocalDate.now().plusYears(3).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
        yearRange = LocalDate.now().year..LocalDate.now().plusYears(3).year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in todayUtc..maxUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
            ) { Text("Confirm", color = Ftc.Red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ftc.Navy) } },
    ) {
        DatePicker(
            state = state,
            title = { Text("When is the event?", modifier = Modifier.padding(start = 24.dp, top = 20.dp), style = MaterialTheme.typography.titleMedium) },
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Ftc.Red,
                todayDateBorderColor = Ftc.Red,
                todayContentColor = Ftc.Red,
                selectedYearContainerColor = Ftc.Red,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodTimeDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val state = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What time is food to be served?", style = MaterialTheme.typography.titleMedium) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    selectorColor = Ftc.Red,
                    timeSelectorSelectedContainerColor = Ftc.RedSoft,
                    timeSelectorSelectedContentColor = Ftc.RedDeep,
                    periodSelectorSelectedContainerColor = Ftc.NavySoft,
                    periodSelectorSelectedContentColor = Ftc.Navy,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val h12 = if (state.hour % 12 == 0) 12 else state.hour % 12
                val ampm = if (state.hour < 12) "am" else "pm"
                onPick(String.format(Locale.ROOT, "%d:%02d%s", h12, state.minute, ampm))
            }) { Text("Confirm", color = Ftc.Red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ftc.Navy) } },
        containerColor = Color.White,
    )
}

@Composable
fun ErrorBanner(banner: QuoteViewModel.Banner?) {
    AnimatedVisibility(banner != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val b = banner ?: return@AnimatedVisibility
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ftc.RedSoft)
                .border(1.dp, Ftc.Red.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(if (b.offline) Icons.Rounded.WarningAmber else Icons.Rounded.ErrorOutline, null, tint = Ftc.Red, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(b.message, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Ftc.RedDeep)
        }
    }
}

@Composable
fun ConsentText(onOpenLegal: (SiteContent.LegalPage) -> Unit) {
    val linkStyle = TextLinkStyles(SpanStyle(color = Ftc.Blue, fontWeight = FontWeight.SemiBold))
    val text = buildAnnotatedString {
        append("By sending, you agree to the ")
        withLink(LinkAnnotation.Clickable("terms", linkStyle) { onOpenLegal(SiteContent.LegalPage.TERMS) }) { append("Terms and Conditions") }
        append(" and ")
        withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onOpenLegal(SiteContent.LegalPage.PRIVACY) }) { append("Privacy Policy") }
        append(".")
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = Ftc.Muted, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp))
}
