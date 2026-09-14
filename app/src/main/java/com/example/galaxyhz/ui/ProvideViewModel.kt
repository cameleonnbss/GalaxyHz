package com.example.galaxyhz.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Entry-point seam: provides the shared [GalaxyHzViewModel] so MainActivity
 * and previews resolve the same instance (and the accented-path friendly
 * default factory stays in one place).
 */
@Composable
fun ProvideViewModel(content: @Composable (GalaxyHzViewModel) -> Unit) {
    val vm: GalaxyHzViewModel = viewModel()
    content(vm)
}
