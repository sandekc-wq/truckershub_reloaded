package com.truckershub.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Wir definieren hier, wie Überschriften und Texte aussehen
val ThubTypography = Typography(
    // Große Überschriften (z.B. "TRUCKERS HUB")
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = TextWhite,
        letterSpacing = 1.sp
    ),
    // Normale Titel (z.B. "Abfahrtskontrolle")
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = ThubNeonBlue
    ),
    // Normaler Text (z.B. Chatnachrichten)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = TextWhite
    ),
    // Kleine Labels (z.B. auf Buttons oder Status)
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = TextGray
    )
)