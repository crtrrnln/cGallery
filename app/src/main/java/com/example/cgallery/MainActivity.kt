package com.example.cgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cgallery.ui.theme.CGalleryTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CGalleryTheme {
                val navigator = rememberListDetailPaneScaffoldNavigator<Any>()
                var backstack by remember { mutableStateOf(listOf<GalleryKey>(GalleryKey.Home)) }
                val scope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        GalleryNavDisplay(
                            backstack = backstack,
                            onBack = {
                                if (backstack.size > 1) {
                                    backstack = backstack.dropLast(1)
                                } else {
                                    finish()
                                }
                            },
                            navigator = navigator
                        )
                    }
                }

                // Handle back button for the adaptive scaffold
                BackHandler(enabled = navigator.canNavigateBack()) {
                    scope.launch {
                        navigator.navigateBack()
                    }
                }
            }
        }
    }
}
