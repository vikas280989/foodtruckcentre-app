package au.com.foodtruckcentre.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.ui.screens.AboutScreen
import au.com.foodtruckcentre.app.ui.screens.ContactScreen
import au.com.foodtruckcentre.app.ui.screens.ContactViewModel
import au.com.foodtruckcentre.app.ui.screens.ContentViewModel
import au.com.foodtruckcentre.app.ui.screens.FeaturingScreen
import au.com.foodtruckcentre.app.ui.screens.HomeScreen
import au.com.foodtruckcentre.app.ui.screens.IntroScreen
import au.com.foodtruckcentre.app.ui.screens.LegalScreen
import au.com.foodtruckcentre.app.ui.screens.QuoteScreen
import au.com.foodtruckcentre.app.ui.screens.QuoteViewModel
import au.com.foodtruckcentre.app.ui.screens.ServicesScreen
import au.com.foodtruckcentre.app.ui.screens.SuccessScreen
import au.com.foodtruckcentre.app.ui.theme.Ftc

private object Routes {
    const val INTRO = "intro"
    const val HOME = "home"
    const val SERVICES = "services"
    const val QUOTE = "quote"
    const val FEATURING = "featuring"
    const val CONTACT = "contact"
    const val ABOUT = "about"
    const val LEGAL = "legal/{slug}"
    const val SUCCESS = "success/{id}?name={name}"
    fun legal(page: SiteContent.LegalPage) = "legal/${page.slug}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.SERVICES, "Services", Icons.Rounded.RoomService),
    Tab(Routes.QUOTE, "Quote", Icons.Rounded.RequestQuote),
    Tab(Routes.FEATURING, "Featuring", Icons.Rounded.LocalShipping),
    Tab(Routes.CONTACT, "Contact", Icons.Rounded.MailOutline),
)

@Composable
fun FtcApp() {
    val nav = rememberNavController()
    // Activity-scoped view models: forms keep their contents while moving between tabs.
    val owner = checkNotNull(LocalActivity.current as? ComponentActivity) { "FtcApp must be hosted in a ComponentActivity" }
    val quoteVm: QuoteViewModel = viewModel(owner)
    val contactVm: ContactViewModel = viewModel(owner)
    val contentVm: ContentViewModel = viewModel(owner)

    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current in tabs.map { it.route }

    fun openTab(route: String) {
        nav.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openQuote(cuisine: SiteContent.Cuisine? = null, eventType: String? = null) {
        quoteVm.prefill(cuisine, eventType)
        openTab(Routes.QUOTE)
    }

    fun openLegal(page: SiteContent.LegalPage) = nav.navigate(Routes.legal(page))
    fun back(): Unit { if (!nav.popBackStack()) openTab(Routes.HOME) }

    Box(Modifier.fillMaxSize().background(Ftc.Cream)) {
        NavHost(
            navController = nav,
            startDestination = Routes.INTRO,
            enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
        ) {
            composable(Routes.INTRO, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                IntroScreen(onDone = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.INTRO) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    content = contentVm,
                    onQuote = { c, t -> openQuote(c, t) },
                    onServices = { openTab(Routes.SERVICES) },
                    onService = { s -> openQuote(eventType = s.presetEventType) },
                    onFeaturing = { openTab(Routes.FEATURING) },
                    onContact = { openTab(Routes.CONTACT) },
                    onAbout = { nav.navigate(Routes.ABOUT) },
                    onLegal = ::openLegal,
                )
            }
            composable(Routes.SERVICES) {
                ServicesScreen(onQuote = { s -> openQuote(eventType = s.presetEventType) }, onBack = null)
            }
            composable(Routes.QUOTE) {
                QuoteScreen(
                    vm = quoteVm,
                    onSuccess = { s ->
                        nav.navigate("success/${s.enquiryId}?name=${android.net.Uri.encode(s.firstName)}") {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onOpenLegal = ::openLegal,
                    onBack = null,
                )
            }
            composable(Routes.FEATURING) {
                FeaturingScreen(contentVm, onQuote = { c -> openQuote(cuisine = c) }, onBack = null)
            }
            composable(Routes.CONTACT) {
                ContactScreen(contactVm, contentVm, onLegal = ::openLegal, onBack = null)
            }
            composable(Routes.ABOUT) {
                AboutScreen(onQuote = { openQuote() }, onLegal = ::openLegal, onBack = ::back)
            }
            composable(Routes.LEGAL, arguments = listOf(navArgument("slug") { type = NavType.StringType })) { entry ->
                val page = SiteContent.LegalPage.entries.firstOrNull { it.slug == entry.arguments?.getString("slug") }
                    ?: SiteContent.LegalPage.PRIVACY
                LegalScreen(page, contentVm, onQuote = { openQuote() }, onBack = ::back)
            }
            composable(
                Routes.SUCCESS,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
                enterTransition = { fadeIn() + slideInVertically { it / 8 } },
            ) { entry ->
                SuccessScreen(
                    enquiryId = entry.arguments?.getString("id").orEmpty(),
                    firstName = entry.arguments?.getString("name").orEmpty(),
                    onHome = { openTab(Routes.HOME) },
                    onAbout = { nav.navigate(Routes.ABOUT) },
                )
            }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            BottomBar(current, onSelect = { route -> if (route != current) openTab(route) })
        }
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(20.dp, RoundedCornerShape(26.dp), ambientColor = Ftc.Navy, spotColor = Ftc.Navy.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == current
                if (tab.route == Routes.QUOTE) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Spacer(Modifier.size(56.dp)) }
                } else {
                    val color by animateColorAsState(if (selected) Ftc.Red else Ftc.Muted, label = "tab")
                    val pill by animateDpAsState(if (selected) 22.dp else 0.dp, label = "pill")
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(role = Role.Tab, onClickLabel = tab.label) { onSelect(tab.route) }
                            .semantics { this.selected = selected }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.height(3.dp).size(width = pill, height = 3.dp).clip(CircleShape).background(Ftc.Red))
                    }
                }
            }
        }
        // Prominent centre "Request Quote" action.
        val quoteSelected = current == Routes.QUOTE
        val quoteBg by animateColorAsState(if (quoteSelected) Ftc.Navy else Ftc.Red, label = "quote")
        Column(
            Modifier.align(Alignment.TopCenter).offset(y = (-18).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .shadow(14.dp, CircleShape, ambientColor = quoteBg, spotColor = quoteBg)
                    .clip(CircleShape)
                    .background(quoteBg)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(color = Color.White),
                        role = Role.Tab,
                        onClickLabel = "Request a Quote",
                    ) { onSelect(Routes.QUOTE) }
                    .semantics { selected = quoteSelected },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.RequestQuote, contentDescription = "Request a Quote", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(
                "Get Quote", style = MaterialTheme.typography.labelSmall,
                color = if (quoteSelected) Ftc.Navy else Ftc.Red, modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
