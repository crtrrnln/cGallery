package com.example.cgallery

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectionViewModel : ViewModel() {
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths = _selectedPaths.asStateFlow()
    
    private val _selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaIds = _selectedMediaIds.asStateFlow()

    fun togglePath(path: String) {
        _selectedPaths.value = if (_selectedPaths.value.contains(path)) {
            _selectedPaths.value - path
        } else {
            _selectedPaths.value + path
        }
    }
    
    fun toggleMediaId(id: Long) {
        _selectedMediaIds.value = if (_selectedMediaIds.value.contains(id)) {
            _selectedMediaIds.value - id
        } else {
            _selectedMediaIds.value + id
        }
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
        _selectedMediaIds.value = emptySet()
    }
    
    fun setSelection(ids: Set<Long>) {
        _selectedMediaIds.value = ids
    }
}
