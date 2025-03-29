package com.example.mycountdays.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mycountdays.EventDao
import kotlinx.coroutines.launch

class EventViewModel(private val eventDao: EventDao) : ViewModel() {
    fun insertItem(event: Event) {
        viewModelScope.launch {
            //eventDao.insert(event)
        }
    }
}
class InventoryViewModelFactory(private val eventDao: EventDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(eventDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}