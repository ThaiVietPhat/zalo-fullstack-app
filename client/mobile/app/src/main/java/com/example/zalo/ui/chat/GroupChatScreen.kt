package com.example.zalo.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zalo.data.local.TokenManager
import com.example.zalo.data.remote.dto.GroupMessageDto
import com.example.zalo.util.FileUtil

@Composable
fun GroupChatScreen(
    groupId: String,
    onBack: () -> Unit,
    tokenManager: TokenManager,
    viewModel: GroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val myUserId = tokenManager.getUserId()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            FileUtil.fromUri(context, it)?.let { file ->
                viewModel.uploadMedia(file)
            }
        }
    }

    LaunchedEffect(groupId) {
        viewModel.loadMessages(groupId)
    }

    if (uiState.summary != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSummary() },
            title = { Text("Tóm tắt nhóm AI") },
            text = { Text(uiState.summary!!.summary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSummary() }) { Text("Đóng") }
            }
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("Nhóm Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (uiState.isTyping) {
                            Text("Có người đang nhập...", color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.summarize() }) {
                        Icon(Icons.Default.Info, contentDescription = "Tóm tắt", tint = Color.White)
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        bottomBar = {
            Column {
                if (uiState.suggestions.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.suggestions) { suggestion ->
                            SuggestionChip(suggestion) {
                                viewModel.sendMessage(suggestion)
                            }
                        }
                    }
                }
                ChatInput(
                    messageText = messageText,
                    onMessageChange = { 
                        messageText = it
                        viewModel.sendTyping(it.isNotEmpty())
                    },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                            viewModel.sendTyping(false)
                        }
                    },
                    onPickFile = { filePicker.launch("*/*") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            ) {
                items(uiState.messages) { message ->
                    GroupMessageBubble(message = message, isMine = message.senderId == myUserId)
                }
            }
        }
    }
}

@Composable
fun GroupMessageBubble(message: GroupMessageDto, isMine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text = message.senderName ?: "User",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            color = if (isMine) Color(0xFFD1E4FF) else Color.White,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Text(
                text = message.content ?: "",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
