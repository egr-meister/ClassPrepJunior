package com.classprep.junior.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Paper {
    val Background = Color(0xFFFBF6EA)
    val Sheet = Color(0xFFFFFCF4)
    val Deep = Color(0xFFF3EBD7)
    val Navy = Color(0xFF1F2D4D)
    val NavyMuted = Color(0xFF4A5878)
    val Rule = Color(0xFFC9D3E6)
    val Margin = Color(0xFFE8B4B0)
    val Tab = Color(0xFFDCE7F7)
    val TabSelected = Color(0xFF2F4E86)
    val Highlight = Color(0xFFFBF1C7)
    val HighlightStrong = Color(0xFFF2D675)
    val Green = Color(0xFF2E7D4F)
    val GreenLight = Color(0xFFDCEFE2)
    val Warning = Color(0xFF8A4B0F)
    val WarningLight = Color(0xFFFBE7CF)
    val Error = Color(0xFFA3262A)
}

private val colors = lightColorScheme(
    primary = Paper.TabSelected,
    onPrimary = Color.White,
    primaryContainer = Paper.Tab,
    onPrimaryContainer = Paper.Navy,
    secondary = Paper.Green,
    onSecondary = Color.White,
    secondaryContainer = Paper.GreenLight,
    onSecondaryContainer = Paper.Navy,
    tertiary = Paper.Warning,
    tertiaryContainer = Paper.Highlight,
    onTertiaryContainer = Paper.Navy,
    background = Paper.Background,
    onBackground = Paper.Navy,
    surface = Paper.Sheet,
    onSurface = Paper.Navy,
    surfaceVariant = Paper.Deep,
    onSurfaceVariant = Paper.NavyMuted,
    surfaceContainer = Paper.Sheet,
    surfaceContainerHigh = Paper.Sheet,
    surfaceContainerHighest = Paper.Deep,
    surfaceContainerLow = Paper.Background,
    outline = Paper.NavyMuted,
    outlineVariant = Paper.Rule,
    error = Paper.Error,
)

private val base = Typography()
private val typography = Typography(
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 25.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)

/** Handwritten-planner feel for small labels such as lesson numbers. */
val LessonNumberStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)

@Composable
fun ClassPrepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
        ),
        content = content,
    )
}
