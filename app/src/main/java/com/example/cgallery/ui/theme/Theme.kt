package com.example.cgallery.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.cgallery.data.*

private val InnocentSinRed = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
)

private val EternalPunishmentBlue = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF001233),
    primaryContainer = Color(0xFF0047AB),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF70B2FF),
    onSecondary = Color(0xFF001A3F),
    secondaryContainer = Color(0xFF003066),
    onSecondaryContainer = Color(0xFFD6E3FF)
)

@Composable
fun CGalleryTheme(
    settingsRepository: AppSettingsRepository? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val actualRepo = remember(settingsRepository) { settingsRepository ?: AppSettingsRepository(context) }
    val settings by actualRepo.settingsFlow.collectAsState(initial = AppSettings())
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        settings.themeAccent == ThemeAccent.ETERNAL_PUNISHMENT_BLUE -> EternalPunishmentBlue
        else -> InnocentSinRed
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
