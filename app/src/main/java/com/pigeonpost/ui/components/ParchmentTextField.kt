package com.pigeonpost.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown900
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.Parchment200
import com.pigeonpost.ui.theme.WaxSealRed500

/**
 * Explicit text-field colors for the parchment aesthetic.
 *
 * Every colour is stated outright rather than inherited from the platform theme.
 * The parchment sheet is always light cream, so typed text, placeholders, labels
 * and the cursor are all dark ink and stay legible even when the device is in
 * dark mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun parchmentTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    // Typed text - dark ink, always readable on cream
    focusedTextColor = DeepBrown900,
    unfocusedTextColor = DeepBrown900,
    disabledTextColor = DeepBrown700,
    errorTextColor = DeepBrown900,

    // Cursor - dark ink so it is visible against parchment
    cursorColor = DeepBrown900,
    errorCursorColor = WaxSealRed500,

    // Container - a slightly brighter inset sheet of parchment
    focusedContainerColor = Parchment200.copy(alpha = 0.75f),
    unfocusedContainerColor = Parchment200.copy(alpha = 0.55f),
    disabledContainerColor = Parchment200.copy(alpha = 0.4f),
    errorContainerColor = Parchment200.copy(alpha = 0.75f),

    // Borders - gold when focused, faded brown ink at rest
    focusedBorderColor = GoldAccent400,
    unfocusedBorderColor = DeepBrown700.copy(alpha = 0.7f),
    disabledBorderColor = DeepBrown700.copy(alpha = 0.35f),
    errorBorderColor = WaxSealRed500,

    // Labels - dark ink, not low-alpha grey
    focusedLabelColor = DeepBrown900,
    unfocusedLabelColor = DeepBrown700,
    disabledLabelColor = DeepBrown700.copy(alpha = 0.5f),
    errorLabelColor = WaxSealRed500,

    // Placeholders - faded ink, but far more contrast than the default
    focusedPlaceholderColor = DeepBrown700.copy(alpha = 0.8f),
    unfocusedPlaceholderColor = DeepBrown700.copy(alpha = 0.8f),
    disabledPlaceholderColor = DeepBrown700.copy(alpha = 0.4f),

    // Leading/trailing icons
    focusedLeadingIconColor = DeepBrown900,
    unfocusedLeadingIconColor = DeepBrown700,
    focusedTrailingIconColor = DeepBrown900,
    unfocusedTrailingIconColor = DeepBrown700
)
