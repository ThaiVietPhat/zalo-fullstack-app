package com.example.zalo.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.zalo.data.remote.dto.MessageDto
import com.example.zalo.util.FileUtil

@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    tokenManager: TokenManager,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val myUserId = tokenManager.getUserId()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            FileUtil.fromUri(context, it)?.let { file ->
                viewModel.uploadMedia(file)
            }
        }
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    if (uiState.summary != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSummary() },
            title = { Text("Tóm tắt AI") },
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
                        Text("Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (uiState.isTyping) {
                            Text("Đang nhập...", color = Color.White, fontSize = 12.sp)
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
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Thông tin hội thoại") },
                            onClick = { showMenu = false }
                        )
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
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                reverseLayout = false
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message = message, isMine = message.senderId == myUserId)
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBDEFB))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            color = Color(0xFF1976D2)
        )
    }
}

@Composable
fun MessageBubble(message: MessageDto, isMine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (message.type == "IMAGE") {
             // In a real app, use Coil AsyncImage here
             Box(Modifier.size(200.dp).background(Color.LightGray).clip(RoundedCornerShape(8.dp))) {
                 Text("Hình ảnh", Modifier.align(Alignment.Center))
             }
        } else {
            Surface(
                color = if (isMine) Color(0xFFD1E4FF) else Color.White,
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isMine) 12.dp else 0.dp,
                    bottomEnd = if (isMine) 0.dp else 12.dp
                ),
                tonalElevation = 1.dp,
                shadowElevation = 0.5.dp
            ) {
                Text(
                    text = message.content ?: "",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ChatInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickFile: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickFile) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
            }
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tin nhắn") },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onSend) {
                Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
