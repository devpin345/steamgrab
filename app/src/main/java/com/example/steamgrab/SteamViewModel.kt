package com.example.steamgrab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class GameInfo(val appId: Int, val name: String, val sizeLabel: String)

sealed class UiState {
    data class LoggedOut(val loading: Boolean = false, val error: String? = null, val needsGuardCode: Boolean = false) : UiState()
    data class GameList(val games: List<GameInfo>) : UiState()
    data class Downloading(val progress: Float, val gameName: String, val status: String) : UiState()
    data class Done(val savedPath: String) : UiState()
}

class SteamViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow<UiState>(UiState.LoggedOut())
    val uiState: StateFlow<UiState> = _uiState

    private val repo = SteamRepository()

    fun login(username: String, password: String, guardCode: String?) {
        _uiState.value = UiState.LoggedOut(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            repo.login(
                username, password, guardCode,
                onNeedGuardCode = {
                    _uiState.value = UiState.LoggedOut(needsGuardCode = true)
                },
                onError = { msg ->
                    _uiState.value = UiState.LoggedOut(error = msg)
                },
                onSuccess = { games ->
                    _uiState.value = UiState.GameList(games)
                }
            )
        }
    }

    fun startDownload(appId: Int) {
        val game = (uiState.value as? UiState.GameList)?.games?.find { it.appId == appId } ?: return
        _uiState.value = UiState.Downloading(0f, game.name, "Memulai...")

        val targetDir = File(getApplication<Application>().getExternalFilesDir(null), "SteamGames/${game.name}")

        viewModelScope.launch(Dispatchers.IO) {
            repo.downloadGame(
                appId = appId,
                savePath = targetDir.absolutePath,
                onProgress = { pct, status ->
                    _uiState.value = UiState.Downloading(pct, game.name, status)
                },
                onComplete = {
                    _uiState.value = UiState.Done(targetDir.absolutePath)
                },
                onError = { msg ->
                    _uiState.value = UiState.LoggedOut(error = msg)
                }
            )
        }
    }

    fun backToList() {
        viewModelScope.launch(Dispatchers.IO) {
            val games = repo.getCachedLicenseList()
            _uiState.value = UiState.GameList(games)
        }
    }
}
