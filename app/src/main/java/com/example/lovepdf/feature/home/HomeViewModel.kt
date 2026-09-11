package com.example.lovepdf.feature.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val store = RecentDocumentsStore(app)

    private val _recents = MutableStateFlow(store.all())
    val recents: StateFlow<List<RecentDocument>> = _recents.asStateFlow()

    fun record(uri: Uri, name: String) {
        store.record(uri, name)
        _recents.value = store.all()
    }

    fun forget(uri: Uri) {
        store.remove(uri)
        _recents.value = store.all()
    }

    fun clearAll() {
        store.clear()
        _recents.value = emptyList()
    }
}
