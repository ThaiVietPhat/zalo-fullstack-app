package com.example.zalo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.zalo.data.local.TokenManager
import com.example.zalo.ui.call.IncomingCallDialog
import com.example.zalo.ui.navigation.ZaloNavGraph
import com.example.zalo.ui.theme.ZaloCloneTheme
import com.example.zalo.util.CallManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var callManager: CallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZaloCloneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    ZaloNavGraph(navController = navController, tokenManager = tokenManager)
                    
                    // Always show incoming call dialog
                    IncomingCallDialog(callManager = callManager)
                }
            }
        }
    }
}
