package com.example.cgallery

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectionViewModel : ViewModel() {
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths = _selectedPaths.asStateFlow()

    fun togglePath(path: String) {
        if (_selectedPaths.value.contains(path)) {
            _selectedPaths.value -= path
        } else {
            _selectedPaths.value += path
        }
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }
}
