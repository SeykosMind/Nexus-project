package com.nexus.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.data.model.DocumentResult
import com.nexus.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: DocumentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<DocumentResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) { _query.value = q }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _aiResponse.value = ""
            val docs = repo.search(q)
            _results.value = docs
            if (docs.isNotEmpty()) {
                val aiAnswer = repo.queryWithAi(q, docs)
                _aiResponse.value = aiAnswer
            }
            _isSearching.value = false
        }
    }

    fun openDocument(doc: DocumentResult) {
        repo.openDocument(context, doc.path)
    }
}
