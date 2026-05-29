package com.nowaste.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowaste.app.domain.AppTheme

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0C7),
    onPrimaryContainer = Color(0xFF3D1900),
    secondary = Color(0xFF5F6F1F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F0B8),
    onSecondaryContainer = Color(0xFF1C2200),
    tertiary = Color(0xFF29A65A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFF4D8),
    onTertiaryContainer = Color(0xFF003914),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF241A12),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF241A12),
    surfaceVariant = Color(0xFFF3E8DA),
    onSurfaceVariant = Color(0xFF55443A),
    outline = Color(0xFF8A7669),
    outlineVariant = Color(0xFFE0D0C2),
    inverseSurface = Color(0xFF3A2F28),
    inverseOnSurface = Color(0xFFFFEDE0),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB36C),
    onPrimary = Color(0xFF4E2600),
    primaryContainer = Color(0xFF713900),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFC5D98F),
    onSecondary = Color(0xFF2D3400),
    secondaryContainer = Color(0xFF434D10),
    onSecondaryContainer = Color(0xFFE1F6A9),
    tertiary = Color(0xFF7BDDA2),
    onTertiary = Color(0xFF00391D),
    tertiaryContainer = Color(0xFF00522D),
    onTertiaryContainer = Color(0xFF98FABB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111417),
    onBackground = Color(0xFFE4E2DD),
    surface = Color(0xFF181C1B),
    onSurface = Color(0xFFE4E2DD),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC3C9BE),
    outline = Color(0xFF8D9489),
    outlineVariant = Color(0xFF424940),
    inverseSurface = Color(0xFFE4E2DD),
    inverseOnSurface = Color(0xFF2D3130),
)

private val NoWasteShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val NoWasteTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun NoWasteTheme(
    theme: AppTheme = AppTheme.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (theme) {
        AppTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NoWasteTypography,
        shapes = NoWasteShapes,
        content = content,
    )
}
