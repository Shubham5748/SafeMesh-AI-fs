package com.safemesh.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.safemesh.ai.ui.navigation.MainAppNavigation
import com.safemesh.ai.ui.theme.SafeMeshAITheme
import com.safemesh.ai.ui.viewmodel.EmergencyViewModel
import com.safemesh.ai.ui.viewmodel.EmergencyViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: EmergencyViewModel by viewModels {
        val app = application as SafeMeshApplication
        EmergencyViewModelFactory(app, app.emergencyRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeMeshAITheme {
                MainAppNavigation(viewModel = viewModel)
            }
        }
    }
}
