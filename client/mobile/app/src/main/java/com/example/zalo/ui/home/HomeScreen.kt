@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.zalo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.zalo.data.remote.dto.ChatDto
import com.example.zalo.data.remote.dto.GroupDto
import com.example.zalo.data.remote.dto.UserDto
import com.example.zalo.ui.ai.AiChatScreen
import com.example.zalo.ui.user.UserViewModel

@Composable
fun HomeScreen(
    onChatClick: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tin nhắn", "Danh bạ", "Nhóm", "AI", "Cá nhân")
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Zalo", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    }
                    if (selectedTab == 4) {
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(onClick = onCreateGroupClick, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Tạo nhóm", tint = Color.White)
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title, fontSize = 10.sp) },
                        icon = {
                            Icon(
                                imageVector = when(index) {
                                    0 -> Icons.Default.Email
                                    1 -> Icons.Default.Person
                                    2 -> Icons.Default.AccountBox
                                    3 -> Icons.Default.Face
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (uiState.isLoading && selectedTab != 3 && selectedTab != 4) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> ChatList(uiState.chats, onChatClick)
                    1 -> ContactsList()
                    2 -> GroupList(uiState.groups, onGroupClick)
                    3 -> AiChatScreen()
                    4 -> MeScreen(onProfileClick)
                }
            }
        }
    }
}

@Composable
fun ChatList(chats: List<ChatDto>, onChatClick: (String) -> Unit) {
    if (chats.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có cuộc trò chuyện nào", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chats) { chat ->
                ChatItem(
                    title = chat.chatName ?: "User",
                    lastMessage = chat.lastMessage ?: "Bắt đầu cuộc trò chuyện",
                    time = chat.lastMessageTime?.take(10) ?: "",
                    unreadCount = chat.unreadCount,
                    avatarUrl = chat.avatarUrl,
                    onClick = { onChatClick(chat.id) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun GroupList(groups: List<GroupDto>, onGroupClick: (String) -> Unit) {
    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa tham gia nhóm nào", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(groups) { group ->
                ChatItem(
                    title = group.name ?: "Nhóm",
                    lastMessage = group.lastMessage ?: "Nhóm mới tạo",
                    time = group.lastMessageTime?.take(10) ?: "",
                    unreadCount = 0,
                    avatarUrl = group.avatarUrl,
                    onClick = { onGroupClick(group.id) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun ContactsList(viewModel: ContactsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.pendingRequests.isNotEmpty()) {
            Text(
                "Lời mời kết bạn (${uiState.pendingRequests.size})",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                items(uiState.pendingRequests) { request ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = request.senderAvatarUrl ?: "https://ui-avatars.com/api/?name=${request.senderName}",
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(request.senderName, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.acceptRequest(request.id) }) {
                            Text("Chấp nhận")
                        }
                    }
                }
            }
            HorizontalDivider()
        }

        Text(
            "Danh bạ (${uiState.contacts.size})",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )
        
        if (uiState.contacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có bạn bè nào", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.contacts) { contact ->
                    ContactItem(contact)
                }
            }
        }
    }
}

@Composable
fun ContactItem(user: UserDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl ?: "https://ui-avatars.com/api/?name=${user.firstName}+${user.lastName}",
            contentDescription = null,
            modifier = Modifier.size(45.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.SemiBold)
            Text(if (user.online) "Đang hoạt động" else user.lastSeenText ?: "Ngoại tuyến", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MeScreen(onProfileClick: () -> Unit, viewModel: UserViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = uiState.user?.avatarUrl ?: "https://ui-avatars.com/api/?name=${uiState.user?.firstName}+${uiState.user?.lastName}",
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${uiState.user?.firstName ?: ""} ${uiState.user?.lastName ?: ""}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Xem trang cá nhân", fontSize = 14.sp, color = Color.Gray)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
        
        HorizontalDivider(thickness = 8.dp, color = Color(0xFFF0F0F0))
        
        ListItem(
            headlineContent = { Text("Ví QR") },
            leadingContent = { Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Blue) }
        )
        ListItem(
            headlineContent = { Text("Cloud của tôi") },
            leadingContent = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Blue) }
        )
        ListItem(
            headlineContent = { Text("Dữ liệu trên máy") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.Blue) }
        )
        
        HorizontalDivider(thickness = 8.dp, color = Color(0xFFF0F0F0))
        
        ListItem(
            headlineContent = { Text("Tài khoản và bảo mật") },
            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) }
        )
        ListItem(
            headlineContent = { Text("Quyền riêng tư") },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray) }
        )
    }
}

@Composable
fun ChatItem(
    title: String,
    lastMessage: String,
    time: String,
    unreadCount: Long,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl ?: "https://ui-avatars.com/api/?name=$title",
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unreadCount > 0) Color.Black else Color.Gray,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unreadCount > 0) {
                    Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
