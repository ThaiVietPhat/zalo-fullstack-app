package com.example.zalo.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zalo.util.CallManager

@Composable
fun IncomingCallDialog(callManager: CallManager) {
    val incomingCall by callManager.incomingCall.collectAsState()

    incomingCall?.let { call ->
        AlertDialog(
            onDismissRequest = { callManager.endCall() },
            title = { Text("Cuộc gọi đến từ ${call.fromUserName}") },
            text = { Text("Loại: ${call.callType}") },
            confirmButton = {
                Button(onClick = { /* Accept Logic */ }) {
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
