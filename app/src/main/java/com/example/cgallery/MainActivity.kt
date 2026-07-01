package com.example.cgallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
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
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val isPermissionGranted = ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED

                var backstack by remember {
                    mutableStateOf(
                        if (isPermissionGranted) listOf(GalleryKey.Gallery)
                        else listOf(GalleryKey.Permission)
                    )
                }
                val scope = rememberCoroutineScope()

                Box(modifier = Modifier.fillMaxSize()) {
                    GalleryNavDisplay(
                        backstack = backstack,
                        onBack = {
                            if (backstack.size > 1) {
                                backstack = backstack.dropLast(1)
                            } else {
                                finish()
                            }
                        },
                        onNavigate = { newKey ->
                            backstack = if (newKey == GalleryKey.Gallery) {
                                listOf(GalleryKey.Gallery)
                            } else if (newKey is GalleryKey.Viewer) {
                                // For List-Detail, we often want to replace the current detail
                                // instead of pushing indefinitely if it's already a Viewer.
                                if (backstack.lastOrNull() is GalleryKey.Viewer) {
                                    backstack.dropLast(1) + newKey
                                } else {
                                    backstack + newKey
                                }
                            } else {
                                backstack + newKey
                            }
                        },
                        navigator = navigator
                    )
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
