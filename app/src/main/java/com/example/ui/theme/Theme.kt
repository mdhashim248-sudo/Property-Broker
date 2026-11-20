package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = BrokerBlueLight,
    onPrimary = OnBrokerBlueContainer,
    primaryContainer = BrokerBlue,
    onPrimaryContainer = BrokerBlueContainer,
    secondary = BrokerPurpleLight,
    onSecondary = OnBrokerPurpleContainer,
    secondaryContainer = BrokerPurple,
    onSecondaryContainer = BrokerPurpleContainer,
    tertiary = BrokerGreenLight,
    onTertiary = OnBrokerGreenContainer,
    tertiaryContainer = Color(0xFF144217),
    onTertiaryContainer = BrokerGreenContainer,
    background = MidnightBackground,
    onBackground = LightInkText,
    surface = MidnightSurface,
    onSurface = LightInkText,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = LightMutedText,
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrokerBlue,
    onPrimary = Color.White,
    primaryContainer = BrokerBlueContainer,
    onPrimaryContainer = OnBrokerBlueContainer,
    secondary = BrokerDeepPurple,
    onSecondary = Color.White,
    secondaryContainer = BrokerPurpleContainer,
    onSecondaryContainer = OnBrokerPurpleContainer,
    tertiary = BrokerGreen,
    onTertiary = Color.White,
    tertiaryContainer = BrokerGreenContainer,
    onTertiaryContainer = OnBrokerGreenContainer,
    background = ScreenBgLight,
    onBackground = InkText,
    surface = PureSurface,
    onSurface = InkText,
    surfaceVariant = SoftSlateVariant,
    onSurfaceVariant = MutedSlateText,
    outline = SubtleOutline,
    error = BrokerRed,
    errorContainer = BrokerRedContainer,
    onErrorContainer = OnBrokerRedContainer,
  )

val PropertyBrokerShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
  )

@Composable
fun PropertyBrokerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = PropertyBrokerTypography,
    shapes = PropertyBrokerShapes,
    content = content,
  )
}
