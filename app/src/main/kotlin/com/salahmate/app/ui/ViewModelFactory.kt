package com.salahmate.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salahmate.app.AppContainer

/**
 * Builds a ViewModel from the app container without a DI framework. Kept tiny on purpose: each
 * ViewModel takes its dependencies via its constructor, so unit tests instantiate them directly.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    container: AppContainer,
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM = viewModel(
    key = key,
    factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
    },
)
