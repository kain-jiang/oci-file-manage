package com.tiramission.ocisync.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tiramission.ocisync.core.model.ArtifactInfo
import com.tiramission.ocisync.core.model.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 仓库浏览状态:查询 + 结果 + 行操作,见 docs/06-ui-design.md §3.4。 */
class ListViewModel(private val syncService: SyncService) : ViewModel() {

    data class UiState(
        val ref: String = "",
        val artifacts: List<ArtifactInfo> = emptyList(),
        val activeFilter: String? = null,          // 当前选中的筛选 chip("k=v")
        val filterChips: List<String> = emptyList(), // 筛选 chips 数据源(来自结果 labels,设计稿 仓库.html)
        val loading: Boolean = false,
        val error: String? = null,
        val deletingRef: String? = null,            // 等待删除确认的 artifact
        val labelingArtifact: ArtifactInfo? = null, // 标签弹窗目标
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onRefChange(v: String) = _uiState.update { it.copy(ref = v) }

    /** 选择筛选 chip(null=全部)。 */
    fun setFilter(filter: String?) {
        _uiState.update { it.copy(activeFilter = filter) }
        search()
    }

    /** 查询(按当前筛选 chip)。 */
    fun search() {
        val state = _uiState.value
        if (state.ref.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val filters = state.activeFilter?.let { listOf(it) } ?: emptyList()
            val result = syncService.list(state.ref.trim(), filters)
            _uiState.update {
                val artifacts = result.getOrDefault(emptyList())
                it.copy(
                    loading = false,
                    artifacts = artifacts,
                    // chips = 结果中出现的标签集合(全部 + k=v)
                    filterChips = artifacts.flatMap { a -> a.labels.entries.map { e -> "${e.key}=${e.value}" } }.distinct(),
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun requestDelete(artifact: ArtifactInfo) = _uiState.update { it.copy(deletingRef = artifact.fullName) }

    fun confirmDelete() {
        val ref = _uiState.value.deletingRef ?: return
        _uiState.update { it.copy(deletingRef = null) }
        viewModelScope.launch {
            syncService.delete(ref)
            search()
        }
    }

    fun dismissDelete() = _uiState.update { it.copy(deletingRef = null) }

    fun openLabelDialog(artifact: ArtifactInfo) = _uiState.update { it.copy(labelingArtifact = artifact) }

    fun dismissLabelDialog() = _uiState.update { it.copy(labelingArtifact = null) }

    /** label set/unset 后刷新。 */
    fun applyLabels(artifact: ArtifactInfo, updates: Map<String, String>, removeKeys: List<String>) {
        viewModelScope.launch {
            syncService.setLabels(artifact.fullName, updates)
            syncService.unsetLabels(artifact.fullName, removeKeys)
            _uiState.update { it.copy(labelingArtifact = null) }
            search()
        }
    }

    class Factory(private val syncService: SyncService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ListViewModel(syncService) as T
    }
}
