package com.pigeonpost.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PigeonPostShapes = Shapes(
    // Small elements like chips and small buttons - slightly cut corners for parchment feel
    small = CutCornerShape(4.dp),
    // Medium elements like cards and dialogs
    medium = RoundedCornerShape(12.dp),
    // Large elements like bottom sheets
    large = RoundedCornerShape(16.dp),
    // Extra large containers
    extraLarge = RoundedCornerShape(24.dp)
)
