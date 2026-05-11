package com.example.zalo.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import com.example.zalo.util.CallManager

@Composable
fun IncomingCallDialog(callManager: CallManager) {
    val incomingCall by callManager.incomingCall.collectAsState()
    var isCallActive by remember { mutableStateOf(false) }

    // If call is ended remotely, reset state
    LaunchedEffect(incomingCall) {
        if (incomingCall == null) {
            isCallActive = false
        }
    }

    if (isCallActive && incomingCall != null) {
        // Full screen active call overlay
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            title = null,
            text = null,
            confirmButton = {},
            dismissButton = {}
        )
        // Draw the call screen on top
        ActiveCallScreen(
            callManager = callManager,
            targetName = incomingCall?.fromUserName ?: "Unknown",
            isVideo = incomingCall?.callType == "video"
        )
    } else if (incomingCall != null && !isCallActive) {
        // Incoming call ringing dialog
        AlertDialog(
            onDismissRequest = { callManager.endCall() },
            title = { Text("Cuộc gọi đến từ ${incomingCall?.fromUserName ?: "Unknown"}") },
            text = { Text("Loại: ${if (incomingCall?.callType == "video") "Video" else "Âm thanh"}") },
            confirmButton = {
                Button(onClick = { isCallActive = true }) {
                    Text("Chấp nhận")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { callManager.endCall() }) {
                    Text("Từ chối")
                }
            }
        )
    }
}
