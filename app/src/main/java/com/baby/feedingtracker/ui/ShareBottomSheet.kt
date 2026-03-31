package com.baby.feedingtracker.ui

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.SharingState
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    sharingState: SharingState,
    isGoogleLoggedIn: Boolean,
    inviteCode: String?,
    sharingError: String?,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onGenerateCode: () -> Unit,
    onRedeemCode: (String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            when (sharingState) {
                is SharingState.Connected -> {
                    ConnectedContent(partnerEmail = sharingState.partnerEmail)
                }
                is SharingState.NotConnected -> {
                    if (!isGoogleLoggedIn) {
                        NotLoggedInContent(
                            googleAuthHelper = googleAuthHelper,
                            googleSignInLauncher = googleSignInLauncher
                        )
                    } else if (inviteCode != null) {
                        CodeGeneratedContent(code = inviteCode)
                    } else {
                        LoggedInNotConnectedContent(
                            sharingError = sharingError,
                            onGenerateCode = onGenerateCode,
                            onRedeemCode = onRedeemCode,
                            onClearError = onClearError
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// State A: Not logged in (anonymous)
// ──────────────────────────────────────────────

@Composable
private fun NotLoggedInContent(
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    Text(
        text = "공유하기",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "배우자와 수유 기록을 함께\n보려면 로그인이 필요합니다",
        style = MaterialTheme.typography.bodyLarge,
        color = LocalExtendedColors.current.subtleText,
        lineHeight = 24.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            val intent = googleAuthHelper.getSignInIntent()
            googleSignInLauncher.launch(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = "G  Google로 로그인",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ──────────────────────────────────────────────
// State B: Logged in, not connected
// ──────────────────────────────────────────────

@Composable
private fun LoggedInNotConnectedContent(
    sharingError: String?,
    onGenerateCode: () -> Unit,
    onRedeemCode: (String) -> Unit,
    onClearError: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    Text(
        text = "공유하기",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Generate invite code button
    Button(
        onClick = onGenerateCode,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = "초대 코드 만들기",
            style = MaterialTheme.typography.labelLarge
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Divider with "또는"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LocalExtendedColors.current.divider)
        )
        Text(
            text = "  또는  ",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.subtleText
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LocalExtendedColors.current.divider)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Code input section
    Text(
        text = "초대 코드 입력",
        style = MaterialTheme.typography.labelMedium,
        color = LocalExtendedColors.current.subtleText
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 6-character code input field
    BasicTextField(
        value = codeInput,
        onValueChange = { newValue ->
            if (newValue.length <= 6) {
                codeInput = newValue.uppercase()
                onClearError()
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters
        ),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (sharingError != null) {
                            LocalExtendedColors.current.deleteColor
                        } else {
                            LocalExtendedColors.current.divider
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Show individual character boxes
                repeat(6) { index ->
                    val char = codeInput.getOrNull(index)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.dp,
                                color = LocalExtendedColors.current.divider,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (index < 5) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            // Hidden actual text field for keyboard input
            Box(modifier = Modifier.size(0.dp)) {
                innerTextField()
            }
        }
    )

    // Error message
    if (sharingError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sharingError,
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.deleteColor
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Connect button
    Button(
        onClick = { onRedeemCode(codeInput) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = codeInput.length == 6,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = "연결하기",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ──────────────────────────────────────────────
// State C: Code generated
// ──────────────────────────────────────────────

@Composable
private fun CodeGeneratedContent(code: String) {
    val clipboardManager = LocalClipboardManager.current

    Text(
        text = "공유하기",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "초대 코드",
        style = MaterialTheme.typography.labelMedium,
        color = LocalExtendedColors.current.subtleText
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Code display
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        code.forEach { char ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "10분 후 만료",
        style = MaterialTheme.typography.bodySmall,
        color = LocalExtendedColors.current.subtleText,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Copy button
    OutlinedButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(code))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "코드 복사하기",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ──────────────────────────────────────────────
// State D: Connected
// ──────────────────────────────────────────────

@Composable
private fun ConnectedContent(partnerEmail: String?) {
    Text(
        text = "공유 상태",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "연결됨",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    if (partnerEmail != null) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$partnerEmail 과 공유 중",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.subtleText
        )
    }
}
