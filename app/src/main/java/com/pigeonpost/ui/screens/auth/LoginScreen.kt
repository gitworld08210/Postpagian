package com.pigeonpost.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pigeonpost.ui.components.ParchmentBackground
import com.pigeonpost.ui.components.WaxSeal
import com.pigeonpost.ui.components.parchmentTextFieldColors
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown900
import com.pigeonpost.ui.theme.WaxSealRed500

/**
 * Medieval-themed login screen with parchment card, wax seal button,
 * gold-accented text fields, sign-in and sign-up tabs.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    // Forgot Password Dialog
    if (uiState.showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = uiState.forgotPasswordEmail,
            onEmailChange = viewModel::updateForgotPasswordEmail,
            isLoading = uiState.forgotPasswordLoading,
            message = uiState.forgotPasswordMessage,
            onSend = viewModel::sendPasswordReset,
            onDismiss = viewModel::dismissForgotPasswordDialog
        )
    }

    ParchmentBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header with wax seal
            WaxSeal(size = 64.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // Title in dark ink so it reads clearly against the parchment
            Text(
                text = "Pigeon Post",
                style = MaterialTheme.typography.displayMedium,
                color = DeepBrown900,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (uiState.isSignUp) "Join the Messenger Guild" else "Enter the Aviary",
                style = MaterialTheme.typography.titleSmall,
                color = DeepBrown700,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Thy Electronic Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = DeepBrown900),
                colors = parchmentTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Secret Passphrase") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = DeepBrown900),
                colors = parchmentTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Error message
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = WaxSealRed500,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot password button - only show in sign-in mode
            if (!uiState.isSignUp) {
                TextButton(onClick = viewModel::showForgotPasswordDialog) {
                    Text(
                        text = "Forgot thy passphrase?",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBrown700,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button styled as wax seal
            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.isSignUp) "Seal & Register" else "Break Seal & Enter",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        WaxSeal(size = 24.dp, showRibbon = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle sign-in / sign-up
            TextButton(onClick = viewModel::toggleMode) {
                Text(
                    text = if (uiState.isSignUp) {
                        "Already a member? Enter the Aviary"
                    } else {
                        "New messenger? Join the Guild"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBrown900
                )
            }
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    message: String?,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Recover Thy Passphrase",
                style = MaterialTheme.typography.titleMedium,
                color = DeepBrown900
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter thy electronic address and a recovery scroll shall be dispatched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBrown700
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Thy Electronic Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = DeepBrown900),
                    colors = parchmentTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.contains("dispatched")) DeepBrown700 else WaxSealRed500
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSend,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Recovery Scroll")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = DeepBrown700
                )
            }
        }
    )
}
