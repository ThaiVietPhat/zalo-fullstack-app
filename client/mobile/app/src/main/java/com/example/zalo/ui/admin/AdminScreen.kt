@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.zalo.ui.admin

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.zalo.data.remote.dto.AdminStatsDto
import com.example.zalo.data.remote.dto.UserDto

@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tổng quan", "Người dùng", "Nhóm", "Nhật ký")

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.loadStats()
            1 -> viewModel.loadUsers()
            2 -> viewModel.loadGroups()
            3 -> viewModel.loadAuditLogs()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Trang quản trị", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> StatsTab(uiState.stats)
                    1 -> UsersTab(uiState.users, viewModel)
                    2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Quản lý nhóm") }
                    3 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nhật ký hệ thống") }
                }
            }
        }
    }
}

@Composable
fun StatsTab(stats: AdminStatsDto?) {
    if (stats == null) return
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Thống kê hệ thống", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Tổng user", stats.totalUsers.toString(), Modifier.weight(1f))
            StatCard("Tổng nhóm", stats.totalGroups.toString(), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Tin nhắn", stats.totalMessages.toString(), Modifier.weight(1f))
            StatCard("Đang online", stats.onlineUsers.toString(), Modifier.weight(1f), Color(0xFF4CAF50))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.primary) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, fontSize = 14.sp)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun UsersTab(users: List<UserDto>, viewModel: AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users) { user ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = user.avatarUrl ?: "https://ui-avatars.com/api/?name=${user.firstName}+${user.lastName}",
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.Bold)
                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Khoá tài khoản", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                viewModel.banUser(user.id)
                            }
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
