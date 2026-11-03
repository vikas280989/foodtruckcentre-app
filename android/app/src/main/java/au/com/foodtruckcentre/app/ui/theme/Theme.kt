package au.com.foodtruckcentre.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.foodtruckcentre.app.R

/** Brand palette – sampled from the Food Truck Centre logo (truck red, navy lettering, yellow awning). */
object Ftc {
    val Red = Color(0xFFD62828)
    val RedDeep = Color(0xFFA61B1B)
    val RedSoft = Color(0xFFFDE8E6)
    val Navy = Color(0xFF0A1F5C)
    val NavyDeep = Color(0xFF061340)
    val NavySoft = Color(0xFFE7EBF6)
    val Blue = Color(0xFF046BD2) // website link/accent blue
    val Mustard = Color(0xFFF4B400)
    val MustardSoft = Color(0xFFFFF3CF)
    val Cream = Color(0xFFFFF8F1)
    val CreamDeep = Color(0xFFF7EBDD)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF152238)
    val Muted = Color(0xFF5D6676)
    val Line = Color(0xFFEADFD1)
    val Success = Color(0xFF1E8E5A)
    val Error = Color(0xFFC62828)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Poppins, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
)

private val FtcTypography = Typography(
    displayLarge = style(40, FontWeight.ExtraBold, 46, -0.5),
    displayMedium = style(34, FontWeight.ExtraBold, 40, -0.4),
    displaySmall = style(28, FontWeight.ExtraBold, 34, -0.3),
    headlineLarge = style(26, FontWeight.Bold, 32, -0.2),
    headlineMedium = style(22, FontWeight.Bold, 28, -0.1),
    headlineSmall = style(19, FontWeight.Bold, 26),
    titleLarge = style(18, FontWeight.SemiBold, 24),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 25),
    bodyMedium = style(14, FontWeight.Normal, 22),
    bodySmall = style(12, FontWeight.Normal, 18),
    labelLarge = style(15, FontWeight.SemiBold, 20, 0.1),
    labelMedium = style(12, FontWeight.SemiBold, 16, 0.4),
    labelSmall = style(11, FontWeight.Medium, 14, 0.4),
)

private val FtcColors = lightColorScheme(
    primary = Ftc.Red,
    onPrimary = Ftc.White,
    primaryContainer = Ftc.RedSoft,
    onPrimaryContainer = Ftc.RedDeep,
    secondary = Ftc.Navy,
    onSecondary = Ftc.White,
    secondaryContainer = Ftc.NavySoft,
    onSecondaryContainer = Ftc.Navy,
    tertiary = Ftc.Mustard,
    onTertiary = Ftc.Ink,
    background = Ftc.Cream,
    onBackground = Ftc.Ink,
    surface = Ftc.White,
    onSurface = Ftc.Ink,
    surfaceVariant = Ftc.CreamDeep,
    onSurfaceVariant = Ftc.Muted,
    surfaceContainerHighest = Ftc.CreamDeep,
    surfaceContainerHigh = Ftc.White,
    surfaceContainer = Ftc.White,
    surfaceContainerLow = Ftc.White,
    outline = Ftc.Line,
    outlineVariant = Ftc.Line,
    error = Ftc.Error,
    onError = Ftc.White,
)

private val FtcShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FtcTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FtcColors, typography = FtcTypography, shapes = FtcShapes, content = content)
}
