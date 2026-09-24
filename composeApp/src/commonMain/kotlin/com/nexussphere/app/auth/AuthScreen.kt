package com.nexussphere.app.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexussphere.app.NexusTheme

/**
 * SnapTrade credential entry screen.
 *
 * Credentials are kept in memory only during this session.
 * Phase 3 will move them into platform-specific secure storage (Keystore / Keychain).
 * They are NEVER stored in SharedPreferences, UserDefaults, or any unencrypted store.
 */
@Composable
fun AuthScreen(
    onConnected: (userId: String, userSecret: String) -> Unit,
    onDemoMode: () -> Unit,
) {
    var userId     by remember { mutableStateOf("") }
    var userSecret by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text     = "Connect Broker",
            color    = NexusTheme.text,
            fontSize = 22.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text     = "Enter your SnapTrade credentials to load live portfolio data.",
            color    = NexusTheme.textMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))

        NexusTextField(
            value       = userId,
            onValueChange = { userId = it },
            label       = "User ID",
            placeholder = "Your SnapTrade user ID",
        )
        Spacer(modifier = Modifier.height(16.dp))
        NexusTextField(
            value       = userSecret,
            onValueChange = { userSecret = it },
            label       = "User Secret",
            placeholder = "Your SnapTrade user secret",
            isPassword  = !showSecret,
            trailingIcon = {
                TextButton(onClick = { showSecret = !showSecret }) {
                    Text(if (showSecret) "Hide" else "Show", color = NexusTheme.accent, fontSize = 12.sp)
                }
            },
        )
        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick  = { onConnected(userId.trim(), userSecret.trim()) },
            enabled  = userId.isNotBlank() && userSecret.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = NexusTheme.accent),
        ) {
            Text("Connect", color = NexusTheme.background)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onDemoMode) {
            Text("Continue in demo mode", color = NexusTheme.textMuted, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text     = "Credentials are sent only to your NexusSphere server (POST body, never URL params) " +
                       "and are never stored on device in this phase.",
            color    = NexusTheme.textMuted.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun NexusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        modifier       = Modifier.fillMaxWidth(),
        label          = { Text(label, color = NexusTheme.textMuted) },
        placeholder    = { Text(placeholder, color = NexusTheme.textMuted.copy(alpha = 0.4f)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
        trailingIcon   = trailingIcon,
        singleLine     = true,
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = NexusTheme.accent,
            unfocusedBorderColor  = NexusTheme.textMuted.copy(alpha = 0.3f),
            focusedTextColor      = NexusTheme.text,
            unfocusedTextColor    = NexusTheme.text,
            cursorColor           = NexusTheme.accent,
        ),
    )
}
