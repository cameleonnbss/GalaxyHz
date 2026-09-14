package com.example.galaxyhz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.example.galaxyhz.theme.GalaxyHzTheme
import com.example.galaxyhz.ui.AppRoot
import com.example.galaxyhz.ui.GalaxyHzViewModel
import com.example.galaxyhz.ui.ProvideViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Slow the root status-polling when the app is not in the foreground
        // (querying root every 2.5 s forever starves Magisk's su daemon).
        val vm = ViewModelProvider(this)[GalaxyHzViewModel::class.java]
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { vm.resumePolling() }
            override fun onPause(owner: LifecycleOwner) { vm.pausePolling() }
        })

        setContent {
            GalaxyHzTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProvideViewModel { viewModel ->
                        AppRoot(viewModel)
                    }
                }
            }
        }
    }
}
